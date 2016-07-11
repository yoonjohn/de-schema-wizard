# Schema Wizard
## Version

3.0.0-beta-1

## About
Schema Wizard is an automation driven, human verified data modelling tool.
* Provide data samples
* Verify automated analysis
* Merge similar fields 
* Export your schema

## Installation

Requirements - 
* Linux environment
* Docker 1.11
* AppArmor

From the project root directory:

    sudo apparmor_parser -r -W sw-script-profile
    sudo docker-compose command

We recommend you start with the example files in this project: **examples files**.  When you start up Schema Wizard, a guided tour will help you through your first schema creation.  And remember, always look for the ![Bus](/docs/readme-ext/white-tour-bus.jph "Tour Bus") and ![Help](/docs/readme-ext/blueQuestionMark_whiteCalloutBg.jpg "Help Button") for guidance!
    
## Use

For a fully detailed description of Schema Wizard's usage and capabilities, please refer to [this page](docs/readme-ext/detailed.md).

## Supported Formats
Schema Wizard is able to parse the following content types:
* CSV (text/csv)
* JSON (application/json)
* CEF (application/cef)
* well-formed XML (application/xml)
* PCAP (application/vnd.tcpdump.pcap)
* PDF (application/pdf)
* ZIP (application/zip)

If Schema Wizard finds a content type that could contain other content types (e.g. a zip of CSV's or a PDF containing XML), it will recursively extract embedded content until it finds numeric or string fields.  It will extract any type of object or list that it finds until it can portray it as one of these data types.

Binary fields are not currently supported.  See [Known Issues](docs/readme-ext/known-issues.md).

## Supported Browsers
![Chrome](/docs/readme-ext/chrome-icon.jpg "Chrome 51") ![Firefox](/docs/readme-ext/firefox-icon.jpg "Firefox 47") ![Opera](/docs/readme-ext/opera-icon.jpg "Opera 38") ![Internet Explorer](/docs/readme-ext/ie-icon.jpg "IE 11+") ![Safari](/docs/readme-ext/safari-icon.jpg "Safari 9.1")


## Security Considerations
### Interpretation Engine
The Interpretation Engine executes arbitrary Python code provided by users of Schema Wizard.  There are constraints to prevent this code from affecting the rest of the application, but it should not yet be considered secure.  This feature has not been tested by security professionals.  For this reason Schema Wizard should not be exposed to anything other than trusted connections.

### Unencrypted network traffic
Schema Wizard is not configured to use SSL.  Sensitive material should not be processed in open networks.

## Contribute
Schema Wizard is happy to be a part of the open source community.  See [Contribute](docs/readme-ext/contribute.md) to help improve Schema Wizard.

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

## References
### Detailed Explanation

The [Detailed Explanation](docs/readme-ext/detailed.md) page offers a more thorough explanation of Schema Wizard.

### Known Issues

For a list of known issues, please visit our [Known Issues Page](docs/readme-ext/known-issues.md).

### Develop Documentation

To help develop Schema Wizard, start at the [Contribute](docs/readme-ext/contribute.md) page.

## Credit
* Credit for Geocoding dataset goes to [ThetmaticMapping][geodata].
* Credit for conversion of [ThetmaticMapping][geodata] goes to [Ogre Web Client][ogre]

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
   
   [geodata]: <http://thematicmapping.org>
   [ogre]: <http://ogre.adc4gis.com/>
    