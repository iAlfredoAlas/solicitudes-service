# 🧾 Solicitudes API - Backend

## 📌 Descripción

Este proyecto implementa un MVP (Minimum Viable Product) para la gestión de solicitudes de **vacaciones y permisos** por empleado.

El sistema permite:

* Crear solicitudes
* Listar solicitudes por empleado
* Aprobar o rechazar solicitudes
* Obtener un resumen anual de días utilizados

---

## ⚙️ Tecnologías utilizadas

* Java 21
* Spring Boot 3.x
* Spring Data JPA
* MySQL
* Lombok
* Dotenv (para variables de entorno)

---

## 🚀 Cómo ejecutar el proyecto

### 1. Configurar variables de entorno

Crear un archivo `.env` en la raíz del proyecto:

```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=prueba_db
DB_USER=root
DB_PASSWORD=root

SERVER_PORT=8081
```

---

### 2. Ejecutar base de datos (MySQL)

Asegurarse de tener un contenedor o instancia de MySQL corriendo con la base de datos `prueba_db`.

---

### 3. Ejecutar la aplicación

Desde IntelliJ o terminal:

```
mvn spring-boot:run
```

---

## 📡 Endpoints disponibles

### 🔹 Crear solicitud

```
POST /api/requests
```

Ejemplo:

```json
{
  "employeeId": "E001",
  "type": "VACACIONES",
  "startDate": "2026-02-10",
  "endDate": "2026-02-12",
  "reason": "Vacaciones personales"
}
```

Respuestas:

* 201 Created
* 400 Bad Request
* 409 Conflict

---

### 🔹 Listar solicitudes

```
GET /api/requests?employeeId=E001&status=PENDIENTE
```

Respuestas:

* 200 OK
* 400 Bad Request

---

### 🔹 Aprobar / Rechazar

```
PATCH /api/requests/{id}/decision
```

Ejemplo:

```json
{
  "status": "APROBADA",
  "decisionComment": "OK"
}
```

Respuestas:

* 200 OK
* 404 Not Found
* 422 Unprocessable Entity

---

### 🔹 Resumen anual

```
GET /api/requests/summary?employeeId=E001&year=2026
```

Respuesta:

```json
{
  "employeeId": "E001",
  "year": 2026,
  "vacacionesUsed": 3.0,
  "permisoUsed": 1.0
}
```

---

## 📏 Reglas de negocio implementadas

* endDate no puede ser menor que startDate
* No se permite traslape de solicitudes por empleado
* Cálculo de días:

  * VACACIONES → días hábiles (lunes a viernes)
  * PERMISO → días calendario
* Límites:

  * VACACIONES → máximo 10 días
  * PERMISO → máximo 5 días
* Flujo de estados:

  * PENDIENTE → APROBADA / RECHAZADA

---

## ⚠️ Supuestos

1. El límite de días aplica por solicitud, no acumulado anual.
2. Solo se consideran solicitudes en estado **APROBADA** para el resumen anual.
3. No se implementó autenticación ni control de usuarios por alcance del MVP.

---

## 🧠 Decisiones técnicas

1. Se utilizó Spring Data JPA para simplificar el acceso a datos.
2. Se implementó un GlobalExceptionHandler para centralizar el manejo de errores.
3. Se utilizó una consulta personalizada para validar traslapes de fechas.

---

## 🔍 Validación de traslape

Se utilizó la siguiente lógica:

```
startDate <= endDate AND endDate >= startDate
```

Esto permite detectar cualquier intersección entre rangos de fechas.

---

## 📊 Cálculo de días

* Para VACACIONES se excluyen sábados y domingos.
* Para PERMISO se cuentan todos los días.
* El cálculo se realiza en el backend para asegurar integridad.

---

## 🤖 Uso de IA

Se utilizó IA (ChatGPT) como apoyo para:

* Generación de estructura base del proyecto
* Validación de reglas de negocio
* Mejores prácticas en manejo de errores

El código fue revisado y adaptado manualmente para cumplir con los requerimientos de la prueba.

---

## 📌 Estado del proyecto

✔ MVP funcional completo
✔ Cumple con requisitos de la prueba técnica
✔ Listo para demostración
