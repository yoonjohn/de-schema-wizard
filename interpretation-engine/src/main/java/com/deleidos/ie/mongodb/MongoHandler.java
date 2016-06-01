package com.deleidos.ie.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.log4j.Logger;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonString;
import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;

public final class MongoHandler {
	private static Logger logger = Logger.getLogger(MongoHandler.class);

	/* Used for establishing a database connection */
	private static final String DB_HOST = "localhost";
	private static final int DB_PORT = 27017;
	public static final String INTERPRETATION_DATABASE = "interpretation_engine";
	public static final String INTERPRETATION_COLLECTION = "interpretations";
	public static final String REVERSE_GEO_DATABASE = "reverse-geo";
	public static final String REVERSE_GEO_COLLECTION = "geospatial";

	/* Mongo objects */
	public static MongoClient mongoClient;
	public static MongoDatabase interpretationDb;
	private static MongoCollection<Document> interpretationCollection;
	public static MongoDatabase reverseGeoDb;
	private static MongoCollection<Document> reverseGeoCollection;

	static {		
		mongoClient = new MongoClient(DB_HOST, DB_PORT);
		
		interpretationDb = mongoClient.getDatabase(INTERPRETATION_DATABASE);
		interpretationCollection = interpretationDb.getCollection(INTERPRETATION_COLLECTION);
		
		reverseGeoDb = mongoClient.getDatabase(REVERSE_GEO_DATABASE);
		reverseGeoCollection = reverseGeoDb.getCollection(REVERSE_GEO_COLLECTION);
	}

	public static void main(String[] args) {
		System.out.println("hi");
	}
	
	public static String test() {
		return "Mongo Handler successfully reached.";
	}
	
	public static BsonDocument latLngToNearBsonQuery(double latitude, double longitude, double maxMiles) {
		double maxMeters = maxMiles * 1609.34;
		BsonDocument bson = new BsonDocument();
		BsonDocument nearBson = new BsonDocument();
		BsonArray coordinates = new BsonArray();
		coordinates.add(new BsonDouble(longitude));
		coordinates.add(new BsonDouble(latitude));
		BsonDocument geometry = new BsonDocument();
		geometry.put("type", new BsonString("Point"));
		geometry.put("coordinates", coordinates);
		nearBson.put("$geometry", geometry);
		nearBson.put("$maxDistance", new BsonDouble(maxMeters));
		nearBson.put("$minDistance", new BsonDouble(0));
		bson.put("$near", nearBson);
		return bson;
	}

	public static BsonDocument latLngToGeoIntersectsObject(double latitude, double longitude) {
		BsonDocument geoIntersects = new BsonDocument();
		BsonDocument geometry = new BsonDocument();
		geometry.put("type", new BsonString("Point"));
		BsonArray coordinates = new BsonArray();
		coordinates.add(new BsonDouble(longitude));
		coordinates.add(new BsonDouble(latitude));
		geometry.put("coordinates", coordinates);
		geoIntersects.put("$geoIntersects", new BsonDocument("$geometry",geometry));
		return geoIntersects;
	}

	public static double radiansFromMiles(double miles) {
		final double earthsRadiusInMiles = 3958.756;
		double radians = miles/earthsRadiusInMiles;
		return radians;
	}

	public static String getCountryCodeFromCoordinates(double latitude, double longitude, double maxMiles) {
		BasicDBObject dbObject = new BasicDBObject();
		BsonDocument bson = latLngToGeoIntersectsObject(latitude, longitude); //latLngToNearBsonQuery(latitude, longitude, maxMiles);
		dbObject.append("geometry", bson);
		Iterable<Document> fi = reverseGeoCollection.find(dbObject).limit(1);
		if(fi.iterator().hasNext()) {
			Document document = fi.iterator().next();
			JSONObject json = new JSONObject(document.toJson());
			String countryCode = json.getJSONObject("properties").getString("name_long");
			return countryCode;
		} else {
			return null;
		}
	}

	// Private methods
	private MongoHandler() {
		// This is never run
	}
}
