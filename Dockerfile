FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml mvnw* ./
COPY .mvn .mvn
RUN ./mvnw -q -B dependency:go-offline || true
COPY src ./src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]