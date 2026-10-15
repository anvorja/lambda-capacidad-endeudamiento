# syntax=docker/dockerfile:1.7
# Compila la Lambda y deja el jar de despliegue en /artifact. No es una imagen para ejecutar: el compose usa la etapa
# "artifact" para copiar el jar a un volumen y desplegarlo en LocalStack (en AWS se sube el mismo jar a Lambda).

FROM eclipse-temurin:21.0.12_8-jdk AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon --console=plain shadowJar -x test

FROM busybox:1.37 AS artifact
COPY --from=build /workspace/build/libs/lambda-capacidad-endeudamiento.jar /artifact/lambda-capacidad-endeudamiento.jar
CMD ["cp", "/artifact/lambda-capacidad-endeudamiento.jar", "/out/lambda-capacidad-endeudamiento.jar"]
