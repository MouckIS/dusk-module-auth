FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Internal dusk-* parent POMs and libraries (dusk-dependencies BOM,
# dusk-module-parent, dusk-common-core/rpc, dusk-module-ddm-shared) are
# hosted on GitHub Packages, not Maven Central. Provide a PAT with
# read:packages scope:
#   docker build --build-arg GH_PAT=<PAT> -t dusk-module-auth .
# If you already have a configured ~/.m2/settings.xml, mount it instead
# (docker build -v /path/settings.xml:/root/.m2/settings.xml ...).
ARG GH_PAT=""

# Use the project's Maven settings and export GH_PAT so ${env.GH_PAT} in
# settings.xml resolves to the credential.
COPY settings.xml /root/.m2/settings.xml
ENV GH_PAT=${GH_PAT}

COPY . .

# 1) Package the module. Tests are skipped (the only test class is a
#    @SpringBootTest that needs a live DB/Redis/Nacos) and the JaCoCo
#    coverage gate is skipped so packaging never fails on coverage.
# 2) Repackage into the executable Spring Boot fat jar. Plain `mvn package`
#    only produces a thin jar because the spring-boot-maven-plugin is
#    declared in pluginManagement but not bound to the build lifecycle.
RUN mvn -B -ntp -U -DskipTests -Djacoco.skip=true clean package \
    && mvn -B -ntp -U -DskipTests -Djacoco.skip=true \
       org.springframework.boot:spring-boot-maven-plugin:3.2.12:repackage

##############################
# Stage 2: Runtime
##############################
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 1001 --create-home dusk \
    && mkdir -p /app/logs \
    && chown -R dusk:dusk /app

COPY --from=build --chown=dusk:dusk \
     /workspace/target/dusk-module-auth.jar /app/app.jar

USER dusk

ENV SPRING_PROFILES_ACTIVE=sit \
    SERVER_PORT=8080 \
    TZ=Asia/Shanghai

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
