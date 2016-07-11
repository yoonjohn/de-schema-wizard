# DigitalEdge Schema Wizard

## Introduction
The DigitalEdge Schema Wizard automates the process of evaluating, interpreting and mapping data sources, as well as the creation or updating of schemas. 

The discovery process is accomplished when the software deeply profiles the data sets through computing a series of statistical characteristics across the data set, and evaluating the data fields to determine the interpretation of those fields in the given application domain. 

The examined data may be structured or unstructured and its content may come in various forms such as: comma separated values, JSON, XML, name/value pairs, plain text, and documents. Additionally, multiple instances of the data may come in compressed file formats. The data engineer can collect the samples as files prior to using Schema Wizard or may direct live streams into a Schema Wizard session.

Armed with this information, it then automatically proposes a schema, or merges the data set into an existing schema. After the automated recommendations are made, the data engineer either confirms them or makes adjustments as needed.

The result is a schema that merges multiple data sources, such as from different databases or data streams. Subsequently, the schemas are used by other tools in the DigitalEdge suite.

##  Getting Started
To begin using the template, download or clone the  [deleidos/de-schema-wizard](https://github.com/deleidos/de-schema-wizard) project to your computer. If using archived containers (container.tar), then refer to Schema Wizard Application from .tar (Local archive).

## Distribution
The DigitalEdge Schema Wizard is packaged in three docker containers: an H2 database container, a reverse geocode container, and a webapp container. 

H2 provides persistence for the ‘Catalog’ of schemas along with metrics for the data sample files used in schema generation. It also provides working space for the services. The reverse geocode container provides the services with a lookup capability for matching coordinates to country codes and utilizes a Mongo database with preloaded data. The webapp is a Jetty instance that serves the presentation layer and embeds the services layer. The presentation layer’s client-side is written in AngularJS and the server-side implements Jersey in a Spring environment running under Jetty. The services are written in Java.

### Schema Wizard Application from .tar (Local archive)

#### Importing Docker images
cat sw-h2.tar | sudo docker import - sw-h2:2.9.1a
cat reverse-geo.tar | sudo docker import - reverse-geo:2.9.1a
cat sw-webapp.tar | sudo docker import - sw-webapp:2.9.1a
 
#### Starting Docker containers from images
H2 Container 
sudo docker run -d -it -p 127.0.0.1:9123:9123 -e "H2_DB_DIR=/usr/local/h2" -e "H2_DB_NAME=data" --name sw-h2 sw-h2:2.9.1a /bin/bash
sudo docker exec -it sw-h2 bash
nohup java -classpath /usr/local/h2/h2-database-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.deleidos.hd.h2.H2Database -tcpAllowOthers -tcpPort 9123 &
*Can take up to a minute
exit
 
Reverse Geo-code container
sudo docker run -d -p 27017:27017 --name reverse-geo reverse-geo:2.9.1a /usr/local/start_and_import.sh
 
Webapp Container 
sudo docker run -d -it -p 80:8080 -e "H2_DB_DIR=/usr/local/h2" -e "H2_DB_NAME=data" --link sw-h2:h2-db --link reverse-geo:mongo-rg --name sw-webapp sw-webapp:2.9.1a /bin/bash
sudo docker exec -it sw-webapp bash
cd /opt/jetty
nohup java -jar start.jar &
*Can take up to a minute
exit

### Requirements
In order to actively run the template, you will need the following libraries installed on your machine:
1.	Mongo 3.2
2.	Bower
3.	JRE 8 

### Install Dependencies
The template project requires external development tools and frontend framework code to operate correctly. The development tools include task runners and the karma testing suite while the front-end packages include bootstrap and angular, among others.
·	Development tools are installed via npm, the node package manager. Tools are listed in package.json.
·	Frontend code is installed via bower, a client-side code package manager. Front-end libraries are listed in bower.json.
Using a command shell, navigate to the location of the template project.
The template is preconfigured for npm to automatically run bower so we can simply execute:
npm install


## Maintainers
The DigitalEdge Schema Wizard is managed by the Leidos DigitalEdge Team. Leidos is headquartered at:

11951 Freedom Drive
Reston, VA 20190
(571) 526-6000

## License
The DigitalEdge Schema Wizard is licensed for use under the Apache 2.0 license.
