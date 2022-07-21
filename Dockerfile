
FROM maven:3.8.6-jdk-11 AS BUILD
RUN mkdir /build

COPY src /build/src
COPY pom.xml /build
#COPY mvnw /build
#COPY .mvn/ /build/

#RUN ls -l /build/

WORKDIR /build

RUN mvn package -DskipTests

FROM openjdk:buster

RUN mkdir /app
WORKDIR /app
COPY --from=BUILD /build/target/bank_fargate-0.0.1-SNAPSHOT.jar /app/app.jar

ENTRYPOINT [ "java", "-jar", "app.jar" ]
