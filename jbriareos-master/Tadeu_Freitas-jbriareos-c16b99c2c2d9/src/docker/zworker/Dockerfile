FROM openjdk:10

WORKDIR /app

ADD ./src /app/src
ADD ./.clustercert /app/.clustercert
ADD ./.servercert /app/.servercert
ADD ./out/artifacts/ZWorker_jar/Briareos_Java.jar /app/out/artifacts/ZWorker_jar/Briareos_Java.jar

CMD ["java",  "-jar",  "./out/artifacts/ZWorker_jar/Briareos_Java.jar"]
