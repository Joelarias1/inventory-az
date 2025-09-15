# Guía de Pruebas para API GraphQL en Postman

Este archivo contiene todas las `queries` y `mutations` necesarias para probar los endpoints de GraphQL del sistema de inventario.

**Instrucciones:**
1.  En Postman, selecciona el método `POST`.
2.  Establece la URL correspondiente (para productos o bodegas).
3.  Ve a la pestaña `Body` y selecciona la opción `GraphQL`.
4.  Copia el contenido del bloque `Query` en el campo "Query".
5.  Copia el contenido del bloque `GraphQL Variables` en el campo "GraphQL Variables".
6.  Envía la petición.

---

## 🚀 API de Productos

**URL del Endpoint**: `https://funciones-inventario-joel.azurewebsites.net/api/productfunction/graphql`

### 1. Obtener todos los productos

No necesita variables.

**Query:**
```graphql
query GetAllProducts {
  products {
    id
    sku
    nombre
    stock
    precio
    estado
    categoria_id
    bodega_id
  }
}
```

**GraphQL Variables:**
```json
{}
```

---

### 2. Obtener un producto por ID

**Query:**
```graphql
query GetProductById($productId: Int!) {
  product(id: $productId) {
    id
    nombre
    descripcion
    stock
    stock_minimo
    stock_maximo
    precio
  }
}
```

**GraphQL Variables:**
```json
{
  "productId": 1
}
```

---

### 3. Crear un nuevo producto

**Query:**
```graphql
mutation CreateNewProduct($sku: String!, $nombre: String!, $stock: Int!, $precio: Float!) {
  createProduct(sku: $sku, nombre: $nombre, stock: $stock, precio: $precio) {
    id
    sku
    nombre
    stock
    precio
  }
}
```

**GraphQL Variables:**
```json
{
  "sku": "GQL-POSTMAN-002",
  "nombre": "Producto Creado desde Guía",
  "stock": 120,
  "precio": 9990.0
}
```

---

### 4. Actualizar un producto existente

**Query:**
```graphql
mutation UpdateExistingProduct($productId: Int!, $nombre: String, $stock: Int, $precio: Float) {
  updateProduct(id: $productId, nombre: $nombre, stock: $stock, precio: $precio) {
    id
    nombre
    stock
    precio
    descripcion
  }
}
```

**GraphQL Variables:**
```json
{
  "productId": 1,
  "nombre": "Laptop HP Re-Actualizada",
  "stock": 25
}
```

---

### 5. Eliminar un producto por ID

**Query:**
```graphql
mutation DeleteExistingProduct($productId: Int!) {
  deleteProduct(id: $productId)
}
```

**GraphQL Variables:**
```json
{
  "productId": 6
}
```
*(Nota: El resultado será `true` si se eliminó correctamente, o `false`/`null` si no se encontró).*

---
---

## 📦 API de Bodegas

**URL del Endpoint**: `https://funciones-inventario-joel.azurewebsites.net/api/warehousefunction/graphql`

### 1. Obtener todas las bodegas

No necesita variables.

**Query:**
```graphql
query GetAllWarehouses {
  warehouses {
    id
    nombre
    direccion
    estado
    capacidad_max
    ocupacion_porcentaje
    productos_count
  }
}
```

**GraphQL Variables:**
```json
{}
```

---

### 2. Obtener una bodega por ID

**Query:**
```graphql
query GetWarehouseById($warehouseId: Int!) {
  warehouse(id: $warehouseId) {
    id
    nombre
    direccion
    responsable
    email
    estado
    productos_count
  }
}
```

**GraphQL Variables:**
```json
{
  "warehouseId": 1
}
```

---

### 3. Obtener capacidad de una bodega

**Query:**
```graphql
query GetWarehouseCapacity($warehouseId: Int!) {
  warehouseCapacity(id: $warehouseId) {
    warehouse_id
    warehouse_nombre
    capacidad_max
    stock_total
    ocupacion_porcentaje
  }
}
```

**GraphQL Variables:**
```json
{
  "warehouseId": 1
}
```

---

### 4. Crear una nueva bodega

**Query:**
```graphql
mutation CreateNewWarehouse($nombre: String!, $direccion: String!, $capacidad: Int!) {
  createWarehouse(nombre: $nombre, direccion: $direccion, capacidad_max: $capacidad) {
    id
    nombre
    direccion
    estado
    capacidad_max
  }
}
```

**GraphQL Variables:**
```json
{
  "nombre": "Bodega Creada desde Guía",
  "direccion": "Av. Markdown 789, Santiago",
  "capacidad": 7500
}
```

---

### 5. Actualizar una bodega existente

**Query:**
```graphql
mutation UpdateExistingWarehouse($warehouseId: Int!, $nombre: String, $estado: String) {
  updateWarehouse(id: $warehouseId, nombre: $nombre, estado: $estado) {
    id
    nombre
    estado
    direccion
  }
}
```

**GraphQL Variables:**
```json
{
  "warehouseId": 1,
  "nombre": "Bodega Principal (Actualizada desde Guía)",
  "estado": "ACTIVO"
}
```

---

### 6. Eliminar una bodega por ID

**Query:**
```graphql
mutation DeleteExistingWarehouse($warehouseId: Int!) {
  deleteWarehouse(id: $warehouseId)
}
```

**GraphQL Variables:**
```json
{
  "warehouseId": 3
}
```
*(Nota: El resultado será `true` si se eliminó correctamente, o `false`/`null` si no se encontró).*
