FROM maven:3.9-eclipse-temurin-21-jammy AS playwright
WORKDIR /tmp/pw

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN mvn -q exec:java \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install --with-deps chromium"

FROM eclipse-temurin:21-jre-jammy
RUN DEBIAN_FRONTEND=noninteractive apt-get update && apt-get install -y \
    curl \
    libglib2.0-0 \
    libnss3 \
    libnspr4 \
    libdbus-1-3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libgio2.0-cil \
    libexpat1 \
    libatspi2.0-0 \
    libx11-6 \
    libxcomposite1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libdrm2 \
    libxcb1 \
    libxkbcommon0 \
    libasound2 \
    libcups2 \
    libpango-1.0-0 \
    libcairo2 \
    && rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} observatory.jar

# Store Playwright browsers in a fixed path inside the image
COPY --from=playwright /ms-playwright /ms-playwright
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

# Prevent runtime downloads
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

ENTRYPOINT ["java","-jar","/observatory.jar"]
