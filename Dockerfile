FROM openjdk:11.0.4-jre-slim

RUN mkdir /app

WORKDIR /app

ADD ./api/target/images-api-1.0.0-SNAPSHOT.jar /app
#ADD ./api/target /app

EXPOSE 8081

CMD ["java", "-jar", "images-api-1.0.0-SNAPSHOT.jar"]
#CMD ["java", "-server", "-cp", "classes:dependency/*", "com.kumuluz.ee.EeApplication"]