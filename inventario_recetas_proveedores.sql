-- ============================================================================
--  MÓDULO: INVENTARIO POR RECETA + PROVEEDORES + ÓRDENES DE COMPRA
--  Restaurante Campestre "Los Patos"
--  ----------------------------------------------------------------------------
--  Añade sobre el esquema existente (database_schema.sql):
--    · Almacén de insumos (Insumos)
--    · Ficha técnica / receta de cada plato (PlatoInsumo)
--    · Proveedores (Proveedores)
--    · Órdenes de compra a proveedor (OrdenesCompra + OrdenCompraDetalle)
--    · Datos de ejemplo (seeders): insumos, proveedores, categorías,
--      platos demo y sus recetas.
--
--  Requisito previo: deben existir las tablas CategoriasMenu y PlatosYBebidas
--  (creadas por database_schema.sql / el arranque de la app).
--
--  Es IDEMPOTENTE: se puede ejecutar varias veces sin duplicar datos.
--  Uso:  mysql -u root Restaurante < inventario_recetas_proveedores.sql
-- ============================================================================

CREATE DATABASE IF NOT EXISTS Restaurante CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE Restaurante;

-- ============================================================================
--  1. TABLAS
-- ============================================================================

-- 1.1 Almacén de insumos (materia prima) ------------------------------------
CREATE TABLE IF NOT EXISTS Insumos (
    id_insumo     INT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    unidad_medida VARCHAR(20)  DEFAULT NULL,   -- GRAMOS, MILILITROS, KILOGRAMOS, LITROS, UNIDAD
    stock_actual  DOUBLE       DEFAULT 0,      -- en la unidad de medida
    stock_minimo  DOUBLE       DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.2 Proveedores ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Proveedores (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    razon_social VARCHAR(150) NOT NULL,
    ruc          VARCHAR(11)  NOT NULL,
    telefono     VARCHAR(20)  DEFAULT NULL,
    email        VARCHAR(100) DEFAULT NULL,
    direccion    VARCHAR(200) DEFAULT NULL,
    CONSTRAINT UK_proveedor_ruc UNIQUE (ruc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.3 Receta / ficha técnica: cuánto insumo consume 1 porción de un plato ----
--     (lista de materiales que enlaza PlatosYBebidas con Insumos)
CREATE TABLE IF NOT EXISTS PlatoInsumo (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    id_item   INT    NOT NULL,             -- FK a PlatosYBebidas
    id_insumo INT    NOT NULL,             -- FK a Insumos
    cantidad  DOUBLE NOT NULL,             -- cantidad en la unidad del insumo
    CONSTRAINT FK_platoinsumo_plato  FOREIGN KEY (id_item)   REFERENCES PlatosYBebidas (id_item),
    CONSTRAINT FK_platoinsumo_insumo FOREIGN KEY (id_insumo) REFERENCES Insumos (id_insumo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.4 Órdenes de compra (cabecera) ------------------------------------------
CREATE TABLE IF NOT EXISTS OrdenesCompra (
    id_orden     INT AUTO_INCREMENT PRIMARY KEY,
    id_proveedor INT       NOT NULL,
    fecha        DATETIME(6) NOT NULL,
    total        DOUBLE    NOT NULL DEFAULT 0,
    CONSTRAINT FK_orden_proveedor FOREIGN KEY (id_proveedor) REFERENCES Proveedores (id_proveedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.5 Órdenes de compra (detalle / líneas) ----------------------------------
--     Al registrar la orden, la app suma 'cantidad' al stock del insumo.
--     El costo de insumos por gramo/mililitro se cotiza por kilo/litro:
--     subtotal = (cantidad / 1000) * costo_unitario   (para GRAMOS y MILILITROS)
--     subtotal =  cantidad         * costo_unitario   (para UNIDAD/KILOGRAMOS/LITROS)
CREATE TABLE IF NOT EXISTS OrdenCompraDetalle (
    id_detalle     INT AUTO_INCREMENT PRIMARY KEY,
    id_orden       INT    NOT NULL,
    id_insumo      INT    NOT NULL,
    cantidad       DOUBLE NOT NULL,        -- en la unidad del insumo (g, ml, und)
    costo_unitario DOUBLE NOT NULL,        -- por kilo/litro/unidad según el insumo
    subtotal       DOUBLE NOT NULL,
    CONSTRAINT FK_ocdet_orden  FOREIGN KEY (id_orden)  REFERENCES OrdenesCompra (id_orden),
    CONSTRAINT FK_ocdet_insumo FOREIGN KEY (id_insumo) REFERENCES Insumos (id_insumo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================================
--  2. SEEDERS (datos de ejemplo) — idempotentes
-- ============================================================================

-- 2.1 Categorías del menú (si no existen por nombre) -------------------------
INSERT INTO CategoriasMenu (nombre, descripcion)
SELECT * FROM (SELECT 'Entradas'        AS n, 'Piqueos y entradas frías o calientes' AS d) t
WHERE NOT EXISTS (SELECT 1 FROM CategoriasMenu WHERE nombre = 'Entradas');
INSERT INTO CategoriasMenu (nombre, descripcion)
SELECT * FROM (SELECT 'Platos de Fondo', 'Platos principales de la casa') t
WHERE NOT EXISTS (SELECT 1 FROM CategoriasMenu WHERE nombre = 'Platos de Fondo');
INSERT INTO CategoriasMenu (nombre, descripcion)
SELECT * FROM (SELECT 'Bebidas', 'Gaseosas, cervezas y refrescos') t
WHERE NOT EXISTS (SELECT 1 FROM CategoriasMenu WHERE nombre = 'Bebidas');
INSERT INTO CategoriasMenu (nombre, descripcion)
SELECT * FROM (SELECT 'Postres', 'Dulces y postres tradicionales') t
WHERE NOT EXISTS (SELECT 1 FROM CategoriasMenu WHERE nombre = 'Postres');

-- 2.2 Insumos (almacén) — solo si no existe uno con el mismo nombre ----------
INSERT INTO Insumos (nombre, unidad_medida, stock_actual, stock_minimo)
SELECT * FROM (SELECT 'Arroz' n, 'GRAMOS' u, 5000 s, 1000 m) t
WHERE NOT EXISTS (SELECT 1 FROM Insumos WHERE nombre = 'Arroz');
INSERT INTO Insumos (nombre, unidad_medida, stock_actual, stock_minimo)
SELECT * FROM (SELECT 'Pato', 'UNIDAD', 6, 2) t
WHERE NOT EXISTS (SELECT 1 FROM Insumos WHERE nombre = 'Pato');
INSERT INTO Insumos (nombre, unidad_medida, stock_actual, stock_minimo)
SELECT * FROM (SELECT 'Pollo', 'GRAMOS', 7000, 1500) t
WHERE NOT EXISTS (SELECT 1 FROM Insumos WHERE nombre = 'Pollo');
INSERT INTO Insumos (nombre, unidad_medida, stock_actual, stock_minimo)
SELECT * FROM (SELECT 'Papa', 'GRAMOS', 8000, 1500) t
WHERE NOT EXISTS (SELECT 1 FROM Insumos WHERE nombre = 'Papa');
INSERT INTO Insumos (nombre, unidad_medida, stock_actual, stock_minimo)
SELECT * FROM (SELECT 'Cebolla', 'GRAMOS', 3000, 500) t
WHERE NOT EXISTS (SELECT 1 FROM Insumos WHERE nombre = 'Cebolla');
INSERT INTO Insumos (nombre, unidad_medida, stock_actual, stock_minimo)
SELECT * FROM (SELECT 'Ají amarillo', 'GRAMOS', 1500, 300) t
WHERE NOT EXISTS (SELECT 1 FROM Insumos WHERE nombre = 'Ají amarillo');
INSERT INTO Insumos (nombre, unidad_medida, stock_actual, stock_minimo)
SELECT * FROM (SELECT 'Aceite', 'MILILITROS', 4000, 800) t
WHERE NOT EXISTS (SELECT 1 FROM Insumos WHERE nombre = 'Aceite');
INSERT INTO Insumos (nombre, unidad_medida, stock_actual, stock_minimo)
SELECT * FROM (SELECT 'Culantro', 'GRAMOS', 800, 200) t
WHERE NOT EXISTS (SELECT 1 FROM Insumos WHERE nombre = 'Culantro');
INSERT INTO Insumos (nombre, unidad_medida, stock_actual, stock_minimo)
SELECT * FROM (SELECT 'Limón', 'UNIDAD', 50, 15) t
WHERE NOT EXISTS (SELECT 1 FROM Insumos WHERE nombre = 'Limón');
INSERT INTO Insumos (nombre, unidad_medida, stock_actual, stock_minimo)
SELECT * FROM (SELECT 'Gaseosa personal', 'UNIDAD', 40, 10) t
WHERE NOT EXISTS (SELECT 1 FROM Insumos WHERE nombre = 'Gaseosa personal');
INSERT INTO Insumos (nombre, unidad_medida, stock_actual, stock_minimo)
SELECT * FROM (SELECT 'Cerveza', 'UNIDAD', 30, 8) t
WHERE NOT EXISTS (SELECT 1 FROM Insumos WHERE nombre = 'Cerveza');

-- 2.3 Proveedores — solo si no existe uno con el mismo RUC -------------------
INSERT INTO Proveedores (razon_social, ruc, telefono, email, direccion)
SELECT * FROM (SELECT 'Distribuidora de Alimentos del Norte S.A.C.' r, '20481234567' ru,
                      '944112233' t, 'ventas@alimentosnorte.pe' e, 'Av. Los Almendros 456, Trujillo' d) x
WHERE NOT EXISTS (SELECT 1 FROM Proveedores WHERE ruc = '20481234567');
INSERT INTO Proveedores (razon_social, ruc, telefono, email, direccion)
SELECT * FROM (SELECT 'Avícola Los Andes E.I.R.L.', '20551239874',
                      '955667788', 'contacto@avicolalosandes.pe', 'Jr. Comercio 120, Lima') x
WHERE NOT EXISTS (SELECT 1 FROM Proveedores WHERE ruc = '20551239874');
INSERT INTO Proveedores (razon_social, ruc, telefono, email, direccion)
SELECT * FROM (SELECT 'Bebidas y Licores Perú S.A.', '20609871234',
                      '966554433', 'pedidos@bebidasperu.com', 'Av. Industrial 789, Lima') x
WHERE NOT EXISTS (SELECT 1 FROM Proveedores WHERE ruc = '20609871234');

-- 2.4 Platos demo (si no existe uno con el mismo nombre) ---------------------
INSERT INTO PlatosYBebidas (id_categoria, nombre, descripcion, precio_venta, disponible, imagen_url)
SELECT (SELECT id_categoria FROM CategoriasMenu WHERE nombre='Platos de Fondo' LIMIT 1),
       'Arroz con Pato', 'Arroz verde con pato tierno, cebolla y culantro.', 32.0, 1,
       'https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=600'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PlatosYBebidas WHERE nombre='Arroz con Pato');

INSERT INTO PlatosYBebidas (id_categoria, nombre, descripcion, precio_venta, disponible, imagen_url)
SELECT (SELECT id_categoria FROM CategoriasMenu WHERE nombre='Platos de Fondo' LIMIT 1),
       'Ají de Gallina', 'Cremoso ají de gallina con papa y arroz blanco.', 28.0, 1,
       'https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=600'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PlatosYBebidas WHERE nombre='Ají de Gallina');

INSERT INTO PlatosYBebidas (id_categoria, nombre, descripcion, precio_venta, disponible, imagen_url)
SELECT (SELECT id_categoria FROM CategoriasMenu WHERE nombre='Entradas' LIMIT 1),
       'Papa a la Huancaína', 'Papas sancochadas bañadas en crema de ají.', 14.0, 1,
       'https://images.unsplash.com/photo-1608835291093-394b0c943a75?w=600'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PlatosYBebidas WHERE nombre='Papa a la Huancaína');

INSERT INTO PlatosYBebidas (id_categoria, nombre, descripcion, precio_venta, disponible, imagen_url)
SELECT (SELECT id_categoria FROM CategoriasMenu WHERE nombre='Bebidas' LIMIT 1),
       'Gaseosa Personal', 'Gaseosa personal 500ml bien helada.', 5.0, 1, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PlatosYBebidas WHERE nombre='Gaseosa Personal');

INSERT INTO PlatosYBebidas (id_categoria, nombre, descripcion, precio_venta, disponible, imagen_url)
SELECT (SELECT id_categoria FROM CategoriasMenu WHERE nombre='Bebidas' LIMIT 1),
       'Cerveza', 'Cerveza nacional 355ml.', 10.0, 1, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PlatosYBebidas WHERE nombre='Cerveza');

-- 2.5 Recetas (PlatoInsumo) --------------------------------------------------
--     Enlaza por nombre de plato + nombre de insumo; evita duplicar líneas.
--     Cantidades en la unidad del insumo (gramos, mililitros o unidades).

-- Helper conceptual: cada bloque agrega 1 ingrediente a la receta de un plato.
-- Arroz con Pato
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 220 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Arroz con Pato' AND i.nombre='Arroz'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 0.25 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Arroz con Pato' AND i.nombre='Pato'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 40 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Arroz con Pato' AND i.nombre='Cebolla'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 30 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Arroz con Pato' AND i.nombre='Aceite'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 15 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Arroz con Pato' AND i.nombre='Culantro'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 20 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Arroz con Pato' AND i.nombre='Ají amarillo'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;

-- Ají de Gallina
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 200 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Ají de Gallina' AND i.nombre='Pollo'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 25 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Ají de Gallina' AND i.nombre='Aceite'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 30 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Ají de Gallina' AND i.nombre='Ají amarillo'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 30 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Ají de Gallina' AND i.nombre='Cebolla'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 150 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Ají de Gallina' AND i.nombre='Arroz'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;

-- Papa a la Huancaína
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 250 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Papa a la Huancaína' AND i.nombre='Papa'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 25 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Papa a la Huancaína' AND i.nombre='Ají amarillo'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 20 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Papa a la Huancaína' AND i.nombre='Aceite'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;

-- Gaseosa Personal
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 1 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Gaseosa Personal' AND i.nombre='Gaseosa personal'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;

-- Cerveza
INSERT INTO PlatoInsumo (id_item, id_insumo, cantidad)
SELECT p.id_item, i.id_insumo, 1 FROM PlatosYBebidas p, Insumos i
WHERE p.nombre='Cerveza' AND i.nombre='Cerveza'
  AND NOT EXISTS (SELECT 1 FROM PlatoInsumo z WHERE z.id_item=p.id_item AND z.id_insumo=i.id_insumo) LIMIT 1;

-- ============================================================================
--  FIN. Verificación rápida:
--    SELECT COUNT(*) FROM Insumos;       -- 11
--    SELECT COUNT(*) FROM Proveedores;   -- 3
--    SELECT COUNT(*) FROM PlatoInsumo;   -- 16 (recetas)
-- ============================================================================

