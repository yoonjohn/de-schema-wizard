# Schema Wizard

[Detailed User Guide](docs/readme-ext/detailed.md)

[Developer Documentation](docs/readme-ext/contribute.md)

[Known Issues And Security](docs/readme-ext/known-issues.md)
## Version

3.0.0-beta2

## About
Schema Wizard is an automation driven, human verified data modeling tool.
* Provide data samples
* Verify automated analysis
* Merge similar fields 
* Export your schema

## Installation

Requirements - 
* Linux environment
* AppArmor
* Docker 1.11
* Docker Compose 1.7.1

Install [docker-compose][docker-compose] and clone this repository.  Then, from the project root directory run:

    sudo apparmor_parser -r -W sw-script-profile
    sudo docker-compose up

We recommend you start with the [example files](docs/example-data) in this project.  When you start up Schema Wizard, a guided tour will help you through your first schema creation.  And remember, always check the ![Bus](/docs/readme-ext/blue-tour-bus.jpg "Tour Bus") and ![Help](/docs/readme-ext/blueQuestionMark_whiteCalloutBg.jpg "Help Button") for guidance!
    
## Use

In addition to automated data sample processing, Schema Wizard has an advanced feature called the [Interpretation Engine](docs/readme-ext/detailed.md#interpretation-engine).  When you are ready to get into the more advanced features of Schema Wizard, head over to the [Detailed Documentation](docs/readme-ext/detailed.md) for more information. 

## Supported Formats
Schema Wizard uses a best effort detection and parsing model.  For any file Schema Wizard receives, it will attempt to extract fields as either numbers or strings.

Basic Formats:
* CSV (text/csv) - Comma Separated Values
* JSON (application/json) - JavaScript Object Notation
* CEF (application/cef) - Common Events Format
* XML (application/xml) - **well formed** Extensible Markup Lanuage
 
Application Formats:
* PCAP (application/vnd.tcpdump.pcap) - Packet Capture
* PDF (application/pdf) - content must be one of the "Basic Formats"
* MS Word (application/vnd.openxmlformats-officedocument.wordprocessingml.document) - content must be one of the "Basic Formats" 

Compressed Formats:
* ZIP (application/zip) - archive of any of the above formats

If Schema Wizard finds a content type that could contain other content types (e.g. a zip of CSV's or a PDF containing XML), it will recursively extract embedded content until it finds numeric or string fields.  For more information on Schema Wizard's parsing strategy, see [Parsing Details](docs/readme-ext/detailed.md#data-samples).

## Supported Browsers
![Chrome](/docs/readme-ext/chrome-icon.jpg "Chrome 51") ![Firefox](/docs/readme-ext/firefox-icon.jpg "Firefox 47") ![Opera](/docs/readme-ext/opera-icon.jpg "Opera 38") ![Internet Explorer](/docs/readme-ext/ie-icon.jpg "IE 11+") ![Safari](/docs/readme-ext/safari-icon.jpg "Safari 9.1")

## Technologies
Schema Wizard uses the following open sourced technologies:
* [Docker][docker]
* [Java][java]
* [Python][python]
* [H2][h2]
* [MongoDB][mongo]
* [Apache Tika][tika]
* [Apache Maven][maven]
* [Redis][redis]
* [Jetty][jetty]
* [Flask][flask]
* [npm][npm]
* [Bower][bower]
* [Grunt][grunt]
* [AngularJS][angular]
* [Ace][ace]
* Credit for Geocoding dataset goes to [ThetmaticMapping][geodata].
* Credit for conversion of [ThetmaticMapping][geodata] goes to [Ogre Web Client][ogre]

## Contribute

Schema Wizard is happy to be a part of the open source community.  See [Contribute](docs/readme-ext/contribute.md) to help improve Schema Wizard.

## Known Issues

For a list of known issues, please visit our [Known Issues Page](docs/readme-ext/known-issues.md).

## Maintainers
The DigitalEdge Schema Wizard is managed by the Leidos DigitalEdge Team. Leidos is headquartered at:

11951 Freedom Drive
Reston, VA 20190
(571) 526-6000

## License
Schema Wizard is licensed for use under the Apache 2.0 license.

[//]: # (Links)

   [java]: <https://www.java.com/>
   [python]: <https://www.python.org/>
   [docker]: <https://www.docker.com/>
   [h2]: <http://www.h2database.com/>
   [mongo]: <https://www.mongodb.com/>
   [jetty]: <http://www.eclipse.org/jetty/>
   [redis]: <http://redis.io/>
   [tika]: <https://tika.apache.org/>
   [angular]: <https://angularjs.org/>
   [flask]: <http://flask.pocoo.org/>
   [maven]: <https://maven.apache.org/>
   [npm]: <https://www.npmjs.com/>
   [bower]: <https://bower.io/>
   [grunt]: <http://gruntjs.com/>
   [ace]: <https://ace.c9.io/>
   [docker-compose]: <https://docs.docker.com/compose/>
   
   [geodata]: <http://thematicmapping.org>
   [ogre]: <http://ogre.adc4gis.com/>
   
