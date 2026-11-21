# APF3 · Portal público de Delicias Peruanas — Fase 1

## Fase 1.1 · Carta visual ampliada desde Botanero Venta (21/11/2026)
- Se incorporó `legacy-menu.js` con los platos recuperados de `Almuerzo.html`, `Cena.html` y `Bebidas.html`.
- 6 almuerzos, 4 cenas y 11 bebidas del proyecto original; los valores anteriores son **precios históricos referenciales**, no ventas activas.
- Se muestran **primero** los productos reales de `GET /api/productos`. Si un nombre coincide con la carta anterior, prevalece **el precio registrado en MySQL** y no se duplica la tarjeta.
- Se añadieron filtros (Almuerzos, Cenas, Bebidas, categorías), búsqueda textual y modo "Solo productos del sistema".
- Hay 23 fotografías optimizadas extraídas de los recursos originales; **se entregan por separado** en `APF3_Fotografias_Carta_Botanero_Original.zip` y deben extraerse en la raíz del repositorio local. No se han añadido a GitHub todavía.
- Si el backend no responde, la pantalla puede mostrar la carta histórica etiquetada, pero indicará **API desconectada**; esa vista no constituye evidencia de integración funcional.

### Para cargar fotos en tu equipo
1. Detén Spring Boot con Ctrl+C.
2. Extrae `APF3_Fotografias_Carta_Botanero_Original.zip` en la raíz `DeliciasPeruanas_APF3`, aceptando la estructura de carpetas interna.
3. Comprueba que existe `backend/src/main/resources/static/portal/images/portada.jpg`.
4. Ejecuta el servidor otra vez; recarga el navegador con Ctrl+F5.

### Nuevas capturas de evidencia
- `APF3_01_Frontend_Inicio.png`: pantalla de Inicio con fotografía de portada.
- `APF3_02_Carta_Todos.png`: lista completa y el estado `API conectada`, con precios reales y referenciales diferenciados.
- `APF3_03_Carta_Almuerzos.png`: filtro Almuerzos y tarjetas con imágenes.
- `APF3_04_Carta_Cenas.png`: filtro Cenas con platos del proyecto original.
- `APF3_05_Carta_Bebidas.png`: filtro Bebidas.
- `APF3_06_Carta_Solo_Sistema.png`: modo "Solo productos del sistema", con precios que provienen de MySQL.
- `APF3_07_Nosotros.png`: historia y fotografía del local.
- `APF3_08_Busqueda_Plato.png`: filtro textual mostrando un plato.
Guarda las evidencias sin tokens, credenciales ni datos sensibles.



## Alcance implementado
- Portal independiente disponible en `/portal/index.html` (se conserva `/` como demo de Integrador II).
- Inicio, carta (filtros de Almuerzos, Cenas, Bebidas y categorías reales), Nosotros, Contacto y registro de clientes.
- Consumo de servicios REST existentes: `GET /api/productos`, `GET /api/categorias`, `POST /api/auth/registro`.
- **Sin platos ni precios inventados**: el catálogo se carga desde la base de datos. Las categorías del restaurante se mantienen en MySQL.
- Solo lectura pública de productos y categorías; altas, cambios y borrados siguen sujetos a JWT y roles.
- Registro público limitado al rol CLIENTE (regla ya existente de AuthService).
- Los errores HTTP 400, 401, 403, 404 y 409 preservan su significado; no se cambian los contratos de operaciones existentes.

## No implementado todavía
- Pedidos S03, inventario por porciones S05 y paneles específicos MOZO/SUPERVISOR/ADMIN.
- Imágenes finales del restaurante: se pueden incorporar después en `portal/images`, respetando sus nombres originales. Hasta entonces se muestran alternativas visuales.
- No se modificaron tablas, migraciones ni conexiones de Aiven, y **no se desplegó producción**.

## Acceso y ejecución local
1. Abrir `backend` del repositorio en Visual Studio Code o IntelliJ.
2. Configurar la conexión MySQL y las variables de seguridad como en Integrador II (no copiar claves al frontend).
3. Ejecutar `./mvnw spring-boot:run` si existe Maven Wrapper; de lo contrario, `mvn spring-boot:run`.
4. Abrir **http://localhost:8080/portal/index.html**.
5. Verificar `GET /api/productos` y `GET /api/categorias` sin token; verificar que `POST /api/productos` sin JWT siga respondiendo 401.
6. El acceso interno original permanece en **http://localhost:8080/**.

## Capturas de pantalla para la evidencia
Guardar en `evidencias/APF3/` los siguientes archivos:
| Nombre exacto | Qué mostrar |
|---|---|
| `APF3_01_Frontend_Inicio.png` | Encabezado e Inicio del nuevo portal |
| `APF3_02_Carta_Todos.png` | Catálogo cargado desde MySQL con estado de conexión |
| `APF3_03_Carta_Almuerzos.png` | Filtro Almuerzos |
| `APF3_04_Carta_Cenas.png` | Filtro Cenas |
| `APF3_05_Carta_Bebidas.png` | Filtro Bebidas |
| `APF3_06_Nosotros_Contacto.png` | Secciones Nosotros y Contacto |
| `APF3_07_Registro_Cliente_201.png` | Registro exitoso con usuario de prueba, sin mostrar contraseña |
| `APF3_08_Registro_Correo_409.png` | Validación de correo duplicado |
| `APF3_09_Postman_Catalogo_200.png` | GET /api/productos público, respuesta JSON |
| `APF3_10_Postman_Admin_401.png` | POST /api/productos sin JWT, respuesta 401 |

**No inventar capturas.** Capturarlas tras ejecutar localmente o en un despliegue de pruebas y ocultar tokens, claves y datos personales.

## Imágenes del frontend
El CSS y JS buscan archivos opcionales:
`portal/images/portada.jpg`, `nosotros.jpg`, `lomo.jpg`, `rocoto.jpg`, `ceviche.jpg`, `anticucho.jpg`, `chaufa.jpg`, `chicha.jpg`.
La ausencia de imágenes no bloquea el portal: aparecen gradientes o íconos. Se recomienda incorporar las fotografías originales de Botanero Venta antes de la sustentación.

## Criterios académicos
Esta fase sirve como evidencia inicial de consumidor REST y registro de cliente. **No demuestra todavía cinco servicios completos ni la composición y orquestación de pedidos del APF3 SOA**.
