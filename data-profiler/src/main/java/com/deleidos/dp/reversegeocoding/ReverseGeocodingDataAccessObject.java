package com.deleidos.dp.reversegeocoding;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonString;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dp.reversegeocoding.ReverseGeocoder.ReverseGeocodingWorker;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class ReverseGeocodingDataAccessObject implements ReverseGeocodingWorker{
	private static final Logger logger = Logger.getLogger(ReverseGeocodingDataAccessObject.class);
	public static final String mongoPropertiesFile = "mongo-server.properties";
	private static ReverseGeocodingDataAccessObject mongoConnection = null;
	private String mongoInitFile;
	private MongoClient mongoClient = null;
	private MongoDatabase database = null;
	private MongoCollection<Document> collection = null;
	protected boolean isLive;

	protected ReverseGeocodingDataAccessObject() {
		isLive = false;
	}

	private void initProperties() {
		mongoInitFile = (System.getenv("MONGO_INIT_PROPERTIES") != null) ? System.getenv("MONGO_INIT_PROPERTIES") : null;
		String host;
		int port;
		String dbName;
		String collectionString;
		if(mongoInitFile == null) {
			try {
				String fullConnectionString = (System.getenv("MONGO_RG_PORT") != null) ? System.getenv("MONGO_RG_PORT") : "tcp://127.0.0.1:27017";
				String[] splits = fullConnectionString.split(":");
				if(splits.length != 3) {
					logger.error("Invalid MONGO_RG_PORT environmental variable.  Should be of the form \"tcp://<host>:<port>\"");
				}
				host = splits[1].substring(2);
				String portString = splits[2];
				port = Integer.valueOf(portString);
				dbName = (System.getenv("MONGO_RB_DB_NAME") != null) ? System.getenv("MONGO_RG_DB_NAME") : "geo2";
				collectionString = (System.getenv("MONGO_RG_DB_COLLECTION") != null) ? System.getenv("MONGO_RG_DB_COLLECTION") : "geospatial";

			} catch (Exception e) {
				logger.error(e);
				logger.error(e.getMessage());
				logger.error("Exception initializing Mongo database with environmental variables.");
				mongoClient = null;
				return;
			} 
		} else {
			Properties propertiesFile = new Properties();
			try {
				propertiesFile.load(getClass().getResourceAsStream("/" + mongoPropertiesFile));
				host = propertiesFile.getProperty("mongo.db.host");
				port = Integer.valueOf(propertiesFile.getProperty("mongo.db.port"));
				dbName = propertiesFile.getProperty("mongo.db.name");
				collectionString = propertiesFile.getProperty("mongo.db.collection");
			} catch (IOException e) {
				logger.error("Could not retrieve "+mongoPropertiesFile+" properties file from classpath.  Using defaults.");
				logger.error(e);
				host = "127.0.0.1";
				port = 27017;
				dbName = "geospatialDB";
				collectionString = "geospatialpolygons";
			}
		}
		logger.info("Mongo client connecting on " + host + ":" + port + " with " + dbName + " database.");
		mongoClient = new MongoClient(host, port);
		database = mongoClient.getDatabase(dbName);
		collection = database.getCollection(collectionString);
		isLive = true;
	}

	public static ReverseGeocodingDataAccessObject getInstance() {
		if(mongoConnection == null) {
			mongoConnection = new ReverseGeocodingDataAccessObject();
			//mongoConnection.initProperties();
		}
		return mongoConnection;
	}

	public static ReverseGeocodingDataAccessObject getInstance(ReverseGeocodingDataAccessObject testDAO) {
		if(mongoConnection == null) {
			mongoConnection = testDAO;
			logger.info("Instance passed to Reverse Geocoding DAO.  This should only be used for testing as it will return dummy data.");
		}
		return mongoConnection;
	}

	private MongoClient getMongoClient() {
		return mongoClient;
	}

	private void setMongoClient(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
	}

	private MongoDatabase getMongoDatabase() {
		return database;
	}

	private void setMongoDatabase(MongoDatabase mongoDatabase) {
		this.database = mongoDatabase;
	}

	public MongoCollection<Document> getCollection() {
		return collection;
	}

	public void setCollection(MongoCollection<Document> collection) {
		this.collection = collection;
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

	@Override
	public List<String> getCountryCodesFromCoordinateList(List<Double[]> coordinates) {
		List<String> countryCodes = new ArrayList<String>();
		for(Double[] coordinate : coordinates) {
			BasicDBObject dbObject = new BasicDBObject();
			BsonDocument bson = latLngToGeoIntersectsObject(coordinate[0], coordinate[1]); //latLngToNearBsonQuery(latitude, longitude, maxMiles);
			dbObject.append("geometry", bson);
			Iterable<Document> fi = collection.find(dbObject).limit(1);
			if(fi.iterator().hasNext()) {
				Document document = fi.iterator().next();
				JSONObject json = new JSONObject(document.toJson());
				String countryCode = json.getJSONObject("properties").getString("name_long");
				countryCodes.add(countryCode);
			} 
		}
		return countryCodes;
	}

	private String getCountryCodeFromCoordinates(double latitude, double longitude, double maxMiles) {
		BasicDBObject dbObject = new BasicDBObject();
		BsonDocument bson = latLngToGeoIntersectsObject(latitude, longitude); //latLngToNearBsonQuery(latitude, longitude, maxMiles);
		dbObject.append("geometry", bson);
		Iterable<Document> fi = collection.find(dbObject).limit(1);
		if(fi.iterator().hasNext()) {
			Document document = fi.iterator().next();
			JSONObject json = new JSONObject(document.toJson());
			String countryCode = json.getJSONObject("properties").getString("name_long");
			return countryCode;
		} else {
			return null;
		}
	}

	public boolean isLive() {
		return isLive;
	}

	public void setLive(boolean isLive) {
		this.isLive = isLive;
	}
}
