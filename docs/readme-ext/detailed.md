[Back to Main](https://github.com/deleidos/de-schema-wizard/#schema-wizard)

# Details
The following page will go into more detail with the key concepts of Schema Wizard.  While using the software, always look for the ![Bus](/docs/readme-ext/blue-tour-bus.jpg "Tour Bus") for guidance.  Schema Wizard also provides context sensitive help with the ![Help](/docs/readme-ext/blueQuestionMark_whiteCalloutBg.jpg "Help Button") button.

## Data Samples
The starting point for using Schema Wizard is uploading data samples.  Schema Wizard will use these samples to create your schema, so clean samples with a sweeping distribution of values will lead yield the best possible schema.  Though metrics that represent the samples are persisted after their analysis, the original files will be discarded. 

### Detection
When Schema Wizard is given an arbitrary file, the first step is detecting its content type.  Schema Wizard uses [Apache Tika][tika] to detect file content type.  The detection stage loops through every detector and gives it a chance to identify the proposed sample.  Once a sample has been detected as a supported type, it will move on to the parsing stage.

### Parsing
Schema Wizard also uses Tika to parse files.  Note that not all formats supported in Tika are supported in Schema Wizard.  However, Schema Wizard will use Tika to extract any embedded content that it can find.  Assuming all of your content is supported, this diagram shows the detection and parsing process:

![File Detection and Parsing](file_processing_flowchart.JPG "File Detection and Parsing")

For illustrative purposes, consider a zip of PDF files that contain XML.  Schema Wizard will first detect and parse the zip file, extracting all of the PDF files.  Then, it will iterate through the extracted files.  For each PDF, it uses Tika to extract the embedded text, and then it parses this content as if it were a plain text XML file.

### Main Types
When parsing has been completed, all values will be classified as a **number** or **string** field.  This is considered a main type.

### Detail Types
Each main type has associated detail types:
* Number - **integer**, **decimal**, **exponent**
* String - **boolean**, **term** (no spaces), **phrase** (contains spaces)

## Interpretation Engine
The Interpretation Engine brings more meaning to your data.  Using a pluggable framework, it inspects field and identifies significant information elements in data sets.  This adds value to the fields in your samples and schemas.  The following section will explain the details of the Interpretation Engine and what you need to know to use it.

### Domains

The Interpretation Engine maintains a set of domains available to Schema Wizard.  A domain is a broad subject of interest.  Domains contain a variety of terms and definitions that are easily identifiable to those familiar with the subject.  These terms and definitions contain domain specific value, but they may also overlap with other domains.  For example, consider the following topic of interest: Transportation.  This domain is made up of entities such as “latitude,” “longitude,” “altitude,” or “heading.”  Now compare Transportation to the Military domain.  These domains each have their own terminology.  There is some overlap between their jargons, as with “latitude” and “longitude,” but each contains disjoint elements, such as a “target” field in the Military domain.  The Interpretation Engine attempts to identify these elements in a data sample using a given domain.  As the Interpretation Engine processes fields, it offers a best effort interpretation of what the field may represent.  In the Interpretation Engine, an interpretation is defined by a set of rules that identify a particular component of a domain.

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
* Average: **currently unused**
* RegEx: a regex that should match all possible values (converted to strings) of this interpretation

String
* Number Distinct Values: the maximum number of distinct strings
* Minimum Length: the minimum possible length of the string value
* Maximum Length: the maximum possible length of the string value
* RegEx: a regex that should match all possible values of this interpretation

Binary **in progress - currently not supported**
* Length: the length (in bytes) of the binary object
* Mime Type: the mime type of the binary object
* Hash: the matching MD5 hash of the binary object
* Entropy: the entropy of the byte frequency histogram produced by the object

**For each type, disregard the "Quantized," "Ordinal," "Categorical," and "Relational" fields.**

Note that an interpretation’s main type and detail type act as constraints as well.  The main type and detail type of a field must match that of the proposed interpretation.

#### Phase 2 – Script Validation

The second phase of the interpretation process allows for more involved user control of interpretations.  Using the popular scripting language Python, a data scientist may write a script that the Interpretation Engine will use to interpret data.  The script has access to the metrics generated by the field in addition to a list of example values.  It returns a Boolean value: “True” if the interpretation is the appropriate interpretation for the field, “False” if it is not.  The following section requires a basic knowledge of the Python programming language.
A user performs her script editing in the “Validation Script” panel.  

The user must implement the “validateInterpretation(field_profile)” function, which is the entry point into the application code.  The signature for this function is automatically generated with a new interpretation, and it is invoked automatically by the application.

In the validation script, the “field_profile” is passed to validateInterpretation() function.  This dictionary contains all the information from Schema Wizard that a user may analyze.  Each attribute that is generated may be looked up by its appropriate keyword.  If an attribute does not apply to the field, it will not be present in the “field_profile” dictionary.

|Keyword|Present for type|Value Type|Value Description|
|:-------:|:-------------:|:--------:|:---------------:|
|main_type|All|String|“number”, “string”|
|detail_type|All|String|“integer”, “decimal”, “exponent”, “boolean”, “term”, “phrase”|
|example_values|All|List|A list of some unique example values found in the data sample.  If the sample has more than 100 distinct values, Schema Wizard will attempt to populate this list with an even distribution of values across the sample.|
|num_distinct_values|All|Integer|Number of distinct values found for the number|
|number_min|Number|Integer or Float|The minimum numerical value|
|number_max|Number|Integer or Float|The maximum numerical value|
|number_average|Number|Integer or Float|The average of all numerical values in the sample|
|number_std_dev|Number|Float|The standard deviation of the values|
|string_min_len|String|Integer|The minimum length of all the string values|
|string_max_len|String|Integer|The maximum length of all the string values|
|string_average_len|String|Float|The standard deviation of the length of the string values|
|string_std_dev_len|String|Float|The average of the length of the string values|

#### Phase 3 – Name Matching Validation
The final effort to interpret a data field is the name matching phase.  This phase compares the extracted field name to a list of possible names that the interpretation may represent.  For example, the “latitude” interpretation will appear in some data samples as “lat.”  If a data scientist becomes aware of a significantly different naming possibility for a field, he may add the name to the list of matching names in the interpretation.

Though an exact match will benefit the Interpretation Engine’s confidence in assigning interpretations, it is not required.  The name matching phase uses the Jaro-Winkler string matching algorithm.  This algorithm generates a “distance” between two strings, which ultimately represents (on a scale of 0-1) how similar the strings are.  The Interpretation Engine performs this function on all possible names, and it returns an ordered list of matches that exceed 70% confidence.  With this plugability, the user should be able to define custom interpretations in her data sets.

## Schema
A schema is the final output of a Schema Wizard workflow.  Schemas are very similar to samples at a glance.  However, there are a few differences that make a schema more valuable.  First of all, a schema can contain merged fields.  This means that the statistics shown in a schema represent values across multiple data samples.  File formats do not matter to a schema, so the metrics could have come from any variety of samples.  Note that sample metadata is still preserved in the schema object.  This field merge is stored as part of the schema object.  A schema field contains a list of 'alias names.'  These names are references to any sample fields that were merged together.  Unless a field is manually created, it will always have at least the name of sample from which it was derived.

In addition to producing a schema, a user may also choose to modify an existing schema.  This uses a new sample to improve, change, or add to an existing schema.  Modifying an existing schema does impose certain complications with metrics.  The number of distinct values may not be calcuable, and histogram representations of the data will appear skewed if existing values are drastically different than new values.  However, modifying an existing schema allows a user to enhanced their data model as new data sources are introduced.


[//]: # (Links)

   [tika]: <https://tika.apache.org/>
