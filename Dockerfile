# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --create-home srirammart && mkdir -p /app/data /app/uploads && chown -R srirammart /app
COPY --from=build /src/target/srirammart-1.0.0.jar /app/app.jar
USER srirammart
ENV UPLOAD_DIR=/app/uploads
ENV PORT=8080
EXPOSE 8080
VOLUME ["/app/data", "/app/uploads"]
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-XX:+UseSerialGC", "-Xss512k", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
