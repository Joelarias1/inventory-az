# Dockerfile para Azure Functions Java
FROM mcr.microsoft.com/azure-functions/java:4-java11

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

# Copiar el proyecto compilado
COPY target/azure-functions/azure-functions-1756416882840/ ${AzureWebJobsScriptRoot}/

# Exponer el puerto 80 para Azure Functions runtime
EXPOSE 80