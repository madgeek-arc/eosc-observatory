FROM openjdk:17
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} observatory.jar
ENTRYPOINT ["java","-jar","/observatory.jar"]
