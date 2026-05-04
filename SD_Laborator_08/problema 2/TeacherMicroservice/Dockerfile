FROM maven:3.9.6-eclipse-temurin-8 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY teacher_answers_database.txt ./teacher_answers_database.txt
RUN mvn -q -DskipTests package

FROM maven:3.9.6-eclipse-temurin-8
WORKDIR /opt/app
COPY --from=build /app/target/TeacherMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar TeacherMicroservice.jar
COPY --from=build /app/teacher_answers_database.txt ./teacher_answers_database.txt

EXPOSE 1600
ENTRYPOINT ["java", "-jar", "TeacherMicroservice.jar"]
