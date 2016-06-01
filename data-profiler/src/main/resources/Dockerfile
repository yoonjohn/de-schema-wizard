FROM java
EXPOSE 9123
EXPOSE 8082
COPY data-profiler-0.0.1-SNAPSHOT-jar-with-dependencies.jar /usr/local/h2/
WORKDIR /usr/local/h2/
CMD ["java", "-classpath", "data-profiler-0.0.1-SNAPSHOT-jar-with-dependencies.jar", "com.deleidos.dp.h2.H2DataAccessObject", "-tcpAllowOthers", "-tcpPort", "9123" ]