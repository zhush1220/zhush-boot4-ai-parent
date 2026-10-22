FROM eclipse-temurin:21-jre

MAINTAINER zhush(andycabbage@163.com)
RUN mkdir -p /zhush-server
WORKDIR /zhush-server

ENV TZ=Asia/Shanghai

# 时区
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo "$TZ" > /etc/timezone

# 路线甲-1（zhush.preview.office-converter=libreoffice）才需要，默认不装以保持镜像体积。
# 启用：compose 里把 WITH_LIBREOFFICE 改为 true 重新构建，再设 PREVIEW_OFFICE_CONVERTER=libreoffice。
# 只装 writer/calc/impress 三个组件；中文字体必装，缺字体时转出的 PDF 中文成方块。
ARG WITH_LIBREOFFICE=false
RUN if [ "$WITH_LIBREOFFICE" = "true" ]; then \
        apt-get update \
        && apt-get install -y --no-install-recommends \
            libreoffice-writer libreoffice-calc libreoffice-impress fonts-noto-cjk \
        && rm -rf /var/lib/apt/lists/*; \
    fi

COPY ./zhush-server.jar zhush-server.jar

EXPOSE 9166

ENTRYPOINT ["sh", "-c", "java -Xms256m -Xmx512m -Xss256k -XX:SurvivorRatio=8 -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=docker -Dfile.encoding=utf-8 -Duser.timezone=Asia/Shanghai -jar /zhush-server/zhush-server.jar"]
