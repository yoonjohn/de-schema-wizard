package com.deleidos.dp.profiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.ColsEntry;
import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.beans.RowEntry;
import com.deleidos.dp.beans.Schema;

public class SchemaReverseGeocodingProfiler extends AbstractReverseGeocodingProfiler<Schema> {
	private static final Logger logger = Logger.getLogger(SchemaReverseGeocodingProfiler.class);
	private boolean calledLoadOutput = false;

	@Override
	public void load(com.deleidos.dp.profiler.api.ProfilerRecord record) {
		if(!calledLoadOutput) {
			logger.error("Schema reverse geocoding profiler received a load call.  This is undefined functionality.");
			calledLoadOutput = true;
		}
		return;
	};

	public void initializeWithMap(Map<String, Profile> profileMap) {
		List<CoordinateProfile> tempFrequencyList = new ArrayList<CoordinateProfile>();
		for(String key : profileMap.keySet()) { 
			Profile profile = profileMap.get(key);
			if(Interpretation.isCoordinate(profile.getInterpretation())) {
				Optional<Histogram> optionalHistogram = profile.getDetail().getHistogramOptional();
				if(optionalHistogram.isPresent()) {
					RegionData regionData = optionalHistogram.get().getRegionData();
					if(regionData != null) {
						String latitudeKey = regionData.getLatitudeKey();
						String longitudeKey = regionData.getLongitudeKey();
						if(!profileMap.containsKey(latitudeKey) || !profileMap.containsKey(longitudeKey)) {
							logger.warn("Invalid lat/lng pair detected.  Defaulting to unknown interpretation.");
							profile.setInterpretation(Interpretation.UNKNOWN);
						}

						CoordinateProfile coordinateProfile = new CoordinateProfile(latitudeKey, longitudeKey);
						if(getCoordinateProfile(tempFrequencyList, latitudeKey, longitudeKey) != null) {
							// other coordinate already taken into account
							continue;
						}
						int addedIndex = coordinateProfiles.size();
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
		tempFrequencyList.forEach((x) -> updateFrequencyList(x));
	}

	private void updateFrequencyList(CoordinateProfile coordinateProfile) {
		CoordinateProfile existingCoordinateProfile = 
				super.getCoordinateProfile(coordinateProfiles, coordinateProfile.getLatitude(), coordinateProfile.getLongitude());
		if(existingCoordinateProfile != null) {
			Map<String, Integer> existingFrequencyMapping = existingCoordinateProfile.getCountryFrequencyMapping();
			Map<String, Integer> updatesForFrequencyMapping = coordinateProfile.getCountryFrequencyMapping();
			for(String countryKey : updatesForFrequencyMapping.keySet()) {
				Integer newMapCount = (existingFrequencyMapping.containsKey(countryKey)) 
						? (existingFrequencyMapping.get(countryKey) + updatesForFrequencyMapping.get(countryKey)) : updatesForFrequencyMapping.get(countryKey);

						logger.debug("Updating count of " + countryKey + " from " + existingFrequencyMapping.get(countryKey) + " to " + newMapCount + " in " + coordinateProfile.getLatitude()+","+coordinateProfile.getLongitude()+".");

						existingFrequencyMapping.put(countryKey, newMapCount);
			}
			coordinateProfile.setCountryFrequencyMapping(existingFrequencyMapping);
			coordinateProfiles.set(existingCoordinateProfile.getIndex(), existingCoordinateProfile);
		} else {
			int addedIndex = coordinateProfiles.size();
			coordinateProfiles.add(coordinateProfile);
			coordinateProfile.setIndex(addedIndex);
			logger.debug("Adding new coordinate profile " +coordinateProfile.getLatitude() +","+coordinateProfile.getLongitude()+" with "+coordinateProfile.getCountryFrequencyMapping()+".");
		} 
	}

	@Override
	public Schema asBean() {
		Schema schema = getBean();

		for(CoordinateProfile coordinateProfile : coordinateProfiles) {
			Profile latProfile = schema.getsProfile().get(coordinateProfile.getLatitude());
			Profile lngProfile = schema.getsProfile().get(coordinateProfile.getLongitude());
			if(latProfile == null || lngProfile == null) {
				logger.error("Keypair " + coordinateProfile.getLatitude() + "/" + coordinateProfile.getLongitude() + " expected as coordinate pair but not found in map.  Partial coordinate merge?");
				logger.warn("Ignoring mapping for \"" + coordinateProfile.getLatitude() + "\" and \"" + coordinateProfile.getLongitude() + "\".");
				break;
			}
			Detail detail1 = latProfile.getDetail();
			Detail detail2 = lngProfile.getDetail();

			detail1.getHistogramOptional().ifPresent(x->x.setRegionData(regionDataFromIndex(coordinateProfile.getIndex())));
			detail2.getHistogramOptional().ifPresent(x->x.setRegionData(regionDataFromIndex(coordinateProfile.getIndex())));

			latProfile.setDetail(detail1);
			lngProfile.setDetail(detail2);

		}
		return schema;
	}

	private RegionData regionDataFromIndex(int index) {
		CoordinateProfile coordinateProfile = coordinateProfiles.get(index);
		RegionData regionData = new RegionData();
		regionData.setCols(Arrays.asList(new ColsEntry("Country"), new ColsEntry("Frequency")));
		List<RowEntry> rows = rowsFromMapping(coordinateProfile.getCountryFrequencyMapping());
		regionData.setRows(rows);
		regionData.setLatitudeKey(coordinateProfile.getLatitude());
		regionData.setLongitudeKey(coordinateProfile.getLongitude());
		return regionData;
	}

}
