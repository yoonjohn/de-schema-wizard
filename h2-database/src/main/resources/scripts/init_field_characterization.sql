CREATE TABLE IF NOT EXISTS main_type
(
       main_type_id INTEGER PRIMARY KEY AUTO_INCREMENT,
       mt_name varchar(10)
);

CREATE TABLE IF NOT EXISTS detail_type
(
       detail_type_id INTEGER PRIMARY KEY AUTO_INCREMENT,
       dt_name varchar(20)
);

CREATE TABLE IF NOT EXISTS type_mapping
(
       tm_id INTEGER NOT NULL AUTO_INCREMENT,
       main_type_id INTEGER NOT NULL,
       detail_type_id INTEGER NOT NULL,
       FOREIGN KEY (main_type_id) REFERENCES main_type(main_type_id) ON DELETE CASCADE,
       FOREIGN KEY (detail_type_id) REFERENCES detail_type(detail_type_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS interpretation
(
       interpretation_id INTEGER AUTO_INCREMENT PRIMARY KEY,
       i_name varchar(255),
       i_id varchar(36)
);

CREATE TABLE IF NOT EXISTS guid_list
(
       guid varchar(36) NOT NULL UNIQUE,
       
);

CREATE TABLE IF NOT EXISTS data_sample
(
       data_sample_id INTEGER PRIMARY KEY AUTO_INCREMENT,
       ds_guid varchar(36) NOT NULL UNIQUE,
       ds_name varchar(255) NOT NULL,
       ds_file_name varchar(255),
       ds_file_type varchar(255),
       ds_version varchar(255),
       ds_last_update timestamp,
       ds_description varchar(1023),
       ds_extracted_content_dir varchar(255),
       ds_num_records int,
       ds_file_size int
);

CREATE TABLE IF NOT EXISTS schema_model
(
       schema_model_id INTEGER AUTO_INCREMENT PRIMARY KEY,
       s_guid varchar(36) NOT NULL UNIQUE,
       s_name varchar(255),
       s_version varchar(255),
       s_lastUpdate timestamp,
       s_description varchar(1023),
       s_sum_sample_records int,
       s_domain_name varchar(255)
);

CREATE TABLE IF NOT EXISTS schema_data_samples_mapping
(
       schema_model_id INTEGER,
       data_sample_id INTEGER,
       FOREIGN KEY (schema_model_id) REFERENCES schema_model(schema_model_id) ON DELETE CASCADE,
       FOREIGN KEY (data_sample_id) REFERENCES data_sample(data_sample_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS histogram
(
       histogram_id INTEGER AUTO_INCREMENT PRIMARY KEY,
       base_histogram_id_for_region_histograms INTEGER NULL,
       latitude_key_for_region_histograms varchar(255) NULL,
       longitude_key_for_region_histograms varchar(255) NULL,
       FOREIGN KEY (base_histogram_id_for_region_histograms) REFERENCES histogram(histogram_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bucket
(
       bucket_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
       histogram_id INTEGER NOT NULL,
       b_order INTEGER NOT NULL,
       b_short_definition varchar(50),
       b_long_definition varchar(255),
       b_count varchar(255),
       FOREIGN KEY (histogram_id) REFERENCES histogram(histogram_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS schema_field
(
       /* Common Fields */
       schema_field_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
       field_name varchar(255) NOT NULL,
       field_order varchar(255),
       num_distinct varchar(255),
       count varchar(255),
       walking_square_sum varchar(255),
       walking_sum varchar(255),
       presence float,
       display_name varchar(255),
       
       /* Foreign Keys */
       schema_model_id INTEGER NOT NULL,
       number_histogram INTEGER NULL,
       string_character_histogram INTEGER NULL,
       string_term_histogram INTEGER NULL,
       binary_character_histogram INTEGER NULL,
       detail_type_id INTEGER NOT NULL,
       interpretation_id INTEGER,
       
       /* Number Field */
       number_min varchar(255),
       number_max varchar(255),
       number_average varchar(255),
       number_std_dev DOUBLE,
       
       /* String Field */
       string_min_length INTEGER,
       string_max_length INTEGER,
       string_average_length DOUBLE,
       string_std_dev_length DOUBLE,
       
       /* Binary Fields */
       binary_mime_type varchar(20),
       binary_length INTEGER,
       binary_hash varchar(255),
       binary_entropy DOUBLE,
       
       FOREIGN KEY (schema_model_id) REFERENCES schema_model(schema_model_id) ON DELETE CASCADE,
       FOREIGN KEY (number_histogram) REFERENCES histogram(histogram_id) ON DELETE CASCADE,
       FOREIGN KEY (string_character_histogram) REFERENCES histogram(histogram_id) ON DELETE CASCADE,
       FOREIGN KEY (string_term_histogram) REFERENCES histogram(histogram_id) ON DELETE CASCADE,
       FOREIGN KEY (binary_character_histogram) REFERENCES histogram(histogram_id) ON DELETE CASCADE,
       FOREIGN KEY (detail_type_id) REFERENCES detail_type(detail_type_id) ON DELETE CASCADE,
       FOREIGN KEY (interpretation_id) REFERENCES interpretation(interpretation_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS data_sample_field
(
       /* Common Fields */
       data_sample_field_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
       field_name varchar(255) NOT NULL,
       field_order varchar(255),
       num_distinct varchar(255),
       count varchar(255),
       walking_square_sum varchar(255),
       walking_sum varchar(255),
       presence float,
       display_name varchar(255),
       
       /* Foreign Keys */
       data_sample_id INTEGER NOT NULL,
       number_histogram INTEGER NULL,
       string_character_histogram INTEGER NULL,
       string_term_histogram INTEGER NULL,
       binary_character_histogram INTEGER NULL,
       detail_type_id INTEGER NOT NULL,
       interpretation_id INTEGER,
       
       /* Number Field */
       number_min varchar(255),
       number_max varchar(255),
       number_average varchar(255),
       number_std_dev DOUBLE,
       
       /* String Field */
       string_min_length INTEGER,
       string_max_length INTEGER,
       string_average_length DOUBLE,
       string_std_dev_length DOUBLE,
       
       /* Binary Fields */
       binary_mime_type varchar(20),
       binary_length INTEGER,
       binary_hash varchar(255),
       binary_entropy DOUBLE,
       
       FOREIGN KEY (data_sample_id) REFERENCES data_sample(data_sample_id) ON DELETE CASCADE,
       FOREIGN KEY (number_histogram) REFERENCES histogram(histogram_id) ON DELETE CASCADE,
       FOREIGN KEY (string_character_histogram) REFERENCES histogram(histogram_id) ON DELETE CASCADE,
       FOREIGN KEY (string_term_histogram) REFERENCES histogram(histogram_id) ON DELETE CASCADE,
       FOREIGN KEY (binary_character_histogram) REFERENCES histogram(histogram_id) ON DELETE CASCADE,
       FOREIGN KEY (detail_type_id) REFERENCES detail_type(detail_type_id) ON DELETE CASCADE,
       FOREIGN KEY (interpretation_id) REFERENCES interpretation(interpretation_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS schema_alias_mapping
(
	schema_field_id integer,
	data_sample_field_id integer,
	FOREIGN KEY (data_sample_field_id) REFERENCES data_sample_field(data_sample_field_id) ON DELETE CASCADE,
    FOREIGN KEY (schema_field_id) REFERENCES schema_field(schema_field_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS interpretation_field_mapping
(
       interpretation_id integer,
       schema_field_id integer,
       data_sample_field_id integer,
       confidence float,
       FOREIGN KEY (interpretation_id) REFERENCES interpretation(interpretation_id) ON DELETE CASCADE,
       FOREIGN KEY (schema_field_id) REFERENCES schema_field(schema_field_id) ON DELETE CASCADE,
       FOREIGN KEY (data_sample_field_id) REFERENCES data_sample_field(data_sample_field_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS deletion_queue
(
       guid varchar(36) NOT NULL UNIQUE,
       last_update date,
       in_progress boolean
);
