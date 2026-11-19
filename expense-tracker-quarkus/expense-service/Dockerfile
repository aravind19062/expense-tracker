# ---- build stage ---------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests are run outside the image build (they need Docker for Dev Services): mvn test
RUN mvn -B -q package -DskipTests

# ---- runtime stage -------------------------------------------------------------
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 1001 --no-create-home quarkus
WORKDIR /deployments
COPY --from=build --chown=1001 /build/target/quarkus-app/lib/ ./lib/
COPY --from=build --chown=1001 /build/target/quarkus-app/*.jar ./
COPY --from=build --chown=1001 /build/target/quarkus-app/app/ ./app/
COPY --from=build --chown=1001 /build/target/quarkus-app/quarkus/ ./quarkus/
USER 1001
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /deployments/quarkus-run.jar"]
