#!/bin/bash

# Script para inicializar Oracle Database con el schema de inventario

echo "=================================="
echo "Inicializando Oracle Database"
echo "Sistema de Inventario"
echo "=================================="

# Esperar a que Oracle esté listo
echo "Esperando a que Oracle Database esté disponible..."
timeout 300 bash -c "until docker exec oracle-xe-inventory sqlplus -s sys/Inventariobd123!@//localhost:1521/XE as sysdba <<< 'SELECT 1 FROM DUAL;' | grep -q '^1'; do sleep 5; echo 'Esperando Oracle...'; done"

if [ $? -eq 0 ]; then
    echo "✅ Oracle Database está disponible!"
    
    # Ejecutar script de schema
    echo "🔧 Creando schema de inventario..."
    docker exec -i oracle-xe-inventory sqlplus -s sys/Inventariobd123!@//localhost:1521/XE as sysdba <<EOF
    -- Ejecutar el schema
    @/docker-entrypoint-initdb.d/startup/oracle-schema-oci.sql
    
    -- Verificar que las tablas se crearon
    SELECT table_name FROM user_tables WHERE table_name IN ('CATEGORIAS', 'BODEGAS', 'PRODUCTOS');
    
    -- Mostrar datos iniciales
    SELECT COUNT(*) as total_categorias FROM CATEGORIAS;
    SELECT COUNT(*) as total_bodegas FROM BODEGAS;
    SELECT COUNT(*) as total_productos FROM PRODUCTOS;
    
    EXIT;
EOF

    echo "✅ Schema de inventario inicializado correctamente!"
    echo ""
    echo "🎯 Base de datos lista para usar:"
    echo "   Host: localhost"
    echo "   Puerto: 1521"
    echo "   SID: XE"
    echo "   Usuario: system"
    echo "   Password: Inventariobd123!"
    
else
    echo "❌ Error: Oracle Database no está disponible después de 5 minutos"
    echo "Revisar logs: docker-compose -f docker-compose-oracle.yml logs oracle-db"
    exit 1
fi