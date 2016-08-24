package com.deleidos.dp.profiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.ColsEntry;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.beans.RowEntry;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.reversegeocoding.CoordinateProfile;

public class ReverseGeocodingLoader { 
	private static final Logger logger = Logger.getLogger(ReverseGeocodingLoader.class);
	//private boolean calledLoadOutput = false;

	public static int getCountryIndexByName(List<RowEntry> rows, String name) {
		for(int i = 0; i < rows.size(); i++) {
			RowEntry row = rows.get(i);
			if(row.getKey().equals(name)) {
				return i;
			}
		}
		return -1;
	}
	
	private static List<CoordinateProfile> profileToCoordinateProfiles(Map<String, Profile> profileMap) {
		List<CoordinateProfile> tempFrequencyList = new ArrayList<CoordinateProfile>();
		for(String key : profileMap.keySet()) { 
			Profile profile = profileMap.get(key);
			if(Interpretation.isCoordinate(profile.getInterpretation())) {
				Optional<Histogram> histogram = profile.getDetail().getHistogramOptional();
				if(histogram.isPresent()) {
					RegionData regionData = histogram.get().getRegionData();
					if(regionData != null) {
						String latitudeKey = regionData.getLatitudeKey();
						String longitudeKey = regionData.getLongitudeKey();

						CoordinateProfile coordinateProfile = new CoordinateProfile(latitudeKey, longitudeKey);
						if(tempFrequencyList.contains(coordinateProfile)) {
							continue;
						}
						int addedIndex = tempFrequencyList.size();
						coordinateProfile.setIndex(addedIndex);
						coordinateProfile.setCountryFrequencyMapping(regionData.toMap());

						tempFrequencyList.add(coordinateProfile);
						logger.info("Added key: " + latitudeKey + "," + longitudeKey);
					}
				}
			} else {
				logger.debug("Not initializing any reverse geocoding for "+key+".");
			}
		}
		return tempFrequencyList;
	}
	
	public static List<CoordinateProfile> addAllCoordinateProfiles(List<CoordinateProfile> existing, List<CoordinateProfile> additions) {
		additions.forEach(x->updateCoordinateProfile(existing, x));
		return existing;
	}
	
	public static List<CoordinateProfile> getCoordinateProfiles(Map<String, Profile> profileMapping) {
		return addAllCoordinateProfiles(new ArrayList<CoordinateProfile>(), 
				profileToCoordinateProfiles(profileMapping));
	}
		
	/*public static List<CoordinateProfile> calculateRegionDataCounts(Schema existingSchema, List<DataSample> samples) {
		List<CoordinateProfile> coordinateProfile = new ArrayList<CoordinateProfile>();
		if(existingSchema != null) {
			addAllCoordinateProfiles(coordinateProfile, profileToCoordinateProfiles(existingSchema.getsProfile()));
		}
		samples.forEach(x->addAllCoordinateProfiles(coordinateProfile, profileToCoordinateProfiles(x.getDsProfile())));
		return coordinateProfile;
	}*/

	private static List<CoordinateProfile> updateCoordinateProfile(List<CoordinateProfile> existingCoordinateProfiles,
			CoordinateProfile coordinateProfile) {
		int index = existingCoordinateProfiles.indexOf(coordinateProfile);
		if(index > -1) {
			CoordinateProfile existingCoordinateProfile = existingCoordinateProfiles.get(index);
			Map<String, Integer> existingFrequencyMapping = existingCoordinateProfile.getCountryFrequencyMapping();
			Map<String, Integer> updatesForFrequencyMapping = coordinateProfile.getCountryFrequencyMapping();
			for(String countryKey : updatesForFrequencyMapping.keySet()) {
				Integer newMapCount = (existingFrequencyMapping.containsKey(countryKey)) 
						? (existingFrequencyMapping.get(countryKey) + updatesForFrequencyMapping.get(countryKey)) : updatesForFrequencyMapping.get(countryKey);

						logger.debug("Updating count of " + countryKey + " from " + existingFrequencyMapping.get(countryKey) + " to " + newMapCount + " in " + coordinateProfile.getLatitude()+","+coordinateProfile.getLongitude()+".");

						existingFrequencyMapping.put(countryKey, newMapCount);
			}
			coordinateProfile.setCountryFrequencyMapping(existingFrequencyMapping);
			existingCoordinateProfiles.set(existingCoordinateProfile.getIndex(), existingCoordinateProfile);
		} else {
			int addedIndex = existingCoordinateProfiles.size();
			existingCoordinateProfiles.add(coordinateProfile);
			coordinateProfile.setIndex(addedIndex);
			logger.debug("Adding new coordinate profile " +coordinateProfile.getLatitude() +","+coordinateProfile.getLongitude()+" with "+coordinateProfile.getCountryFrequencyMapping()+".");
		}
		return existingCoordinateProfiles;
	}

	/*public static Map<String, RegionData> generateRegionDataMap(Schema schema, List<DataSample> samples) {
		Map<String, RegionData> regionDataMapping = new HashMap<String, RegionData>();
		List<CoordinateProfile> coordinateProfiles = calculateRegionDataCounts(schema, samples);
		
		for(CoordinateProfile coordinateProfile : coordinateProfiles) {

			RegionData r1 = regionDataFromIndex(coordinateProfile);
			RegionData r2 = regionDataFromIndex(coordinateProfile);

			regionDataMapping.put(coordinateProfile.getLatitude(), r1);
			regionDataMapping.put(coordinateProfile.getLongitude(), r2);
			
		}
		return regionDataMapping;
	}*/

	public static RegionData regionDataFromCoordinateProfile(CoordinateProfile coordinateProfile) {
		RegionData regionData = new RegionData();
		regionData.setCols(Arrays.asList(new ColsEntry("Country"), new ColsEntry("Frequency")));
		List<RowEntry> rows = RegionData.rowsFromMapping(coordinateProfile.getCountryFrequencyMapping());
		regionData.setRows(rows);
		regionData.setLatitudeKey(coordinateProfile.getLatitude());
		regionData.setLongitudeKey(coordinateProfile.getLongitude());
		return regionData;
	}
	
}
