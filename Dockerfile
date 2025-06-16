FROM openjdk:21-slim
RUN apt update && apt install curl -y
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} observatory.jar
ENTRYPOINT ["java","-jar","/observatory.jar"]
