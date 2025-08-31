# Sistema de Inventario - Arquitectura Serverless

**Desarrollo Cloud Native II (DSY2207)**

Sistema de inventario de productos implementado con arquitectura serverless, usando Spring Boot como BFF, Azure Functions para operaciones CRUD, y Oracle Database.

## 🏗️ Arquitectura

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   BFF (8080)    │    │ Product Function│    │Warehouse Function│
│  Spring Boot    │───▶│    (7071)       │    │    (7072)       │
│   WebFlux       │    │  Azure Function │    │  Azure Function │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                        │                      │
         │                        ▼                      ▼
         ▼              ┌─────────────────┐    ┌─────────────────┐
┌─────────────────┐     │   Oracle DB     │    │   Oracle DB     │
│   Oracle DB     │     │   (Productos)   │    │   (Bodegas)     │
│   (Backup)      │     │     :1521       │    │     :1521       │
└─────────────────┘     └─────────────────┘    └─────────────────┘
```

## 🚀 Componentes

### 1. BFF (Backend for Frontend) - Puerto 8080
- **Framework**: Spring Boot + WebFlux
- **Función**: Orquesta las llamadas a las funciones serverless
- **Endpoints**: REST API para productos y bodegas

### 2. Product Function - Puerto 7071
- **Tipo**: Azure Function (serverless)
- **Función**: Operaciones CRUD de productos
- **Base de datos**: Oracle (con fallback a datos mock)

### 3. Warehouse Function - Puerto 7072  
- **Tipo**: Azure Function (serverless)
- **Función**: Operaciones CRUD de bodegas
- **Base de datos**: Oracle

### 4. Oracle Database - Puerto 1521
- **Imagen**: Oracle Express Edition 21c
- **Esquema**: Tablas de inventario con datos iniciales

## 📋 Requisitos

- Docker & Docker Compose
- Java 11+
- Maven 3.6+
- Git

## 🛠️ Instalación y Configuración

### Inicio Rápido
```bash
# Clonar el repositorio
git clone <repository-url>
cd inventory-az

# Iniciar todo el sistema (compila automáticamente)
./start.sh
```

### Inicio Manual
```bash
# 1. Compilar proyectos Java
cd bff && mvn clean package -DskipTests && cd ..
cd azure-functions && mvn clean package -DskipTests && cd ..

# 2. Iniciar servicios con Docker
docker-compose up --build -d

# 3. Verificar que los servicios estén corriendo
docker-compose ps
```

## 🔍 Endpoints Disponibles

### BFF (Puerto 8080)

#### Productos
```bash
GET    /api/productos         # Listar productos
POST   /api/productos         # Crear producto
GET    /api/productos/{id}    # Obtener producto por ID
PUT    /api/productos/{id}    # Actualizar producto
DELETE /api/productos/{id}    # Eliminar producto
```

#### Bodegas
```bash
GET    /api/bodegas           # Listar bodegas
POST   /api/bodegas           # Crear bodega  
GET    /api/bodegas/{id}      # Obtener bodega por ID
PUT    /api/bodegas/{id}      # Actualizar bodega
DELETE /api/bodegas/{id}      # Eliminar bodega
```

#### Health Check
```bash
GET    /api/health            # Estado del sistema
```

### Funciones Directas

#### Product Function (Puerto 7071)
```bash
GET    /api/ProductFunction                      # Listar productos
GET    /api/ProductFunction?id={id}              # Obtener por ID
GET    /api/ProductFunction?categoria={id}       # Filtrar por categoría
GET    /api/ProductFunction?bodega={id}          # Filtrar por bodega
POST   /api/ProductFunction                      # Crear producto
PUT    /api/ProductFunction?id={id}              # Actualizar producto
DELETE /api/ProductFunction?id={id}              # Eliminar producto
```

#### Warehouse Function (Puerto 7072)
```bash
GET    /api/WarehouseFunction                    # Listar bodegas
GET    /api/WarehouseFunction?id={id}            # Obtener por ID
POST   /api/WarehouseFunction                    # Crear bodega
PUT    /api/WarehouseFunction?id={id}            # Actualizar bodega
DELETE /api/WarehouseFunction?id={id}            # Eliminar bodega
```

## 🧪 Pruebas

### Probar todos los endpoints automáticamente
```bash
./test-endpoints.sh
```

### Ejemplos de uso manual

```bash
# Listar productos
curl -X GET http://localhost:8080/api/productos

# Crear producto
curl -X POST http://localhost:8080/api/productos \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "TEST-001",
    "nombre": "Producto de Prueba",
    "descripcion": "Producto creado para prueba",
    "stock": 100,
    "stock_minimo": 10,
    "precio": 19990,
    "categoria_id": 1,
    "bodega_id": 1
  }'

# Obtener producto por ID
curl -X GET http://localhost:8080/api/productos/1

# Listar bodegas  
curl -X GET http://localhost:8080/api/bodegas
```

## 🔧 Comandos Útiles

```bash
# Ver logs en tiempo real
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f bff
docker-compose logs -f product-function  
docker-compose logs -f oracle-db

# Reiniciar un servicio específico
docker-compose restart bff

# Detener todos los servicios
docker-compose down

# Limpiar y reiniciar
docker-compose down -v
docker-compose up --build -d
```

## 🗄️ Base de Datos

### Conexión
- **Host**: localhost
- **Puerto**: 1521  
- **SID**: XE
- **Usuario**: system
- **Password**: Inventariobd123!

### Estructura
- **CATEGORIAS**: Categorías de productos
- **BODEGAS**: Información de bodegas
- **PRODUCTOS**: Productos del inventario  
- **MOVIMIENTOS_INVENTARIO**: Historial de movimientos

### Datos Iniciales
El sistema incluye datos de ejemplo:
- 5 categorías básicas
- 1 bodega principal  
- 4 productos de muestra

## 🐛 Troubleshooting

### Problema: Servicios no se conectan
```bash
# Verificar que todos los contenedores estén corriendo
docker-compose ps

# Verificar logs
docker-compose logs -f
```

### Problema: Oracle no inicia
```bash
# Verificar logs de Oracle
docker-compose logs -f oracle-db

# Reiniciar Oracle
docker-compose restart oracle-db
```

### Problema: Functions no responden
```bash
# Verificar que se compilaron correctamente
cd azure-functions && mvn clean package -DskipTests

# Reiniciar functions
docker-compose restart product-function warehouse-function
```

### Problema: Puerto ocupado
```bash
# Verificar puertos en uso
netstat -tulpn | grep -E ":(8080|7071|7072|1521)"

# Cambiar puertos en docker-compose.yml si es necesario
```

## 📊 Monitoreo

### Health Checks
- **BFF**: http://localhost:8080/api/health
- **Functions**: Se incluyen health checks automáticos en Docker

### Logs
Todos los servicios incluyen logging detallado para debugging.

## 🔐 Configuración de Seguridad

Para producción, considera:
- Cambiar passwords por defecto
- Usar variables de entorno para secrets
- Configurar SSL/TLS
- Implementar autenticación y autorización

## 📝 Notas Técnicas

- Las funciones tienen fallback a datos mock si no pueden conectar a Oracle
- El BFF usa WebFlux para llamadas asíncronas a las functions  
- Oracle se configura automáticamente con el schema inicial
- Los contenedores tienen health checks para garantizar disponibilidad

## 🎯 Características Implementadas

✅ Microservicio BFF con Spring Boot  
✅ 2 Azure Functions (Productos y Bodegas)  
✅ Conexión a Oracle Database  
✅ Operations CRUD completas  
✅ Docker Compose para orquestación  
✅ Scripts de automatización  
✅ Health checks y monitoring  
✅ Datos de prueba incluidos  
✅ Manejo de errores  
✅ Logs detallados  

## 👥 Equipo

Sistema desarrollado para **Desarrollo Cloud Native II (DSY2207)**

---

*Para cualquier duda sobre el funcionamiento del sistema, revisar los logs con `docker-compose logs -f` o ejecutar `./test-endpoints.sh` para validar la funcionalidad.*