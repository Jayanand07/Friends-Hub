# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Run Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/target/social-media-backend-0.0.1-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-Xms128m", "-Xmx224m", "-XX:MetaspaceSize=96m", "-XX:MaxMetaspaceSize=144m", "-XX:ReservedCodeCacheSize=32m", "-Xss256k", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
