package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger - Integración con Oracle Cloud
 */
public class Function {
    /**
     * Esta función escucha en el endpoint "/api/HttpExample". 
     * Dos formas de invocarla usando el comando "curl" en bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request with Oracle Cloud integration.");

        // Parse query parameter
        final String query = request.getQueryParameters().get("name");
        final String name = request.getBody().orElse(query);

        try {
            // Verificar conexión a Oracle Cloud
            String dbStatus = checkDatabaseConnection();
            
            String greeting = (name == null) ? "Mundo" : name;
            
            String response = String.format(
                "{\n" +
                "  \"saludo\": \"¡Hola, %s!\",\n" +
                "  \"timestamp\": \"%s\",\n" +
                "  \"function\": \"Azure Function con Oracle Cloud\",\n" +
                "  \"database_status\": \"%s\",\n" +
                "  \"message\": \"Sistema de Inventario - Integración exitosa\",\n" +
                "  \"endpoints\": {\n" +
                "    \"health\": \"/api/health\",\n" +
                "    \"productos\": \"/api/productos\",\n" +
                "    \"bodegas\": \"/api/bodegas\"\n" +
                "  }\n" +
                "}", 
                greeting,
                java.time.LocalDateTime.now(),
                dbStatus
            );
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(response)
                    .build();
                    
        } catch (Exception e) {
            context.getLogger().severe("Error en la función: " + e.getMessage());
            
            String errorResponse = String.format(
                "{\n" +
                "  \"error\": \"Error en Azure Function\",\n" +
                "  \"timestamp\": \"%s\",\n" +
                "  \"message\": \"%s\"\n" +
                "}", 
                java.time.LocalDateTime.now(),
                e.getMessage()
            );
            
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body(errorResponse)
                    .build();
        }
    }
    
    /**
     * Verifica la conexión a Oracle Cloud
     */
    private String checkDatabaseConnection() {
        try {
            // Configuración de Oracle Cloud - URL directa
            String url = "jdbc:oracle:thin:@adb.sa-santiago-1.oraclecloud.com:1522/g228b45149ea60f_inventariobd_high.adb.oraclecloud.com";
            String username = "admin";
            String password = "Inventariobd123!";
            
            // Intentar conexión
            Connection connection = DriverManager.getConnection(url, username, password);
            connection.close();
            
            return "conectado";
        } catch (Exception e) {
            return "desconectado: " + e.getMessage();
        }
    }
}
