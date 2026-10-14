FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY core/pom.xml core/pom.xml
COPY ingest/pom.xml ingest/pom.xml
COPY api/pom.xml api/pom.xml
COPY core/src core/src
COPY ingest/src ingest/src
COPY api/src api/src
RUN mvn -B -ntp -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
WORKDIR /app
COPY --from=build --chown=app:app /build/api/target/crypto-intel.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
