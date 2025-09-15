#!/bin/bash

# Script para compilar y desplegar el sistema de inventario
# Autor: Sistema Azure Functions
# Fecha: $(date)

set -e  # Salir si algún comando falla

echo "=================================="
echo "Sistema de Inventario - Build & Deploy"
echo "=================================="
echo ""

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_step() {
    echo -e "${BLUE}[PASO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Función para limpiar en caso de error
cleanup() {
    print_error "Error en el proceso de build. Limpiando..."
    docker-compose down --remove-orphans || true
}

trap cleanup ERR

# 1. Verificar dependencias
print_step "Verificando dependencias..."

if ! command -v mvn &> /dev/null; then
    print_error "Maven no está instalado"
    exit 1
fi

if ! command -v docker &> /dev/null; then
    print_error "Docker no está instalado"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose no está instalado"
    exit 1
fi

print_success "Dependencias verificadas"

# 2. Limpiar builds anteriores
print_step "Limpiando builds anteriores..."
docker-compose down --remove-orphans || true
docker system prune -f || true

# 3. Compilar Azure Functions
print_step "Compilando Azure Functions..."
cd azure-functions
mvn clean package -DskipTests
print_success "Azure Functions compiladas"
cd ..

# 4. Compilar BFF
print_step "Compilando BFF..."
cd bff
mvn clean package -DskipTests
print_success "BFF compilado"
cd ..

# 5. Construir imágenes Docker
print_step "Construyendo imágenes Docker..."
docker-compose build --no-cache
print_success "Imágenes Docker construidas"

# 6. Ejecutar contenedores
print_step "Iniciando contenedores..."
docker-compose up -d
print_success "Contenedores iniciados"

# 7. Esperar que los servicios estén listos
print_step "Esperando que los servicios estén listos..."
sleep 10

# 8. Verificar estado de los servicios
print_step "Verificando estado de los servicios..."

# Verificar Azure Functions - REST
print_step "Probando Azure Functions (REST)..."
if curl -f -s "http://localhost:7071/api/ProductFunction?test=1" > /dev/null; then
    print_success "ProductFunction (REST) funcionando ✓"
else
    print_warning "ProductFunction (REST) no responde"
fi

if curl -f -s "http://localhost:7072/api/WarehouseFunction?test=1" > /dev/null; then
    print_success "WarehouseFunction (REST) funcionando ✓"
else
    print_warning "WarehouseFunction (REST) no responde"
fi

# Verificar Azure Functions - GraphQL
print_step "Probando Azure Functions (GraphQL)..."
if curl -f -s "http://localhost:7071/api/ProductFunction/graphql" > /dev/null; then
    print_success "ProductFunction (GraphQL) funcionando ✓"
else
    print_warning "ProductFunction (GraphQL) no responde"
fi

if curl -f -s "http://localhost:7072/api/WarehouseFunction/graphql" > /dev/null; then
    print_success "WarehouseFunction (GraphQL) funcionando ✓"
else
    print_warning "WarehouseFunction (GraphQL) no responde"
fi

# Verificar BFF
if curl -f -s "http://localhost:8080/api/health" > /dev/null; then
    print_success "BFF funcionando ✓"
else
    print_warning "BFF no responde"
fi

echo ""
echo "=================================="
echo "🚀 DESPLIEGUE COMPLETADO"
echo "=================================="
echo ""
echo "📍 Endpoints disponibles:"
echo ""
echo "🔹 Azure Functions - REST:"
echo "   • ProductFunction:   http://localhost:7071/api/ProductFunction"
echo "   • WarehouseFunction: http://localhost:7072/api/WarehouseFunction"
echo ""
echo "🔹 Azure Functions - GraphQL:"
echo "   • ProductFunction:   http://localhost:7071/api/ProductFunction/graphql"
echo "   • WarehouseFunction: http://localhost:7072/api/WarehouseFunction/graphql"
echo ""
echo "🔹 BFF (Backend for Frontend):"
echo "   • API Gateway:       http://localhost:8080/api"
echo ""
echo "📖 Ejemplos de uso:"
echo ""
echo "REST - Listar productos:"
echo "curl http://localhost:7071/api/ProductFunction"
echo ""
echo "GraphQL - Query productos:"
echo "curl -X POST http://localhost:7071/api/ProductFunction/graphql \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"query\":\"{ products { id nombre stock precio } }\"}'"
echo ""
echo "REST - Listar bodegas:"
echo "curl http://localhost:7072/api/WarehouseFunction"
echo ""
echo "GraphQL - Query bodegas:"
echo "curl -X POST http://localhost:7072/api/WarehouseFunction/graphql \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"query\":\"{ warehouses { id nombre direccion estado } }\"}'"
echo ""
echo "📝 Para ver logs:"
echo "docker-compose logs -f [service-name]"
echo ""
echo "🛑 Para detener:"
echo "docker-compose down"
echo ""