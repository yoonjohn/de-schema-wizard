:: Mongo Initialization Script
:: For Windows environments, must run a Maven install before executing this script.
:: Reinitialize the Mongo database using the src/main/resources/domains directory
:: See src/main/resources/*_template.yml files for conventions.
:: Also re-initialize reverse geocoding data.
:: It will drop all existing reverse geo and domain data
:: The Mongo daemon must be running for this script to work, call "mongod" to start the mongo server

mongo geo2 --eval "db.dropDatabase()"
mongo domain_manager --eval "db.dropDatabase()"
mongo reverse_geo --eval "db.dropDatabase()"
mongo reverse-geo --eval "db.dropDatabase()"

:: Import necessary files.
mongoimport --db reverse_geo --collection geospatial --file "%~dp0\country_polygon_data.json"
mongoimport --db domain_manager --collection domains --file ""%~dp0\target\domain_imports.json"
mongoimport --db domain_manager --collection interpretations --file ""%~dp0\target\interpretation_imports.json"

echo Creating geospatial index.
mongo reverse_geo --eval "db.geospatial.createIndex({geometry: '2dsphere'})"
