> Actualización de redacción 22/11/2026: se prepararon capítulos y diagramas propuestos en `docs/apf3/apf3_bpm_b2b_secciones_informe.md` y un borrador Word/PDF externo de 10 páginas. **No cambia la condición de evidencias y entrega final pendientes.**

# Estado de cobertura de la rúbrica — SOA APF3

> Corte de avance: 22/11/2026, rama `feature/soa-apf3-operacion-restaurante`. Este documento es un **diagnóstico de preparación**, no una calificación docente. No confundir APF3 de SOA con APF3 de Integrador II.

## Matriz de criterios (20 puntos)

| Criterio | Puntos | Estado del proyecto | Evidencia que falta cerrar |
|---|---:|---|---|
| 5 servicios con contratos, operaciones, seguridad, errores y capas | 4 | **Implementación sólida**: S01 Identidad, S02 Carta/Productos, S03 Pedido/Cocina, S04 Reservas, S05 Inventario; adicional S06 Caja/Ventas | Tabla de contratos JSON por servicio, endpoint/HTTP/rol/errores, esquema de capas y capturas reales |
| Consumidor web y fundamentos SOAP/REST, code-first/contract-first, integración y descubrimiento/UDDI | 4 | **Consumidor REST desarrollado** (portal público y panel de rol) | Cuadro comparativo SOAP vs REST, justificación del proyecto, code-first vs contract-first, servicio alternativo SOAP, UDDI y 2–3 capturas de consumo |
| Proceso con 4 servicios empresariales, orquestación/coreografía/ESB y modelo B2B de 5 servicios | 4 | **Flujo real** S01→S02→S05→S03 y S06; **diseño B2B redactado** con cinco servicios externos propuestos. Sin ESB ni servicios distribuidos implementados | Diagrama de orquestación, análisis coreografía vs orquestación, razón para NO usar ESB. **Verificar y explicar 5 servicios B2B potenciales ya redactados**, participantes, REST/SOAP/EDI y 2 controles de seguridad |
| BPM: BPMN As-Is y To-Be, 2 mejoras, 2 KPI, correspondencia BPM–SOA | 4 | **Borrador redactado** en `docs/apf3/apf3_bpm_b2b_secciones_informe.md` con As-Is/To-Be, KPI y diagrama de integración; pendiente revisión de figuras e integración al informe final | Diagramas BPMN de atención manual y digital: piscinas/carriles, tareas/gateways/eventos; cuantificar dos mejoras y definir 2 KPIs con fórmula y seguimiento |
| 3–5 conclusiones coherentes | 2 | **Pendiente en el informe consolidado** | Escribir conclusiones sobre beneficios, límites de SOA, seguridad, trazabilidad, pruebas |
| Exposición/presentación/video | 2 | **Pendiente de verificar** | Diapositivas, demo funcional, guion y video según las instrucciones oficiales del curso |

## Afirmaciones técnicamente correctas para defensa

1. **Monolito modular con servicios REST/JSON** en Spring Boot, no seis microservicios independientes.
2. La **orquestación** de negocio sucede en servicios del backend, dentro de transacciones locales; no hay bus ESB ni UDDI operativo.
3. El panel presenta roles CLIENTE, MOZO, COCINERO, CAJA, SUPERVISOR, ADMIN. **COCINERO es el único que marca LISTO**; **MOZO el único que marca ENTREGADO**. Administración supervisa Cocina solo lectura.
4. Las vistas de pedidos y Cocina se actualizan por **sondeo REST cada cinco segundos**, no por WebSocket.
5. La demo utiliza H2 volátil en puerto 8081. **No** se ha desplegado sobre Render ni se han aplicado migraciones a Aiven. Capturas del demo no deben presentarse como producción.

## Próximas acciones ordenadas por impacto en nota

**Primero: cerrar código con capturas:** login por rol, pedido MESA/LLEVAR, observación y anulación parcial, Cocina solo pendiente y control COCINERO, stock y salidas por fecha, Caja y ticket, venta del día, reserva. Especialmente:
- `SOA_APF3_64_Mozo_Comanda_Compacta.png` — lista corta, botón Editar plato.
- `SOA_APF3_65_Mozo_Editar_Plato_Desplegable.png` — controles abiertos en solo una línea.
- `SOA_APF3_66_Admin_Cocina_Solo_Lectura.png` — seguimiento sin marcar LISTO.
- `SOA_APF3_67_Cocinero_Cocina_Marcar_Listo.png` — mismo pendiente con opción LISTO autorizada.

**Segundo: BPM y B2B:** completar primero los dos BPMN y los 5 escenarios externos B2B, porque cada bloque vale 4 puntos y aún faltan artefactos que se puedan evaluar. Ejemplos de servicios B2B solo **propuestos**: aprovisionamiento de proveedores, cotización de insumos, facturación electrónica autorizada, seguimiento de repartos y pagos con pasarela externa (no afirmar integraciones operativas). Controles de diseño: TLS autenticado y tokens o firmas verificables por intercambio.

**Tercero: memoria SOA consolidada:** incluir y revisar APF1/APF2 según el enunciado, estructuras/diagramas, tablas de REST, discusión SOAP/UDDI, 3–5 conclusiones y fuentes IEEE. Revisar requisitos de entrega PDF, Arial 11 y extensión mínima en la guía del curso.

**Cuarto: presentación y video:** ensayar recorrido cliente→mozo→cocinero→caja e inventario con contraseña temporal y datos no sensibles. No mostrar tokens JWT, contraseñas ni información bancaria real.

**No tomar las pruebas automatizadas como sustituto de la evidencia BPM/B2B.** El resultado técnico es fuerte; la nota final depende también del análisis escrito, las capturas reales, los diagramas y la exposición.
