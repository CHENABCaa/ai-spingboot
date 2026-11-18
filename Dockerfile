# 后端 Dockerfile（Railway / Zeabur / Render / 任意支持 Docker 的平台通用）
# 多阶段构建：先编译，再打包成精简运行镜像

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 1236
ENTRYPOINT ["java", "-jar", "app.jar"]
