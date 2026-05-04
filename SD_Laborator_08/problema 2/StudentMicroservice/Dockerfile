FROM maven:3.9.6-eclipse-temurin-8 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY questions_database.txt ./questions_database.txt
RUN mvn -q -DskipTests package

FROM maven:3.9.6-eclipse-temurin-8
WORKDIR /opt/app
COPY --from=build /app/target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar StudentMicroservice.jar
COPY --from=build /app/questions_database.txt ./questions_database.txt

EXPOSE 1700
ENTRYPOINT ["java", "-jar", "StudentMicroservice.jar"]
