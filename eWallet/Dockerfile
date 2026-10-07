# build source code using maven and jdk 21
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# copy pom.xml and src
COPY pom.xml .
COPY src ./src

# build .jar files
RUN mvn clean package -DskipTests

# runtime environment (ubuntu
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

#copy .jar from builder to current environment
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

# run command
ENTRYPOINT ["java", "-jar", "app.jar"]
