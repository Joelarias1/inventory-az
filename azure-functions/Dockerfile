# Dockerfile para Azure Functions Java
FROM mcr.microsoft.com/azure-functions/java:4-java11

# Instalar Oracle JDBC driver
ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true

# Copiar el proyecto compilado
COPY target/azure-functions/ ${AzureWebJobsScriptRoot}/

# Crear directorio para Oracle wallet
RUN mkdir -p /opt/oracle/wallet

# Exponer el puerto 80 para Azure Functions runtime
EXPOSE 80