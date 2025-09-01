# Single-stage Docker build for Enterprise RAG Core Service
# Uses pre-built JAR from local Maven build

FROM eclipse-temurin:21-jre

# Install wget for health checks
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r rag && useradd -r -g rag -s /bin/false rag

# Set working directory
WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs && chown -R rag:rag /app

# Copy the pre-built JAR file
COPY rag-core-service/target/rag-core-service-*.jar app.jar

# Change ownership to non-root user
RUN chown rag:rag app.jar

# Switch to non-root user
USER rag

# Expose the service port
EXPOSE 8084

# Configure JVM for container environment (more memory for LLM processing)
ENV JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]