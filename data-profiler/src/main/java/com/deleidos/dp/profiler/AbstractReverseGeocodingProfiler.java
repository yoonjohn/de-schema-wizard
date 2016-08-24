package com.deleidos.dp.profiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.deleidos.dp.accumulator.AbstractProfileAccumulator;
import com.deleidos.dp.beans.RowEntry;
import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateHandler;
import com.deleidos.dp.reversegeocoding.CoordinateProfile;
import com.deleidos.dp.reversegeocoding.ReverseGeocoder;
import com.deleidos.dp.reversegeocoding.ReverseGeocoder.ReverseGeocoderCallbackListener;

public abstract class AbstractReverseGeocodingProfiler<B> implements Profiler, ReverseGeocoderCallbackListener {
	protected GroupingBehavior groupingBehavior = GroupingBehavior.GROUP_ARRAY_VALUES;
	private static final Logger logger = Logger.getLogger(AbstractReverseGeocodingProfiler.class);
	protected Map<String, AbstractProfileAccumulator> accumulatorMapping;
	protected List<CoordinateProfile> coordinateProfiles;
	protected ReverseGeocoder reverseGeocoder;
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
		logger.debug(reverseGeocodeQueries + " total reverse geocoding queries executed.");
		return getBean();
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

	public Map<String, AbstractProfileAccumulator> getAccumulatorMapping() {
		return accumulatorMapping;
	}

	public void setAccumulatorMapping(Map<String, AbstractProfileAccumulator> accumulatorMapping) {
		this.accumulatorMapping = accumulatorMapping;
	}

}
