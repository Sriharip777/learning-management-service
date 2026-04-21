# Install shared modules first
cd ~/educonnect-infrastructure-platform
mvn clean install -DskipTests -B -q

# Build service JAR
cd ~/learning-management-service
mvn clean package -DskipTests -B -q

# Use simple Dockerfile that copies prebuilt JAR
cat > Dockerfile << 'EOF'
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring && chown -R spring:spring /app
USER spring:spring
COPY --chown=spring:spring target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
EOF

# Build & Push
docker build --platform linux/amd64 --provenance=false --no-cache \
  -t us-east1-docker.pkg.dev/fineflux/pulse/learning-management-service:main-1.0.0.1 . && \
docker push us-east1-docker.pkg.dev/fineflux/pulse/learning-management-service:main-1.0.0.1