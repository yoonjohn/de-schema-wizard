package com.deleidos.dmf.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.deleidos.dmf.exception.AnalyticsCancelledWorkflowException;
import com.deleidos.dmf.progressbar.SimpleProgressUpdater;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateHandler;
import com.google.common.io.Files;

public class CancellableFileUploader implements Callable<List<File>> {
	private static final Logger logger = Logger.getLogger(CancellableFileUploader.class);
	private final HttpServletRequest httpRequest;
	private final List<File> uploadedFileList;
	private final String destinationDirectory;
	private final DiskFileItemFactory diskFileItemFactory;
	private final ExecutorService watcherExecutor;
	private final FileUploadProgressWatcherRunnable progressWatcherRunnable;
	private long expectedFileSize;
	private volatile FileUploadException uploadException = null;
	public static final int MAX_UPLOAD_SECONDS = 600; // maximum of five minutes   
	public static final long MEGABYTES_TO_SHOW_BYTE_UPDATES = 5;

	public CancellableFileUploader(HttpServletRequest request, ExecutorService watcherExecutor, ProfilingProgressUpdateHandler progressUpdater, String destinationDirectory) throws IOException {
		this.uploadedFileList = new ArrayList<File>();
		this.httpRequest = request;
		this.destinationDirectory = destinationDirectory;
		this.diskFileItemFactory = new DiskFileItemFactory();
		diskFileItemFactory.setRepository(Files.createTempDir());
		if(progressUpdater instanceof SimpleProgressUpdater) {
			long totalKB = ((SimpleProgressUpdater)progressUpdater).getTotalRecords()/1024;
			// only set description callback for uploads greater than 50 MB
			if(totalKB/1024 > MEGABYTES_TO_SHOW_BYTE_UPDATES) {
				((SimpleProgressUpdater)progressUpdater).setDescriptionCallback(
						(updater, progress)->"Uploading files ("+String.valueOf(progress/1024)+"/"+totalKB+" KB).");
			}
		}
		progressWatcherRunnable = new FileUploadProgressWatcherRunnable(progressUpdater, 
				MAX_UPLOAD_SECONDS*1000, diskFileItemFactory.getRepository());
		this.watcherExecutor = watcherExecutor;
	}

	@Override
	public List<File> call() throws AnalyticsCancelledWorkflowException, FileUploadException {
		Future<?> watcherRunnableReturn = watcherExecutor.submit(progressWatcherRunnable);
		long startTime = System.currentTimeMillis();
		final FileUploadRunnable uploadRunnable = new FileUploadRunnable(progressWatcherRunnable, watcherRunnableReturn);
		Thread uploadThread = new Thread(uploadRunnable);	
		uploadThread.start();
		while(uploadThread.isAlive()) {
			if(System.currentTimeMillis() - startTime > MAX_UPLOAD_SECONDS*1000) {
				failureCallback(new FileUploadException("File upload timed out after " + MAX_UPLOAD_SECONDS + " seconds."));
			}
			if(uploadRunnable.isCancelled()) {
				failureCallback(new AnalyticsCancelledWorkflowException("Workflow cancelled during file upload."));
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				failureCallback(e);
				break;
			}
		}
		logger.debug("Removing temporary upload directory "+diskFileItemFactory.getRepository()+".");
		try {
			FileUtils.deleteDirectory(diskFileItemFactory.getRepository());
		} catch (IOException e) {
			logger.error("Could not delete repository.  Continuing with analysis.", e);
		}
		if(uploadException != null) {
			if(uploadException.getCause() instanceof FileUploadBase.FileUploadIOException) {
				logger.debug("Suspected early termination.", uploadException);
				throw new AnalyticsCancelledWorkflowException("Suspected early termination.  Cancelling.", uploadException);
			}
			throw uploadException;
		} else {
			logger.info("Uploaded " + uploadedFileList.size() + " files.");
		}
		return uploadedFileList;	
	}

	private void failureCallback(Exception e) {
		this.uploadException = new FileUploadException("File upload failed due to " + e +".", e);
	}

	private class FileUploadRunnable implements Runnable {
		private final FileUploadProgressWatcherRunnable watcherRunnable;
		private Future<?> watcherReturn;
		private volatile boolean isUploadCancelled;
		private boolean successfulShutdown;

		public FileUploadRunnable(FileUploadProgressWatcherRunnable progressWatcher, Future<?> watcherReturn) {
			this.watcherRunnable = progressWatcher;
			this.isUploadCancelled = false;
			this.watcherReturn = watcherReturn;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			logger.debug("Starting up file upload thread.");
			try {
				//this line handles the transfer from form data to a file on disk
				List<FileItem> multiparts = new ServletFileUpload(diskFileItemFactory).parseRequest(httpRequest);	
				for(FileItem fileItem : multiparts) {
					if (!fileItem.isFormField()) {
						// this block copies the temporary file to a different destination
						String name = new File(fileItem.getName()).getName();
						File uploadedFile = new File(destinationDirectory, name);
						fileItem.write(uploadedFile);
						uploadedFileList.add(uploadedFile);
						logger.debug("");
						logger.debug("File: " + uploadedFile.getAbsolutePath());
						logger.debug("");
					} else {
						throw new FileUploadException("File item was not form field.");
					}
				}
				watcherRunnable.setUploadComplete();
				successfulShutdown = endWatcherRunnable(watcherReturn, watcherRunnable.getDir().getAbsolutePath());
			} catch (Exception e) {
				watcherRunnable.setUploadComplete();
				successfulShutdown = endWatcherRunnable(watcherReturn, watcherRunnable.getDir().getAbsolutePath());
				failureCallback(e);
			}
		}

		private boolean endWatcherRunnable(Future<?> watcherResult, String path) {
			try {
				boolean result = watcherResult.get(5, TimeUnit.SECONDS) == null;
				if(result) {
					logger.debug("Watcher thread for "+path+" successfully shutdown.");
				}
				return result;
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				logger.debug("Threading error.  Attempting immediate shutdown.", e);
				watcherResult.cancel(true);
				return watcherResult.isDone();
			}
		}

		public boolean isCancelled() {
			return isUploadCancelled;
		}

		public boolean isSuccessfulShutdown() {
			return successfulShutdown;
		}

	}

	public long getExpectedFileSize() {
		return expectedFileSize;
	}

	public void setExpectedFileSize(long expectedFileSize) {
		this.expectedFileSize = expectedFileSize;
	}

	private class FileUploadProgressWatcherRunnable implements Runnable {
		private boolean disableWatcher;
		private volatile boolean isUploadComplete;
		private long originalDirectorySize;
		private final File dir;
		private final ProfilingProgressUpdateHandler fileUploadProgressUpdater;
		private final long timeout;

		public FileUploadProgressWatcherRunnable(ProfilingProgressUpdateHandler progressUpdater, long timeout, File watchDirectory) {
			disableWatcher = false;
			this.fileUploadProgressUpdater = progressUpdater;
			this.isUploadComplete = false;
			this.dir = watchDirectory;
			this.timeout = timeout;
			originalDirectorySize = 0;
			for(String child : dir.list()) {
				originalDirectorySize += new File(child).length();
			}
		}

		@Override
		public void run() {
			long t1 = System.currentTimeMillis();
			while(System.currentTimeMillis() - t1 < timeout && !disableWatcher) {
				// need to synchronize so upload doesn't complete, move onto next progress state, and then get updated
				// with this updateSize
				synchronized(this) {
					if(!isUploadComplete) {
						int size = 0;
						for(File child : dir.listFiles()) {
							size += child.length();
						}
						long updatedSize = size - originalDirectorySize;
						fileUploadProgressUpdater.handleProgressUpdate(updatedSize);
					} else {
						return;
					}
				}
				try {
					Thread.sleep(50);
				} catch(InterruptedException e) {
					logger.error("Threading interrupt watching directory " + dir + ".", e);
					disableWatcher = true;
				}
			}
		}

		public boolean isUploadComplete() {
			return isUploadComplete;
		}

		// need to synchronize so upload is complete and progress updater are set together
		public synchronized void setUploadComplete() {
			this.isUploadComplete = true;
			if(fileUploadProgressUpdater instanceof SimpleProgressUpdater) {
				((SimpleProgressUpdater)fileUploadProgressUpdater).removeDescriptionCallback();
			}
		}

		public File getDir() {
			return dir;
		}


	}
}
