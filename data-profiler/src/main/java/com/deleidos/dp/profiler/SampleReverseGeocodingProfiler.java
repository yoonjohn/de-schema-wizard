package com.deleidos.dp.profiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.ColsEntry;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.beans.RowEntry;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.profiler.AbstractReverseGeocodingProfiler.CoordinateProfile;

public class SampleReverseGeocodingProfiler extends AbstractReverseGeocodingProfiler<DataSample> {
	private static final Logger logger = Logger.getLogger(SampleReverseGeocodingProfiler.class);

	protected static List<String> emptyCoordinatePair() {
		return new ArrayList<String>(Arrays.asList(null, null));
	}

	public void initializeWithSampleBean(DataSample sample) {
		setBean(sample);
		Map<String, Profile> latProfiles = new HashMap<String, Profile>();
		Map<String, Profile> lngProfiles = new HashMap<String, Profile>();
		
		for(String key : sample.getDsProfile().keySet()) { 
			Profile profile = sample.getDsProfile().get(key);
			if(Interpretation.isLatitude(profile.getInterpretation())) {
				latProfiles.put(key, profile);
			} else if(Interpretation.isLongitude(profile.getInterpretation())) {
				lngProfiles.put(key, profile);
			}
		}
		
		attemptToMatchLatLongPairs(latProfiles, lngProfiles);

	}

	private void attemptToMatchLatLongPairs(Map<String, Profile> latProfiles, Map<String, Profile> lngProfiles) {
		
		Iterator<String> latProfilesKeyIter = latProfiles.keySet().iterator();
		while(latProfilesKeyIter.hasNext()) {
			String latProfileKey = latProfilesKeyIter.next();
			String highestConfidenceLongMatch = null;
			double highestConfidence = 0;
			for(String lngProfileKey : lngProfiles.keySet()) {
				double stringMatchConfidence = MetricsCalculationsFacade.jaroWinklerComparison(latProfileKey, lngProfileKey);
				// do string comparison
				// track the highestConfidenceMatch
				// later, maybe implement some sort of position indicator in the profile
				if(stringMatchConfidence > .5) {
					if(stringMatchConfidence > highestConfidence) {
						highestConfidenceLongMatch = lngProfileKey;
						highestConfidence = stringMatchConfidence;
					}
				}
			}
			if(highestConfidenceLongMatch != null) {
				// match found, remove from profile mappings
				CoordinateProfile coordinateProfile = new CoordinateProfile(latProfileKey, highestConfidenceLongMatch);
				int addedIndex = coordinateProfiles.size();
				coordinateProfile.setIndex(addedIndex);
				coordinateProfiles.add(coordinateProfile);
				
				latProfilesKeyIter.remove();
				lngProfiles.remove(highestConfidenceLongMatch);

			}
		}
		
		for(String unmatchedLat : latProfiles.keySet()) {
			logger.warn(unmatchedLat + " was an unmatched latitude.");
		}
		for(String unmatchedLng : lngProfiles.keySet()) {
			logger.warn(unmatchedLng + " was an unmatched longitude.");
		}
	}

	@Override
	public DataSample asBean() {
		super.asBean();
		DataSample sampleWithRegionData = getBean();

		for(CoordinateProfile coordinateProfile : coordinateProfiles) {
			for(String key : Arrays.asList(coordinateProfile.getLatitude(), coordinateProfile.getLongitude())) {
				Profile profile = sampleWithRegionData.getDsProfile().get(key);
				Detail detail = profile.getDetail();

				RegionData regionData = new RegionData();
				regionData.setCols(Arrays.asList(new ColsEntry("Country"), new ColsEntry("Frequency")));
				List<RowEntry> rows = rowsFromMapping(coordinateProfile.getCountryFrequencyMapping());
				regionData.setRows(rows);
				regionData.setLatitudeKey(coordinateProfile.getLatitude());
				regionData.setLongitudeKey(coordinateProfile.getLongitude());

				detail.setRegionDataIfApplicable(regionData);
				profile.setDetail(detail);
			}
		}
		return sampleWithRegionData; // region data set here
	}
	

}
