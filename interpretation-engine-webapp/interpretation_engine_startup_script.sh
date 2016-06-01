#!/bin/bash

echo Starting Mongo.
mongod --fork --logpath /var/log/mongodb.log --pidfilepath /var/log/mongo.pid
MONGO_PID=$!
sleep 5
echo Importing data...
mongoimport --db reverse-geo --collection geospatial --file /usr/local/country_polygon_data.json
echo Data imported.  Indexing...
mongo --eval 'db.geospatial.createIndex({geometry: "2dsphere"})'
echo Geospatial data indexed. Mongo ready.

echo Starting Jetty
cd /usr/local/jetty
java -jar start.jar &
echo Jetty ready.

while [ -s /var/log/mongo.pid ]
do  
sleep 1
done
