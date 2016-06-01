FROM java
EXPOSE 9123
COPY h2-database-0.0.1-SNAPSHOT-jar-with-dependencies.jar /usr/local/h2/
WORKDIR /usr/local/h2/
CMD ["java", "-classpath", "h2-database-0.0.1-SNAPSHOT-jar-with-dependencies.jar", "com.deleidos.hd.h2.H2Database", "-tcpAllowOthers", "-tcpPort", "9123" ]
