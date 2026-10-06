# Stage 1 -- build the WAR from source.
#
# The build runs inside the image so that the Docker build does not depend on
# whatever happens to be in target/ on the machine doing the build. Unit tests
# run here too (the default surefire config excludes the Selenium suite, which
# needs a browser and a running application, so it cannot run in this stage).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package

# Stage 2 -- run the WAR on Tomcat.
#
# Tomcat 11 matches the version the application is deployed to on the host, and
# JRE 21 matches maven.compiler.release in pom.xml. curl is installed only so
# the HEALTHCHECK below can ask the container how it is doing.
FROM tomcat:11-jre21-temurin
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Deployed under a named context rather than ROOT so the container URL is
# identical in shape to the host one:
#   http://localhost:8082/food-distribution-tracker/
COPY --from=build /build/target/food-distribution-tracker.war \
     /usr/local/tomcat/webapps/food-distribution-tracker.war

EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
    CMD curl --fail --silent http://localhost:8080/food-distribution-tracker/ > /dev/null || exit 1
