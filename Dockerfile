# 基于 OpenJDK 11 的基础镜像
FROM openjdk:11.0-jre-buster
LABEL maintainer="研究院研发组 <research-maint@itcast.cn>"

# 设置 JVM 参数和时区
ENV JAVA_OPTS=""
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 安装 Maven
RUN apt-get update && apt-get install -y maven

# 设置工作目录
WORKDIR /app

# 复制项目的 JAR 文件
ADD app.jar /app/app.jar

# 复制 Maven 的 settings.xml 文件，用于访问私有 Nexus 仓库
COPY /usr/local/src/maven/settings.xml /root/.m2/settings.xml

# 使用 Maven 构建项目，并从私有仓库中获取依赖
RUN mvn clean install -s /root/.m2/settings.xml

# 启动应用
ENTRYPOINT ["sh", "-c", "java -jar $JAVA_OPTS /app/app.jar"]