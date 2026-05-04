FROM maven:3.9.6-eclipse-temurin-8 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM maven:3.9.6-eclipse-temurin-8
WORKDIR /opt/app
COPY --from=build /app/target/MessageManagerMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar MessageManagerMicroservice.jar

EXPOSE 1500
ENTRYPOINT ["java", "-jar", "MessageManagerMicroservice.jar"]
