# Usamos una imagen ligera de Java 17
FROM eclipse-temurin:17-jdk-alpine

# Copiamos los archivos de tu proyecto al contenedor
COPY . .

# Damos permisos de ejecución al instalador de Maven
RUN chmod +x mvnw

# Compilamos el proyecto saltando los tests para ir más rápido
RUN ./mvnw clean package -DskipTests

# Le decimos a Render que usaremos el puerto 8080
EXPOSE 8080

# Comando para iniciar la app con perfil de PRODUCCIÓN
ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=prod -jar target/*.jar"]
