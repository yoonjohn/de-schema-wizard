DROP TABLE IF EXISTS main_type;

CREATE TABLE main_type
(
	main_type_id INTEGER NOT NULL PRIMARY KEY,
	mt_name varchar(10)
);

DROP TABLE IF EXISTS detail_type;

CREATE TABLE detail_type
(
	detail_type_id INTEGER NOT NULL PRIMARY KEY,
	dt_name varchar(20)
);

DROP TABLE IF EXISTS type_mapping;

CREATE TABLE type_mapping
(
	tm_id INTEGER NOT NULL AUTO_INCREMENT,
	main_type_id INTEGER NOT NULL,
	detail_type_id INTEGER NOT NULL,
	FOREIGN KEY (main_type_id) REFERENCES main_type(main_type_id),
	FOREIGN KEY (detail_type_id) REFERENCES detail_type(detail_type_id)
);

DROP TABLE IF EXISTS data_sample;

CREATE TABLE data_sample
(
	data_sample_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	ds_id varchar(36) NOT NULL UNIQUE,
	ds_name varchar(255) NOT NULL UNIQUE,
	ds_file_name varchar(255),
	ds_file_type varchar(255),
	ds_version varchar(255),
	ds_last_update timestamp,
	ds_description varchar(1023)
);

DROP TABLE IF EXISTS field;

CREATE TABLE field
(
	field_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	data_sample_id INTEGER NOT NULL,
	f_name varchar(255) NOT NULL,
	f_order INTEGER NULL,
	FOREIGN KEY (data_sample_id) REFERENCES data_sample(data_sample_id)
);

DROP TABLE IF EXISTS schema_model;

CREATE TABLE schema_model
(
	schema_model_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	s_id varchar(36),
	s_name varchar(255),
	s_version varchar(255),
	s_lastUpdate timestamp,
	s_description varchar(1023)
);

DROP TABLE IF EXISTS schema_data_samples_mapping;

CREATE TABLE schema_data_samples_mapping
(
	schema_model_id INTEGER,
	data_sample_id INTEGER,
	FOREIGN KEY (schema_model_id) REFERENCES schema_model(schema_model_id),
	FOREIGN KEY (data_sample_id) REFERENCES data_sample(data_sample_id)
);

DROP TABLE IF EXISTS schema_field;

CREATE TABLE schema_field
(
	schema_field_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	field_id INTEGER,
	schema_model_id INTEGER,
	f_name varchar(255),
	FOREIGN KEY (field_id) REFERENCES field(field_id)
);

DROP TABLE IF EXISTS guid_list;

CREATE TABLE guid_list
(
	guid varchar(36) NOT NULL UNIQUE
);

DROP TABLE IF EXISTS schema_field_mapping;

CREATE TABLE schema_field_mapping
(
	schema_model_id INTEGER,
	schema_field_id INTEGER,
	FOREIGN KEY (schema_model_id) REFERENCES schema_model(schema_model_id),
	FOREIGN KEY (schema_field_id) REFERENCES schema_field(schema_field_id)
);

DROP TABLE IF EXISTS classification;

CREATE TABLE classification
(
	classification_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	c_name varchar(255)
);

DROP TABLE IF EXISTS bucket;

CREATE TABLE bucket
(
	histogram_id INTEGER NOT NULL,
	bucket_id INTEGER NOT NULL AUTO_INCREMENT,
	b_order INTEGER NOT NULL,
	b_definition varchar(255),
	b_count varchar(255),
	PRIMARY KEY (histogram_id, bucket_id)
);

DROP TABLE IF EXISTS histogram;

CREATE TABLE histogram
(
	histogram_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY
);

DROP TABLE IF EXISTS number_field;

CREATE TABLE number_field
(
	number_field_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	field_id INTEGER NOT NULL,
	histogram_id INTEGER,
	detail_type_id INTEGER,
	classification_id INTEGER,
	min varchar(255),
	max varchar(255),
	average varchar(255),
	std_dev DOUBLE,
	num_distinct INTEGER,
	count varchar(255),
	walking_square_sum varchar(255),
	walking_sum varchar(255),
	FOREIGN KEY (field_id) REFERENCES field(field_id),
	FOREIGN KEY (histogram_id) REFERENCES histogram(histogram_id),
	FOREIGN KEY (detail_type_id) REFERENCES detail_type(detail_type_id),
	FOREIGN KEY (classification_id) REFERENCES classification(classification_id)
);

DROP TABLE IF EXISTS binary_field;

CREATE TABLE binary_field
(
	binary_field_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	field_id INTEGER NOT NULL,
	histogram_id INTEGER,
	detail_type_id INTEGER,
	classification_id INTEGER,
	mime_type varchar(20),
	length INTEGER,
	hash varchar(255),
	entropy DOUBLE,
	count varchar(255),
	walking_square_sum varchar(255),
	walking_sum varchar(255),
	FOREIGN KEY (field_id) REFERENCES field(field_id),
	FOREIGN KEY (histogram_id) REFERENCES histogram(histogram_id),
	FOREIGN KEY (detail_type_id) REFERENCES detail_type(detail_type_id),
	FOREIGN KEY (classification_id) REFERENCES classification(classification_id)
);

DROP TABLE IF EXISTS string_field;

CREATE TABLE string_field
(
	string_field_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	field_id INTEGER NOT NULL,
	histogram_id INTEGER,
	character_histogram_id INTEGER,
	detail_type_id INTEGER,
	classification_id INTEGER,
	min_length INTEGER,
	max_length INTEGER,
	average_length DOUBLE,
	std_dev_length DOUBLE,
	num_distinct INTEGER,
	count varchar(255),
	walking_square_sum varchar(255),
	walking_sum varchar(255),
	FOREIGN KEY (field_id) REFERENCES field(field_id),
	FOREIGN KEY (histogram_id) REFERENCES histogram(histogram_id),
	FOREIGN KEY (character_histogram_id) REFERENCES histogram(histogram_id),
	FOREIGN KEY (detail_type_id) REFERENCES detail_type(detail_type_id),
	FOREIGN KEY (classification_id) REFERENCES classification(classification_id)
);
