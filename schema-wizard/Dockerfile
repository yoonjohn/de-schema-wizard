FROM jetty:9.3.8

RUN apt-get update
RUN apt-get install -y tcpdump

# Copy over files
ADD schwiz.war /var/lib/jetty/webapps
ADD libjnetpcap.so /usr/lib/
#ADD libjnetpcap.so /var/lib/jetty/resources/

# Update symbolic dynamic links
RUN ["ldconfig"]

# Creating necessary dirs
RUN ["mkdir", "/uploads"]
RUN ["mkdir", "/logs"]
RUN ["touch", "/logs/sw.log"]

# Giving Jetty access permissions
RUN chown -R jetty:jetty /uploads /logs /var/lib/jetty

# Run Jetty
RUN java -jar "$JETTY_HOME/start.jar" --add-to-startd=jmx,stats --module=resources --list-classpath