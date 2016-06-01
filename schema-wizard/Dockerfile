FROM java:8-jdk

# In case someone loses the Dockerfile
RUN rm -rf /etc/Dockerfile
ADD Dockerfile /etc/Dockerfile

# Install packages
RUN apt-get update && \ 
    apt-get install -y wget

# Download and install jetty
ENV JETTY_VERSION 9.3.8
ENV RELEASE_DATE v20160314
RUN wget http://download.eclipse.org/jetty/${JETTY_VERSION}.${RELEASE_DATE}/dist/jetty-distribution-${JETTY_VERSION}.${RELEASE_DATE}.tar.gz && \
    tar -xzvf jetty-distribution-${JETTY_VERSION}.${RELEASE_DATE}.tar.gz && \
    rm -rf jetty-distribution-${JETTY_VERSION}.${RELEASE_DATE}.tar.gz && \
    mv jetty-distribution-${JETTY_VERSION}.${RELEASE_DATE}/ /opt/jetty

# Configure Jetty user and clean up install
RUN useradd jetty && \
    chown -R jetty:jetty /opt/jetty && \
    rm -rf /opt/jetty/webapps.demo

# Set defaults for docker run
WORKDIR /opt/jetty
CMD ["java", "-jar", "start.jar", "jetty.home=/opt/jetty"]


WORKDIR /opt/jetty
EXPOSE 8080
EXPOSE 80
COPY schwiz.war /opt/jetty/webapps
RUN ["mkdir", "/uploads"]
CMD ["java", "-jar", "start.jar"]