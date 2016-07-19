package com.deleidos.dp.profiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.RowEntry;
import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateListener;
import com.deleidos.dp.reversegeocoding.ReverseGeocoder;
import com.deleidos.dp.reversegeocoding.ReverseGeocoder.ReverseGeocoderCallbackListener;

public abstract class AbstractReverseGeocodingProfiler<B> implements Profiler, ReverseGeocoderCallbackListener {
	protected GroupingBehavior groupingBehavior = GroupingBehavior.GROUP_ARRAY_VALUES;
	private static final Logger logger = Logger.getLogger(AbstractReverseGeocodingProfiler.class);
	//private Map<List<String>, List<Double[]>> latLngBuffer;
	//protected Map<List<String>, Map<String, Integer>> frequencyMapping;
	protected List<CoordinateProfile> coordinateProfiles;
	private ProfilingProgressUpdateListener progressUpdateListener;
	protected ReverseGeocoder reverseGeocoder;
	//protected boolean geocoderReady = false;
	protected volatile int numberASynchronousReverseGeocodingCallbacks = 0;
	protected volatile int reverseGeocodingAnswers = 0;
	protected int unaffiliatedGeoCount = 0;
	protected int bufferedQueries = 0;
	protected int reverseGeocodeQueries = 0;
	protected int minimumBatchSize = 100;
	protected B bean;

	protected static List<String> emptyCoordinatePair() {
		return new ArrayList<String>(Arrays.asList(null, null));
	}

	public AbstractReverseGeocodingProfiler() {
		coordinateProfiles = new ArrayList<CoordinateProfile>();
		try {
			reverseGeocoder = new ReverseGeocoder();
			// geocoderReady = ReverseGeocodingDataAccessObject.getInstance().isLive();
		} catch (Exception e) {
			logger.error("Geocoder not ready.");
			logger.error(e);
		}
		if(this instanceof ReverseGeocoderCallbackListener) {
			reverseGeocoder.setCallbackListener((ReverseGeocoderCallbackListener)this);
		}
	}

	protected boolean isOtherIndexNull(String[] coordinatePair, int index) {
		return true;
	}

	@Override
	public abstract void load(ProfilerRecord record); 

	protected void sendCoordinateProfileBatchesToReverseGeocoder() throws DataAccessException {
		waitForCallbacks();
		numberASynchronousReverseGeocodingCallbacks = coordinateProfiles.size();
		for(CoordinateProfile coordinateProfile : coordinateProfiles) {
			reverseGeocoder.getCountriesFromLatLngsASync(coordinateProfile.getIndex(), 
					coordinateProfile.getUndeterminedCoordinateBuffer());
			coordinateProfile.getUndeterminedCoordinateBuffer().clear();
		}
	}

	private void waitForCallbacks() throws DataAccessException {
		final long TIMEOUT_MILLIS = 60*1000;
		long start = System.currentTimeMillis(); 
		int awaiting = numberASynchronousReverseGeocodingCallbacks;
		if(awaiting == 0) {
			return;
		}
		logger.debug("Awaiting the return of " + numberASynchronousReverseGeocodingCallbacks + " callbacks.");
		while(numberASynchronousReverseGeocodingCallbacks > 0) {
			try {
				Thread.sleep(100);
				synchronized(this) {
					if(numberASynchronousReverseGeocodingCallbacks != awaiting && numberASynchronousReverseGeocodingCallbacks > 0) {
						logger.debug("Awaiting the return of " + numberASynchronousReverseGeocodingCallbacks + " callbacks.");
						awaiting = numberASynchronousReverseGeocodingCallbacks;
					}
				}
				if(System.currentTimeMillis() - start > TIMEOUT_MILLIS) {
					throw new DataAccessException("Timed out waiting for reverse geocoding calls to return.");
				}
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
	}

	@Override
	public void handleResult(int coordinateIndex, List<String> resultingCountryNames) {
		logger.debug("Handling request " + coordinateProfiles.get(coordinateIndex).getLatitude()+","+coordinateProfiles.get(coordinateIndex).getLongitude());
		CoordinateProfile coordinateProfile = this.coordinateProfiles.get(coordinateIndex);
		for(String country : resultingCountryNames) {
			if(country != null) {
				coordinateProfile.getCountryFrequencyMapping().compute(country, (k, v) -> (v == null) ? 1 : v + 1);
			} else {
				unaffiliatedGeoCount++;
			}
		}
		reverseGeocodingAnswers += resultingCountryNames.size();
		if(numberASynchronousReverseGeocodingCallbacks > 0) {
			numberASynchronousReverseGeocodingCallbacks--;
		}
		if(progressUpdateListener != null) {
			progressUpdateListener.handleProgressUpdate(reverseGeocodingAnswers);
		}
	}

	@Override
	public Object asBean() {
		try {
			sendCoordinateProfileBatchesToReverseGeocoder();
			waitForCallbacks(); 
			//need to wait for last batch to come back
		} catch(DataAccessException e) {
			logger.error(e);
			logger.error("Lost "+bufferedQueries+" reverse geocoding queries due to a connection timeout.");
		}
		logger.info(reverseGeocodeQueries + " total reverse geocoding queries executed.");
		return null;
	}

	protected List<RowEntry> rowsFromMapping(Map<String, Integer> singleFieldFrequencyMapping) {
		List<RowEntry> rows = new ArrayList<RowEntry>();
		for(String key : singleFieldFrequencyMapping.keySet()) {
			rows.add(new RowEntry(key, singleFieldFrequencyMapping.get(key)));
		}
		return rows;
	}

	public int getReverseGeocodeQueries() {
		return reverseGeocodeQueries;
	}

	protected B getBean() {
		return bean;
	}

	protected void setBean(B bean) {
		this.bean = bean;
	}

	protected CoordinateProfile getCoordinateProfile(List<CoordinateProfile> coordinateProfiles, String latitudeKey, String longitudeKey) {
		// O(n)
		for(CoordinateProfile coordinateProfile : coordinateProfiles) {
			if(coordinateProfile.getLatitude().equals(latitudeKey) && coordinateProfile.getLongitude().equals(longitudeKey)) {
				return coordinateProfile;
			}
		}
		return null;
	}

	public int getMinimumBatchSize() {
		return minimumBatchSize;
	}

	public void setMinimumBatchSize(int minimumBatchSize) {
		this.minimumBatchSize = minimumBatchSize;
	}

	public int getReverseGeocodingAnswers() {
		return reverseGeocodingAnswers;
	}

	public ProfilingProgressUpdateListener getProgressUpdateListener() {
		return progressUpdateListener;
	}

	public void setProgressUpdateListener(ProfilingProgressUpdateListener progressUpdateListener) {
		this.progressUpdateListener = progressUpdateListener;
	}

	protected class CoordinateProfile {
		private int index;
		private String latitude;
		private String longitude;
		private List<Double[]> undeterminedCoordinateBuffer;
		private Map<String, Integer> countryFrequencyMapping;

		public CoordinateProfile(String latKey, String lngKey) {
			latitude = latKey;
			longitude = lngKey;
			undeterminedCoordinateBuffer = new ArrayList<Double[]>();
			countryFrequencyMapping = new HashMap<String, Integer>();
		}

		public List<Double[]> getUndeterminedCoordinateBuffer() {
			return undeterminedCoordinateBuffer;
		}

		public void setUndeterminedCoordinateBuffer(List<Double[]> undeterminedCoordinateBuffer) {
			this.undeterminedCoordinateBuffer = undeterminedCoordinateBuffer;
		}

		public Map<String, Integer> getCountryFrequencyMapping() {
			return countryFrequencyMapping;
		}

		public void setCountryFrequencyMapping(Map<String, Integer> countryFrequencyMapping) {
			this.countryFrequencyMapping = countryFrequencyMapping;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public String getLatitude() {
			return latitude;
		}

		public void setLatitude(String latitude) {
			this.latitude = latitude;
		}

		public String getLongitude() {
			return longitude;
		}

		public void setLongitude(String longitude) {
			this.longitude = longitude;
		}

	}
}
