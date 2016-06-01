package com.deleidos.dp.reversegeocoding;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class ReverseGeocoder {
	private static final Logger logger = Logger.getLogger(ReverseGeocoder.class);
	private ReverseGeocodingDataAccessObject reverseGeocodingBackend;
	private ReverseGeocoderCallbackListener callbackListener = null;

	public ReverseGeocoder() {
		reverseGeocodingBackend = ReverseGeocodingDataAccessObject.getInstance();
	}
	
	public ReverseGeocoder(TestReverseGeocodingDataAccessObject trgdao) {
		logger.info("Test reverse geocoder initialized.");
	}
	
	public interface ReverseGeocoderCallbackListener {
		public void handleResult(int coordinateProfileIndex, List<String> resultingCountryNames);
	}
	
	public void setCallbackListener(ReverseGeocoderCallbackListener listener) {
		this.callbackListener = listener;
	}
	
	public void getCountriesFromLatLngsASync(int coordinateProfileIndex, List<Double[]> latLngs) {
		// mimic the functionality of a separate container with another thread for now
		if(latLngs.isEmpty()) {
			if(callbackListener != null) {
				synchronized(callbackListener) {
					callbackListener.handleResult(coordinateProfileIndex, new ArrayList<String>());
				}
			}
			return;
		}
		List<Double[]> latLngsCopy = new ArrayList<Double[]>(latLngs);
		Thread t = new Thread() {
			@Override
			public void run() {
				List<String> resultList = reverseGeocodingBackend.getCountryCodesFromCoordinateList(latLngsCopy);
				if(callbackListener != null) {
					synchronized(callbackListener) {
						// nothing else should be changing values in the callbackListener at this point
						callbackListener.handleResult(coordinateProfileIndex, resultList);
					}
				}
			};
		};
		t.start();
	}
	
}
