#!/bin/bash

# Mongo initialization for container environment

echo Starting Mongo.
mongod --fork --logpath /var/log/mongodb.log --pidfilepath /var/log/mongo.pid
MONGO_PID=$!

sleep 5

echo Importing dynamically generated data...
mongoimport --db reverse_geo --collection geospatial --file deployment/country_polygon_data.json
mongoimport --db domain_manager --collection domains --file deployment/target/domain_imports.json
mongoimport --db domain_manager --collection interpretations --file deployment/target/interpretation_imports.json

echo Data imported. 

echo Indexing geospatial data...
mongo reverse_geo --eval "db.geospatial.createIndex({geometry: '2dsphere'})"
echo Geospatial data indexed. Mongo ready.

while [ -s /var/log/mongo.pid ]
do
sleep 1
done
