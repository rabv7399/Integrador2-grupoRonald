USE delicias_peruanas;

-- =====================================================
-- DATOS INICIALES - DELICIAS PERUANAS
-- APF2 - Curso Integrador II: Sistemas
-- =====================================================

-- 1. CATEGORIAS
INSERT IGNORE INTO categorias (nombre, tipo, descripcion)
VALUES
('Entradas', 'COMIDA', 'Entradas y aperitivos de la gastronomia peruana'),
('Platos de Fondo', 'COMIDA', 'Platos principales de la carta'),
('Bebidas', 'BEBIDA', 'Bebidas disponibles para acompañar los pedidos');

-- 2. MESAS
INSERT IGNORE INTO mesas (numero, capacidad, estado)
VALUES
(1, 4, 'DISPONIBLE'),
(2, 4, 'DISPONIBLE'),
(3, 6, 'DISPONIBLE');

-- 3. PRODUCTOS
INSERT IGNORE INTO productos
(codigo, nombre, descripcion, precio, disponible, categoria_id)
SELECT
'DP-001',
'Lomo Saltado',
'Plato peruano preparado con carne, cebolla, tomate y papas',
25.00,
TRUE,
id
FROM categorias
WHERE nombre = 'Platos de Fondo';

INSERT IGNORE INTO productos
(codigo, nombre, descripcion, precio, disponible, categoria_id)
SELECT
'DP-002',
'Aji de Gallina',
'Plato tradicional peruano a base de pollo y crema de aji',
22.00,
TRUE,
id
FROM categorias
WHERE nombre = 'Platos de Fondo';

INSERT IGNORE INTO productos
(codigo, nombre, descripcion, precio, disponible, categoria_id)
SELECT
'DP-003',
'Chicha Morada',
'Bebida tradicional peruana elaborada a base de maiz morado',
8.00,
TRUE,
id
FROM categorias
WHERE nombre = 'Bebidas';

-- =====================================================
-- VERIFICACION DE DATOS CARGADOS
-- =====================================================

SELECT id, nombre, tipo
FROM categorias
ORDER BY id;

SELECT id, numero, capacidad, estado
FROM mesas
ORDER BY id;

SELECT
    p.id,
    p.codigo,
    p.nombre,
    p.precio,
    p.disponible,
    c.nombre AS categoria
FROM productos p
INNER JOIN categorias c
    ON p.categoria_id = c.id
ORDER BY p.id;