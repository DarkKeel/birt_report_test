FROM eclipse-temurin:17-jdk-alpine
ADD target/spring-boot-report.jar spring-boot-report.jar
ADD reports /reports
ENTRYPOINT ["java","-jar","/spring-boot-report.jar"]