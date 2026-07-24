# ---- Build stage ----
FROM rockylinux:9 AS build

RUN dnf install -y java-21-openjdk-devel && dnf clean all

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version --no-daemon

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# ---- Run stage ----
FROM rockylinux:9-minimal AS run

RUN microdnf install -y java-21-openjdk-headless && microdnf clean all

WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
