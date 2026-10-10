# Delicias Peruanas — APF2

Proyecto académico del curso **Curso Integrador II: Sistemas**.

## Estado actual

La versión correspondiente al **APF2** migra el núcleo transaccional del proyecto a **Java 21 con Spring Boot**, aplicando una arquitectura por capas y desplegando el backend en la nube.

### Tecnologías principales

- Java 21
- Spring Boot
- Spring Data JPA / Hibernate
- Spring Security
- JWT stateless
- BCrypt
- MySQL 8.4 en Aiven
- JUnit 5, Mockito y MockMvc
- Docker
- Render

## Arquitectura del backend

```text
backend/
├── src/main/java/com/deliciasperuanas/backend/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── security/
│   └── service/
├── src/test/java/
├── database/
│   ├── schema.sql
│   └── data.sql
├── Dockerfile
└── pom.xml
```

## Funcionalidades implementadas

- Autenticación mediante JWT.
- Contraseñas protegidas con BCrypt.
- Autorización por roles CLIENTE, OPERADOR y ADMIN.
- Gestión de categorías, productos, mesas y reservas.
- Prevención de reservas superpuestas.
- Persistencia mediante JPA/Hibernate.
- Consultas parametrizadas para reducir el riesgo de SQL Injection.
- Validaciones de entrada y controles de acceso.
- Pruebas unitarias y de integración.

## Enlaces de revisión

- **Repositorio:** https://github.com/rabv7399/Integrador2-grupoRonald
- **Backend público:** https://delicias-peruanas-api.onrender.com
- **Pull Request de integración APF2:** https://github.com/rabv7399/Integrador2-grupoRonald/pull/32

> La URL base del backend puede responder HTTP 401 porque los recursos están protegidos mediante JWT. La validación funcional se realiza consumiendo los endpoints de la API.

## Evolución desde APF1

La primera versión del proyecto se desarrolló principalmente con HTML/PHP como prototipo inicial. A partir de la retroalimentación del APF1, el núcleo transaccional fue migrado a Java con Spring Boot.

La versión original del frontend se conserva en la rama:

```text
apf1-frontend-original
```

La rama `main` representa la versión técnica actual del **APF2**.
