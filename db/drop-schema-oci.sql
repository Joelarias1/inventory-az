-- =====================================================
-- SCRIPT PARA ELIMINAR COMPLETAMENTE EL SCHEMA DE INVENTARIO
-- =====================================================
-- ⚠️ ADVERTENCIA: Este script eliminará TODOS los datos y objetos
-- Ejecutar solo si estás seguro de querer eliminar todo

-- =====================================================
-- ELIMINAR VISTAS PRIMERO (porque dependen de las tablas)
-- =====================================================
DROP VIEW V_RESUMEN_BODEGAS;
DROP VIEW V_PRODUCTOS_STOCK_BAJO;
DROP VIEW V_PRODUCTOS_COMPLETOS;

-- =====================================================
-- ELIMINAR TRIGGERS
-- =====================================================
DROP TRIGGER TR_PRODUCTOS_AUDIT;
DROP TRIGGER TR_BODEGAS_AUDIT;
DROP TRIGGER TR_CATEGORIAS_AUDIT;

-- =====================================================
-- ELIMINAR TABLAS EN ORDEN (por dependencias)
-- =====================================================

-- Primero eliminar MOVIMIENTOS_INVENTARIO (depende de PRODUCTOS y BODEGAS)
DROP TABLE MOVIMIENTOS_INVENTARIO;

-- Luego eliminar PRODUCTOS (depende de CATEGORIAS y BODEGAS)
DROP TABLE PRODUCTOS;

-- Finalmente eliminar las tablas base
DROP TABLE BODEGAS;
DROP TABLE CATEGORIAS;

-- =====================================================
-- MENSAJE DE CONFIRMACIÓN
-- =====================================================
SELECT 'SCHEMA DE INVENTARIO ELIMINADO COMPLETAMENTE' as MENSAJE FROM DUAL;
SELECT 'Objetos eliminados: Vistas, Triggers, Tablas' as OBJETOS FROM DUAL;
SELECT 'Datos perdidos: Todos los productos, bodegas, categorías y movimientos' as DATOS FROM DUAL;

COMMIT;
