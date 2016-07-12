[Main](https://github.com/deleidos/de-schema-wizard/)

# Details
The following page will go into more detail with the key concepts of Schema Wizard.  While using the software, always look for the ![Bus](/docs/readme-ext/blue-tour-bus.jpg "Tour Bus") for guidance.  Schema Wizard also provides context sensitive help with the ![Help](/docs/readme-ext/blueQuestionMark_whiteCalloutBg.jpg "Help Button") button.

## Data Samples
The starting point for using Schema Wizard is uploading data samples.  Schema Wizard will use these samples to create your schema, so clean samples with a sweeping distribution of values will lead yield the best possible schema.  Though metrics that represent the samples are persisted after their analysis, the original files will be discarded. 

### Detection
When Schema Wizard is given an arbitrary file, the first step is detecting its content type.  Schema Wizard uses [Apache Tika][tika] to detect file content type.  The detection stage loops through every detector and gives it a chance to identify the proposed sample.  Ideally, Schema Wizard only adds to the detection capabilities of Tika, but it is important to note that a single flawed detector can have negative affects on the entire application.  

### Parsing
Schema Wizard also uses Tika to parse files.  Note that not all formats supported in Tika are supported in Schema Wizard.  However, Schema Wizard will use Tika to extract any embedded content that it can find.  This diagram shows the detection and parsing process:
[File Detection and Parsing](file_parsing_flowchart.JPG)
For example, consider a Microsoft Word file containing JSON data.  Though "Microsoft Word" is not supported (because Schema Wizard cannot parse *ever* MS Word file), Schema Wizard will be able to parse this document.  It uses Tika to extract the embedded text from the document, and then it parses this content as if it were a plain text JSON file.

## Interpretation Engine

### Domains

The Interpretation Engine maintains a set of domains available to Schema Wizard.  A domain is easily defined as a broad subject of interest.  Domains contain a variety of terms and definitions that are easily identifiable to those familiar with the subject.  These terms and definitions contain domain specific value, but they may also overlap with other domains.  For example, consider the following topic of interest: Transportation.  This domain is made up of entities such as “latitude,” “longitude,” “altitude,” or “heading.”  Now compare Transportation to the Military domain.  These domains each have their own terminology.  There is some overlap between their jargons, as with “latitude” and “longitude,” but each contains disjoint elements, such as a “target” field in the Military domain.  The Interpretation Engine attempts to identify these elements in a data sample using a given domain.  As the Interpretation Engine processes fields, it offers a best effort interpretation of what the field may represent.  In the Interpretation Engine, an interpretation is defined by a set of rules that identify a particular component of a domain.

### Interpretations

Schema Wizard provides automated data extraction and statistical analysis.  Working with the Interpretation Engine, Schema Wizard assigns domain specific meaning to the data it extracts.  Once Schema Wizard has generated metrics representing a data field, The Interpretation Engine utilizes a three phase validation process to offer its interpretation of a field.

There are 3 phases to of validation for interpretations.

#### Phase 1 – Constraint Validation

During the first phase of the interpretation process, defined constraints are used to quickly nullify inappropriate interpretations.  These constraints are specific to the interpretation, and they are determined when the interpretation is created.  One example of a constraint is a maximum value.  Consider “heading” in the Transportation domain.  A heading is a value that represents the direction an aircraft is moving.  Using degrees, a heading falls in the range 0-360.  We can therefore say that a heading must have a maximum value of 360.  Likewise, any field with a maximum value greater than 360 will not be considered a heading.
Each main data type has an associated selection of constraints.  The current available constraints are the following:

Number
* Maximum Number Distinct Values: the maximum number of distinct values
* Minimum: the minimum possible numerical value
* Maximum: the maximum possible numerical value

String
* Number Distinct Values: the maximum number of distinct strings
* Minimum Length: the minimum possible length of the string value
* Maximum Length: the maximum possible length of the string value
* Regex: a regex that will match all possible values of this interpretation

Binary **in progress**
* Length: the length (in bytes) of the binary object
* Mime Type: the mime type of the binary object
* Hash: the matching MD5 hash of the binary object
* Entropy: the entropy of the byte frequency histogram produced by the object

Note that an interpretation’s main type and detail type act as constraints as well.  The main type and detail type of a field must match that of the proposed interpretation.

#### Phase 2 – Script Validation

The second phase of the interpretation process allows for more involved user control of interpretations.  Using the popular scripting language Python, a data scientist may write a script that the Interpretation Engine will use to interpret data.  The script has access to the metrics generated by the field in addition to a list of example values.  It returns a Boolean value: “True” if the interpretation is the appropriate interpretation for the field, “False” if it is not.  The following section requires a basic knowledge of the Python programming language.
A user performs her script editing in the “Validation Script” panel.  

The user may notice that she cannot edit the last two lines of Python code.  This code snippet is necessary for the Interpretation Engine to correctly call and execute the script.  It also forces the user to implement the “validateInterpretation(arg)” function, which is the user’s entry point into the application code.  The signature for this function is automatically generated with a new interpretation.

In the validation script, the “field_profile” is passed to validateInterpretation() function.  This dictionary contains all the information from Schema Wizard that a user may analyze.  Each attribute that is generated may be looked up by its appropriate keyword.  If an attribute does not apply to the field, it will not be present in the “field_profile” dictionary.

|Keyword|Present for type|Value Type|Value Description|
|:-------:|:-------------:|:--------:|:---------------:|
|main_type|All|String|“number”, “string”, or “binary”|
|detail_type|All|String|“integer”, “decimal”, “exponent”, “boolean”, “term”, “phrase”, “date/time”, “audio”, “video”, or “image”|
|example_values|All|List|A list of some unique example values found in the data sample.  If the sample has more than 100 distinct values, Schema Wizard will attempt to populate this list with an even distribution of values across the sample.|
|number_num_distinct_values|Number|Integer|Number of distinct values found for the number|
|number_min|Number|Integer;Float|The minimum numerical value|
|number_max|Number|Integer;Float|The maximum numerical value|
|number_average|Number|Integer;Float|The average of all numerical values in the sample|
|number_std_dev|Number|Float|The standard deviation of the values|
|string_num_distinct_values|String|Integer|Number of distinct values found for the string|
|string_min_len|String|Integer|The minimum length of all the string values|
|string_max_len|String|Integer|The maximum length of all the string values|
|string_average_len|String|Float|The standard deviation of the length of the string values|
|string_std_dev_len|String|Float|The average of the length of the string values|
|binary_len|Binary|Integer|The length (in bytes) of the binary value|
|binary_mime_type|Binary|String|The best effort determination of the MIME type of the binary object|
|binary_hash|Binary|String|The MD5 hash generated by the binary object|
|binary_entropy|Binary|Float|The entropy calculated from the byte frequency distribution of the binary object|

#### Phase 3 – Name Matching Validation
The final effort to interpret a data field is the name matching phase.  This phase compares the extracted field name to a list of possible names that the interpretation may represent.  For example, the “latitude” interpretation will appear in some data samples as “lat.”  If a data scientist becomes aware of a significantly different naming possibility for a field, he may add the name to the list of matching names in the interpretation.

Though an exact match will benefit the Interpretation Engine’s confidence in assigning interpretations, it is not required.  The name matching phase uses the Jaro-Winkler string matching algorithm.  This algorithm generates a “distance” between two strings, which ultimately represents (on a scale of 0-1) how similar the strings are.  The Interpretation Engine performs this function on all possible names, and it returns an ordered list of matches that exceed 70% confidence.  It is up to the user to verify the Interpretation Engine’s choice. 


## Schema

[//]: # (Links)

   [tika]: <https://tika.apache.org/>
