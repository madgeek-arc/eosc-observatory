FROM openjdk:8-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} observatory.jar
ENTRYPOINT ["java","-jar","/observatory.jar"]
