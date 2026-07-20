-- ======================================================
-- NUEVO ESQUEMA DE BASE DE DATOS - v2.0
-- RESTAURANTE CAMPESTRE "LOS PATOS"
-- Separación de Inventario (Proveedores) y Carta (Clientes)
-- ======================================================

CREATE DATABASE IF NOT EXISTS Restaurante CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE Restaurante;

-- ==========================================
-- 1. SEGURIDAD Y ROLES
-- ==========================================

CREATE TABLE Roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    permisos TEXT
) ENGINE=InnoDB;

CREATE TABLE Usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    id_rol INT NOT NULL,
    mfa_secret VARCHAR(255) DEFAULT NULL,
    is_mfa_enabled BOOLEAN DEFAULT FALSE,
    account_non_locked BOOLEAN DEFAULT TRUE,
    failed_attempts INT DEFAULT 0,
    last_login DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES Roles(id_rol)
) ENGINE=InnoDB;

-- ==========================================
-- 2. COMPRAS E INVENTARIO (Solo para Proveedores)
-- ==========================================

CREATE TABLE Proveedor (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    razon_social VARCHAR(150) NOT NULL,
    ruc VARCHAR(11) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    email VARCHAR(100)
) ENGINE=InnoDB;

-- Antes llamado 'Productos', ahora es 'Insumos' para el almacén
CREATE TABLE Insumos (
    id_insumo INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    unidad_medida VARCHAR(20), -- Kg, Litros, Unidades, etc.
    stock_actual DECIMAL(10,2) DEFAULT 0.0,
    stock_minimo DECIMAL(10,2) DEFAULT 0.0
) ENGINE=InnoDB;

CREATE TABLE OrdenCompra (
    id_orden INT AUTO_INCREMENT PRIMARY KEY,
    id_proveedor INT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('PENDIENTE', 'RECIBIDO', 'CANCELADO') DEFAULT 'PENDIENTE',
    total DECIMAL(10,2) DEFAULT 0.0,
    CONSTRAINT fk_orden_proveedor FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
) ENGINE=InnoDB;

CREATE TABLE OrdenDetalle (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_orden INT NOT NULL,
    id_insumo INT NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_det_orden FOREIGN KEY (id_orden) REFERENCES OrdenCompra(id_orden) ON DELETE CASCADE,
    CONSTRAINT fk_det_insumo FOREIGN KEY (id_insumo) REFERENCES Insumos(id_insumo)
) ENGINE=InnoDB;

-- ==========================================
-- 3. CARTA Y MENÚ (Solo para Clientes)
-- ==========================================

CREATE TABLE CategoriasMenu (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL, -- Ej: 'Entradas', 'Platos de Fondo', 'Bebidas', 'Postres'
    descripcion VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE PlatosYBebidas (
    id_item INT AUTO_INCREMENT PRIMARY KEY,
    id_categoria INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    precio_venta DECIMAL(10,2) NOT NULL,
    disponible BOOLEAN DEFAULT TRUE,
    imagen_url VARCHAR(255),
    CONSTRAINT fk_item_categoria FOREIGN KEY (id_categoria) REFERENCES CategoriasMenu(id_categoria)
) ENGINE=InnoDB;

-- ==========================================
-- 4. CLIENTES
-- ==========================================

CREATE TABLE Cliente (
    id_cliente INT AUTO_INCREMENT PRIMARY KEY,
    tipo_cliente ENUM('NATURAL', 'JURIDICO') NOT NULL,
    telefono VARCHAR(20),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE Natural_Persona (
    id_cliente INT PRIMARY KEY,
    dni VARCHAR(8) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    CONSTRAINT fk_natural_cliente FOREIGN KEY (id_cliente) REFERENCES Cliente(id_cliente) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Juridico_Persona (
    id_cliente INT PRIMARY KEY,
    ruc VARCHAR(11) NOT NULL UNIQUE,
    razon_social VARCHAR(150) NOT NULL,
    representante VARCHAR(100),
    CONSTRAINT fk_juridico_cliente FOREIGN KEY (id_cliente) REFERENCES Cliente(id_cliente) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 5. PEDIDOS Y SALÓN
-- ==========================================

CREATE TABLE Mesas (
    id_mesa INT AUTO_INCREMENT PRIMARY KEY,
    numero INT NOT NULL UNIQUE,
    capacidad INT NOT NULL,
    estado ENUM('LIBRE', 'OCUPADA', 'RESERVADA', 'MANTENIMIENTO') DEFAULT 'LIBRE'
) ENGINE=InnoDB;

CREATE TABLE Pedidos (
    id_pedido INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('PENDIENTE', 'PREPARANDO', 'SERVIDO', 'PAGADO', 'CANCELADO') DEFAULT 'PENDIENTE',
    total DECIMAL(10,2) DEFAULT 0.0,
    id_mesa INT,
    id_usuario INT, -- El mesero/admin que atiende
    id_cliente INT,
    CONSTRAINT fk_pedido_mesa FOREIGN KEY (id_mesa) REFERENCES Mesas(id_mesa),
    CONSTRAINT fk_pedido_usuario FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES Cliente(id_cliente)
) ENGINE=InnoDB;

-- Detalle del pedido ahora usa PlatosYBebidas en lugar de Insumos
CREATE TABLE PedidoDetalle (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_item_menu INT NOT NULL, -- FK a PlatosYBebidas
    cantidad INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_det_pedido_v2 FOREIGN KEY (id_pedido) REFERENCES Pedidos(id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_det_item_menu FOREIGN KEY (id_item_menu) REFERENCES PlatosYBebidas(id_item)
) ENGINE=InnoDB;

-- ==========================================
-- 6. PAGOS Y TICKETS
-- ==========================================

CREATE TABLE MetodosPago (
    id_metodo INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE Pago (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL UNIQUE,
    id_cliente INT NOT NULL,
    id_metodo INT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_pedido FOREIGN KEY (id_pedido) REFERENCES Pedidos(id_pedido),
    CONSTRAINT fk_pago_cliente FOREIGN KEY (id_cliente) REFERENCES Cliente(id_cliente),
    CONSTRAINT fk_pago_metodo FOREIGN KEY (id_metodo) REFERENCES MetodosPago(id_metodo)
) ENGINE=InnoDB;

CREATE TABLE Tickets (
    id_ticket INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    fecha_emision DATETIME DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(10,2) NOT NULL,
    serie_numero VARCHAR(20) UNIQUE,
    CONSTRAINT fk_ticket_pedido FOREIGN KEY (id_pedido) REFERENCES Pedidos(id_pedido)
) ENGINE=InnoDB;

-- ==========================================
-- DATOS INICIALES DE PRUEBA
-- ==========================================

INSERT INTO Roles (nombre, permisos) VALUES 
('ROLE_ADMIN', 'Acceso total'),
('ROLE_MESERO', 'Pedidos y mesas'),
('ROLE_CAJERO', 'Pagos y tickets');

INSERT INTO CategoriasMenu (nombre, descripcion) VALUES 
('Entradas', 'Aperitivos y piqueos'),
('Platos de Fondo', 'Especialidades de la casa'),
('Bebidas', 'Jugos, refrescos y licores');

INSERT INTO MetodosPago (nombre, descripcion) VALUES 
('Efectivo', 'Pago presencial'),
('Tarjeta', 'POS'),
('Yape/Plin', 'Billetera digital');

-- Usuario admin (admin123)
INSERT INTO Usuario (nombre, email, contrasena, id_rol, is_mfa_enabled, mfa_secret) 
VALUES ('Administrador', 'admin@lospatos.com', '$2a$10$z8PNzVjXBUUkhOPrCCubC.cpV1l62InbmF.XJCVWPEKm31OA3Z8uO', 1, 1,"JBSWY3DPEHPK3PXP");
