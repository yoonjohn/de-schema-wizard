INSERT INTO schema_model VALUES (NULL, 'sad89fuy98a5f-12a3-1231-124sdf31d21f', 'Flight Data', '1.0-SNAPSHOT', CURRENT_TIMESTAMP ,'This is a test insertion');
INSERT INTO schema_model VALUES (NULL, 'fdg45s65dfg498dfs4gdsf41g65sdfg54dne', 'Aight Data', '1.0-SNAPSHOT', CURRENT_TIMESTAMP ,'This is a test insertion');
INSERT INTO schema_model VALUES (NULL, 'abfdklgmdklsfngmkldsfjngkdsfngklsdfe', 'Zike Data', '1.0-SNAPSHOT', CURRENT_TIMESTAMP ,'This is a test insertion');
INSERT INTO data_sample VALUES (NULL, 'fdeb76c6-472a-4c6c-8301-e9cfd63e30fa', '/TeamsHalf', '/TeamsHalf.csv', 'text/csv', NULL, NULL, NULL, NULL); 
INSERT INTO data_sample VALUES (NULL, 'ghsdkgfdsg-sdfgh-sfdgers-dfghw3e4gfs', '/TeamsHalf', '/TeamsHalf.csv', 'text/json', NULL, NULL, NULL, NULL); 
INSERT INTO data_sample VALUES (NULL, 't456dfsgohjk4-h4j6o54jk5-541654h6-54', '/TeamsHalf', '/TeamsHalf.csv', 'text/txt', NULL, NULL, NULL, NULL); 

INSERT INTO schema_data_samples_mapping(schema_model_id, data_sample_id) VALUES (1, 1);
INSERT INTO schema_data_samples_mapping(schema_model_id, data_sample_id) VALUES (2, 2);
INSERT INTO schema_data_samples_mapping(schema_model_id, data_sample_id) VALUES (3, 3);

INSERT INTO guid_list(guid) VALUES ('sad89fuy98a5f-12a3-1231-124sdf31d21f');
INSERT INTO guid_list(guid) VALUES ('fdg45s65dfg498dfs4gdsf41g65sdfg54dne');
INSERT INTO guid_list(guid) VALUES ('abfdklgmdklsfngmkldsfjngkdsfngklsdfe');
INSERT INTO guid_list(guid) VALUES ('fdeb76c6-472a-4c6c-8301-e9cfd63e30fa');
INSERT INTO guid_list(guid) VALUES ('ghsdkgfdsg-sdfgh-sfdgers-dfghw3e4gfs');
INSERT INTO guid_list(guid) VALUES ('t456dfsgohjk4-h4j6o54jk5-541654h6-54');

INSERT INTO histogram(histogram_id) VALUES (NULL),(NULL), (NULL) ,(NULL), (NULL) ,(NULL), (NULL) ,(NULL), (NULL) ,(NULL), (NULL) ,(NULL), (NULL) ,(NULL), (NULL) ,(NULL), (NULL);

INSERT INTO interpretation(interpretation_id, i_name) VALUES (NULL, 'location');
INSERT INTO interpretation(interpretation_id, i_name) VALUES (NULL, 'sea travel');
INSERT INTO interpretation(interpretation_id, i_name) VALUES (NULL, 'ground travel');
INSERT INTO interpretation(interpretation_id, i_name) VALUES (NULL, 'air travel');

INSERT INTO detail_type(detail_type_id, dt_name) VALUES (NULL, 'INTEGER');
INSERT INTO detail_type(detail_type_id, dt_name) VALUES (NULL, 'DECIMAL');
INSERT INTO detail_type(detail_type_id, dt_name) VALUES (NULL, 'EXPONENT');
INSERT INTO detail_type(detail_type_id, dt_name) VALUES (NULL, 'DATE_TIME');
INSERT INTO detail_type(detail_type_id, dt_name) VALUES (NULL, 'BOOLEAN');
INSERT INTO detail_type(detail_type_id, dt_name) VALUES (NULL, 'TERM');
INSERT INTO detail_type(detail_type_id, dt_name) VALUES (NULL, 'PHRASE');
INSERT INTO detail_type(detail_type_id, dt_name) VALUES (NULL, 'IMAGE');
INSERT INTO detail_type(detail_type_id, dt_name) VALUES (NULL, 'VIDEO_FRAME');
INSERT INTO detail_type(detail_type_id, dt_name) VALUES (NULL, 'AUDIO_SEGMENT');

INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 1, 0, '1', '26'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 2, 0, 'N', '52'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 3, 0, 'N', '52'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 4, 0, 'N', '52'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 5, 0, 'AL', '28') , (NULL, 5, 1, 'NL', '24'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 6, 0, 'A', '28') , (NULL, 6, 1, 'L', '52') , (NULL, 6, 2, 'N', '24'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 7, 0, 'ATL', '2') , (NULL, 7, 1, 'BAL', '2') , (NULL, 7, 2, 'BOS', '2') , (NULL, 7, 3, 'CAL', '2') , (NULL, 7, 4, 'CHA', '2') , (NULL, 7, 5, 'CHN', '2') , (NULL, 7, 6, 'CIN', '2') , (NULL, 7, 7, 'CLE', '2') , (NULL, 7, 8, 'DET', '2') , (NULL, 7, 9, 'HOU', '2') , (NULL, 7, 10, 'KCA', '2') , (NULL, 7, 11, 'LAN', '2') , (NULL, 7, 12, 'MIN', '2') , (NULL, 7, 13, 'ML4', '2') , (NULL, 7, 14, 'MON', '2') , (NULL, 7, 15, 'NYA', '2') , (NULL, 7, 16, 'NYN', '2') , (NULL, 7, 17, 'OAK', '2') , (NULL, 7, 18, 'PHI', '2') , (NULL, 7, 19, 'PIT', '2') , (NULL, 7, 20, 'SDN', '2') , (NULL, 7, 21, 'SEA', '2') , (NULL, 7, 22, 'SFN', '2') , (NULL, 7, 23, 'SLN', '2') , (NULL, 7, 24, 'TEX', '2') , (NULL, 7, 25, 'TOR', '2'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 8, 0, '4', '2') , (NULL, 8, 1, 'A', '18') , (NULL, 8, 2, 'B', '4') , (NULL, 8, 3, 'C', '12') , (NULL, 8, 4, 'D', '4') , (NULL, 8, 5, 'E', '8') , (NULL, 8, 6, 'F', '2') , (NULL, 8, 7, 'H', '8') , (NULL, 8, 8, 'I', '8') , (NULL, 8, 9, 'K', '4') , (NULL, 8, 10, 'L', '14') , (NULL, 8, 11, 'M', '6') , (NULL, 8, 12, 'N', '22') , (NULL, 8, 13, 'O', '10') , (NULL, 8, 14, 'P', '4') , (NULL, 8, 15, 'R', '2') , (NULL, 8, 16, 'S', '10') , (NULL, 8, 17, 'T', '10') , (NULL, 8, 18, 'U', '2') , (NULL, 8, 19, 'X', '2') , (NULL, 8, 20, 'Y', '4'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 9, 0, '48', '2') , (NULL, 9, 1, '49', '1') , (NULL, 9, 2, '50', '5') , (NULL, 9, 3, '51', '4') , (NULL, 9, 4, '52', '10') , (NULL, 9, 5, '53', '9') , (NULL, 9, 6, '54', '4') , (NULL, 9, 7, '55', '3') , (NULL, 9, 8, '56', '6') , (NULL, 9, 9, '57', '4') , (NULL, 9, 10, '58', '1') , (NULL, 9, 11, '59', '1') , (NULL, 9, 12, '60', '2'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 10, 0, '15', '1') , (NULL, 10, 1, '16', '1') , (NULL, 10, 2, '17', '2') , (NULL, 10, 3, '18', '1') , (NULL, 10, 4, '20', '2') , (NULL, 10, 5, '21', '3') , (NULL, 10, 6, '23', '4') , (NULL, 10, 7, '24', '3') , (NULL, 10, 8, '25', '5') , (NULL, 10, 9, '26', '2') , (NULL, 10, 10, '27', '3') , (NULL, 10, 11, '28', '2') , (NULL, 10, 12, '29', '4') , (NULL, 10, 13, '30', '5') , (NULL, 10, 14, '31', '7') , (NULL, 10, 15, '33', '2') , (NULL, 10, 16, '34', '2') , (NULL, 10, 17, '35', '1') , (NULL, 10, 18, '36', '1') , (NULL, 10, 19, '37', '1'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 11, 0, '1', '8') , (NULL, 11, 1, '2', '9') , (NULL, 11, 2, '3', '7') , (NULL, 11, 3, '4', '8') , (NULL, 11, 4, '5', '9') , (NULL, 11, 5, '6', '9') , (NULL, 11, 6, '7', '2'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 12, 0, '20', '2') , (NULL, 12, 1, '21', '4') , (NULL, 12, 2, '22', '5') , (NULL, 12, 3, '23', '10') , (NULL, 12, 4, '24', '1') , (NULL, 12, 5, '25', '2') , (NULL, 12, 6, '26', '5') , (NULL, 12, 7, '27', '4') , (NULL, 12, 8, '28', '2') , (NULL, 12, 9, '29', '5') , (NULL, 12, 10, '30', '3') , (NULL, 12, 11, '32', '1') , (NULL, 12, 12, '33', '2') , (NULL, 12, 13, '34', '1') , (NULL, 12, 14, '36', '2') , (NULL, 12, 15, '37', '1') , (NULL, 12, 16, '39', '1') , (NULL, 12, 17, '42', '1'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 13, 0, 'E', '26') , (NULL, 13, 1, 'W', '26'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 14, 0, 'E', '26') , (NULL, 14, 1, 'W', '26'); 
INSERT INTO bucket (bucket_id, histogram_id, b_order, b_definition, b_count) VALUES (NULL, 15, 0, '1981', '52'); 

/* Schema String Metrics 
 * INSERT INTO schema_field(schema_field_id, schema_model_id, number_histogram, string_character_histogram, string_term_histogram, binary_character_histogram, detail_type_id, interpretation_id, field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, string_min_length, string_max_length, string_average_length, string_std_dev_length) VALUES (NULL, 1, 2, 1, 1, 1, 1, 1, 'DivWin', '1', 1, '1', '1', '1', 1, 1, 1, 1);
 */
 
/* Schema Number Metrics */
INSERT INTO schema_field(schema_field_id, schema_model_id, number_histogram, string_character_histogram, string_term_histogram, binary_character_histogram, detail_type_id, interpretation_id, field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, number_min, number_max, number_average, number_std_dev) VALUES (NULL, 1, 1, 1, 1, 1, 1, 1, 'DivWin', '1', 1, '1', '1', '1', '1', '1', '1', 1);

/* Data Sample String Metrics 
 * INSERT INTO data_sample_field(data_sample_field_id, data_sample_id, number_histogram, string_character_histogram, string_term_histogram, binary_character_histogram, detail_type_id, interpretation_id, field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, number_min, number_max, number_average, number_std_dev) VALUES (NULL, 1, 1, 1, 1, 1, 6, 1, 'DivWin', '1', 1, '1', '1', '1', '1', '1', '1', 1);
 */
/* Data Sample Number Metrics */
INSERT INTO data_sample_field(data_sample_field_id, data_sample_id, number_histogram, string_character_histogram, string_term_histogram, binary_character_histogram, detail_type_id, interpretation_id, field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, number_min, number_max, number_average, number_std_dev) VALUES (NULL, 1, 1, 1, 1, 1, 1, 1, 'DivWin', '1', 1, '1', '1', '1', '1', '1', '1', 1);

INSERT INTO schema_alias_mapping VALUES(1, 1);