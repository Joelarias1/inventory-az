# Dockerfile para Azure Functions Java
FROM mcr.microsoft.com/azure-functions/java:4-java17

# Configuración de Azure Functions
ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true

# Instalar herramientas de red para debugging
USER root
RUN apt-get update && apt-get install -y \
    dnsutils \
    iputils-ping \
    telnet \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copiar el proyecto compilado desde la ruta de compilación estandarizada
COPY target/azure-functions/azure-functions/ ${AzureWebJobsScriptRoot}/

# Exponer el puerto 80 para Azure Functions runtime
EXPOSE 80