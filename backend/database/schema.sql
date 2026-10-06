-- =========================================================
-- PROYECTO: DELICIAS PERUANAS
-- SCRIPT DDL - ESQUEMA RELACIONAL NORMALIZADO HASTA 3FN
-- MOTOR: MySQL / compatible MariaDB
-- =========================================================

CREATE DATABASE IF NOT EXISTS delicias_peruanas
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE delicias_peruanas;


-- =========================================================
-- TABLA: usuarios
-- =========================================================
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(20),
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,

        PRIMARY KEY (id),

    CONSTRAINT uq_usuarios_correo
        UNIQUE (correo),

    CONSTRAINT chk_usuarios_rol
        CHECK (rol IN ('ADMIN', 'OPERADOR', 'CLIENTE'))
) ENGINE=InnoDB;


-- =========================================================
-- TABLA: categorias
-- =========================================================
CREATE TABLE IF NOT EXISTS categorias (
    id BIGINT AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(30),
    descripcion VARCHAR(255),

        PRIMARY KEY (id),

    CONSTRAINT uq_categorias_nombre
        UNIQUE (nombre)
) ENGINE=InnoDB;


-- =========================================================
-- TABLA: productos
-- =========================================================
CREATE TABLE IF NOT EXISTS productos (
    id BIGINT AUTO_INCREMENT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    precio DECIMAL(10,2) NOT NULL,
    disponible BOOLEAN NOT NULL DEFAULT TRUE,
    categoria_id BIGINT NOT NULL,

        PRIMARY KEY (id),

    CONSTRAINT uq_productos_codigo
        UNIQUE (codigo),

    CONSTRAINT chk_productos_precio
        CHECK (precio > 0),

    CONSTRAINT fk_productos_categoria
        FOREIGN KEY (categoria_id)
        REFERENCES categorias(id)
) ENGINE=InnoDB;


-- =========================================================
-- TABLA: mesas
-- =========================================================
CREATE TABLE IF NOT EXISTS mesas (
    id BIGINT AUTO_INCREMENT,
    numero INT NOT NULL,
    capacidad INT NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',

        PRIMARY KEY (id),

    CONSTRAINT uq_mesas_numero
        UNIQUE (numero),

    CONSTRAINT chk_mesas_numero
        CHECK (numero > 0),

    CONSTRAINT chk_mesas_capacidad
        CHECK (capacidad > 0),

    CONSTRAINT chk_mesas_estado
        CHECK (estado IN ('DISPONIBLE', 'OCUPADA', 'RESERVADA'))
) ENGINE=InnoDB;


-- =========================================================
-- TABLA: reservas
-- =========================================================
CREATE TABLE IF NOT EXISTS reservas (
    id BIGINT AUTO_INCREMENT,
    cliente VARCHAR(150) NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    cantidad_personas INT NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    mesa_id BIGINT NOT NULL,

        PRIMARY KEY (id),

    CONSTRAINT chk_reservas_personas
        CHECK (cantidad_personas > 0),

    CONSTRAINT chk_reservas_horario
        CHECK (hora_fin > hora_inicio),

    CONSTRAINT chk_reservas_estado
        CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA')),

    CONSTRAINT fk_reservas_mesa
        FOREIGN KEY (mesa_id)
        REFERENCES mesas(id)
) ENGINE=InnoDB;


-- =========================================================
-- INDICES B-TREE
-- =========================================================

CREATE INDEX idx_productos_categoria
    USING BTREE
    ON productos(categoria_id);

CREATE INDEX idx_reservas_mesa_fecha
    USING BTREE
    ON reservas(mesa_id, fecha);

CREATE INDEX idx_reservas_fecha_estado
    USING BTREE
    ON reservas(fecha, estado);

CREATE INDEX idx_usuarios_rol_activo
    USING BTREE
    ON usuarios(rol, activo);