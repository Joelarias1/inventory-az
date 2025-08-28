package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger - Versión simplificada
 */
public class Function {
    
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        final String query = request.getQueryParameters().get("name");
        final String name = request.getBody().orElse(query);

        String greeting = (name == null) ? "Mundo" : name;
        
        String response = String.format(
            "{\n" +
            "  \"saludo\": \"¡Hola, %s!\",\n" +
            "  \"timestamp\": \"%s\",\n" +
            "  \"function\": \"Azure Function - Sistema de Inventario\",\n" +
            "  \"status\": \"funcionando\",\n" +
            "  \"message\": \"Función desplegada exitosamente en Azure\",\n" +
            "  \"endpoints\": {\n" +
            "    \"health\": \"/api/health\",\n" +
            "    \"productos\": \"/api/productos\",\n" +
            "    \"bodegas\": \"/api/bodegas\"\n" +
            "  }\n" +
            "}", 
            greeting,
            java.time.LocalDateTime.now()
        );
        
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(response)
                .build();
    }
}
