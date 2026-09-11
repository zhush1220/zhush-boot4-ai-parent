FROM eclipse-temurin:21-jre

MAINTAINER zhush(andycabbage@163.com)
RUN mkdir -p /zhush-server
WORKDIR /zhush-server

ENV TZ=Asia/Shanghai

# 时区
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo "$TZ" > /etc/timezone

COPY ./zhush-server.jar zhush-server.jar

EXPOSE 9166

ENTRYPOINT ["sh", "-c", "java -Xms256m -Xmx512m -Xss256k -XX:SurvivorRatio=8 -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=docker -Dfile.encoding=utf-8 -Duser.timezone=Asia/Shanghai -jar /zhush-server/zhush-server.jar"]
