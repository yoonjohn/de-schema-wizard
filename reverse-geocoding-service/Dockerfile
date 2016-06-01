FROM mongo
EXPOSE 27017
ADD country_polygon_data.json /usr/local/country_polygon_data.json
ADD start_and_import.sh /usr/local/start_and_import.sh
RUN chmod +x /usr/local/start_and_import.sh
WORKDIR /usr/local/
CMD ["/usr/local/start_and_import.sh"]