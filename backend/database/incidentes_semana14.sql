-- DELICIAS PERUANAS - SEMANA 14
-- Script de referencia para MySQL. Ejecutar solo en BD de pruebas con respaldo.
-- No usar CREATE DATABASE / USE: se ejecuta en el esquema ya seleccionado.
-- El backend puede crear las tablas automaticamente con DDL_AUTO=update.
CREATE TABLE IF NOT EXISTS estado_ticket(
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 nombre_estado VARCHAR(40) NOT NULL UNIQUE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS prioridad(
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 nivel_nombre VARCHAR(2) NOT NULL UNIQUE,
 descripcion VARCHAR(40) NOT NULL,
 tiempo_sla_minutos INT NOT NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS asignado_a(
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 nombre_responsable VARCHAR(100) NOT NULL,
 nivel_soporte INT NOT NULL,
 especialidad VARCHAR(100)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS ticket(
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 codigo VARCHAR(20) UNIQUE,
 titulo VARCHAR(150) NOT NULL,
 descripcion TEXT NOT NULL,
 modulo_afectado VARCHAR(100) NOT NULL,
 fecha_registro DATETIME(6) NOT NULL,
 usuario_reporta VARCHAR(150) NOT NULL,
 estado_id BIGINT NOT NULL,
 prioridad_id BIGINT NOT NULL,
 asignado_a_id BIGINT NULL,
 CONSTRAINT fk_ticket_estado FOREIGN KEY(estado_id) REFERENCES estado_ticket(id),
 CONSTRAINT fk_ticket_prioridad FOREIGN KEY(prioridad_id) REFERENCES prioridad(id),
 CONSTRAINT fk_ticket_asignado FOREIGN KEY(asignado_a_id) REFERENCES asignado_a(id)
) ENGINE=InnoDB;
-- Catalogos cargados automaticamente por IncidentesInicializador.java.
-- Tiempos P3 y P4: valores operativos propuestos, no establecidos por docente.
-- Comprobacion: SHOW TABLES; SHOW CREATE TABLE ticket; SELECT * FROM ticket ORDER BY id DESC;