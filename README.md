# 🍳 OhMyFreezer

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x-brightgreen)
![Flutter](https://img.shields.io/badge/Flutter-3-blue)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

> Sistema profesional de gestión de ingredientes y recetas para cocinas profesionales.

**OhMyFreezer** permite a los equipos de cocina gestionar su inventario de ingredientes, consultar recetas paso a paso, registrar elaboraciones y recibir alertas automáticas cuando el stock cae por debajo del mínimo definido.

---

# 📸 Visión general

El sistema está compuesto por:

* **Backend REST API** desarrollado con **Spring Boot**
* **Base de datos relacional** en **MySQL**
* **Aplicación móvil** desarrollada con **Flutter**

Permite centralizar la gestión de cocina y mejorar el control de inventario.

---

# 🏗 Arquitectura del sistema

```mermaid
flowchart LR

A[Flutter Mobile App] -->|HTTP REST| B[Spring Boot Backend API]
B --> C[(MySQL Database)]

B --> D[Servicio de Recetas]
B --> E[Servicio de Ingredientes]
B --> F[Servicio de Usuarios]
B --> G[Servicio de Alertas]
B --> H[Servicio de Estadísticas]
```

---

# 🧱 Arquitectura del Backend

El backend sigue una **arquitectura en capas** típica de Spring Boot.

```mermaid
flowchart TB

Controller --> Service
Service --> Repository
Repository --> Database[(MySQL)]

Controller[Controllers\nREST API]
Service[Services\nLógica de negocio]
Repository[Repositories\nJPA / Hibernate]
Database[(MySQL)]
```

### Capas

**Controller**

* Expone los endpoints REST
* Maneja las peticiones HTTP

**Service**

* Contiene la lógica de negocio
* Valida datos
* Coordina repositorios

**Repository**

* Acceso a base de datos mediante **Spring Data JPA**

---

# 🗄 Modelo conceptual de datos

```mermaid
erDiagram

USUARIO {
    int id
    string nombre
    string email
    string password
    string rol
}

INGREDIENTE {
    int id
    string nombre
    float cantidad
    float stockMinimo
}

RECETA {
    int id
    string nombre
    string descripcion
}

RECETA_INGREDIENTE {
    int receta_id
    int ingrediente_id
    float cantidad
}

PASO_RECETA {
    int id
    int receta_id
    int orden
    string descripcion
}

REGISTRO_USO_RECETA {
    int id
    int receta_id
    datetime fecha
}

USUARIO ||--o{ REGISTRO_USO_RECETA : usa
RECETA ||--o{ PASO_RECETA : contiene
RECETA ||--o{ RECETA_INGREDIENTE : requiere
INGREDIENTE ||--o{ RECETA_INGREDIENTE : participa
```

---

# 📁 Estructura del repositorio

```
OhMyFreezer
│
├── backend
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── config
│
└── frontend
    └── flutter_app
```

---

# 🔧 Backend

## Tecnologías

* Java 17
* Spring Boot 3
* Spring Data JPA
* Hibernate
* MySQL 8
* Maven

---

## Requisitos

* JDK 17+
* MySQL 8
* Maven 3.8+

---

## Configuración

Crear el archivo:

```
backend/src/main/resources/application.properties
```

con el siguiente contenido:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ohmyfreezer
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

server.port=8080
```

---

## Ejecutar backend

```bash
cd backend
mvn spring-boot:run
```

API disponible en:

```
http://localhost:8080/api
```

---

# 🔌 Endpoints principales

| Módulo       | Endpoint            |
| ------------ | ------------------- |
| Usuarios     | `/api/usuarios`     |
| Ingredientes | `/api/ingredientes` |
| Recetas      | `/api/recetas`      |
| Alertas      | `/api/alertas`      |
| Registros    | `/api/registros`    |
| Estadísticas | `/api/estadisticas` |

---

# 📱 Frontend (Flutter)

## Tecnologías

* Flutter 3
* Provider (gestión de estado)
* http (cliente REST)
* shared_preferences
* flutter_local_notifications
* connectivity_plus

---

## Requisitos

* Flutter SDK 3+
* Android Studio o VSCode
* Emulador Android o dispositivo físico

---

## Configuración

Editar:

```
frontend/flutter_app/lib/services/api_client.dart
```

y configurar la IP del backend:

```dart
static const String baseUrl = 'http://192.168.X.X:8080/api';
```

---

## Ejecutar aplicación

```bash
cd frontend/flutter_app
flutter pub get
flutter run
```

---

# 👥 Roles de usuario

| Rol            | Permisos                                                            |
| -------------- | ------------------------------------------------------------------- |
| Cocinero       | Consultar ingredientes y recetas, elaborar recetas paso a paso      |
| Jefe de cocina | Gestionar recetas e ingredientes, ver estadísticas, recibir alertas |

---

# 🔔 Sistema de alertas

El sistema genera **alertas automáticas** cuando un ingrediente cae por debajo de su `stockMinimo`.

Funcionamiento:

1. Se descuenta stock al elaborar una receta
2. El backend comprueba el nivel mínimo
3. Se genera una alerta si es necesario
4. La app consulta alertas periódicamente

---

# 🚀 Roadmap

Próximas mejoras:

* Dashboard de estadísticas avanzado
* Exportación de informes
* Integración con tablets en cocina
* Sistema de pedidos internos

---

# 📌 Estado del proyecto

| Módulo        | Estado           |
| ------------- | ---------------- |
| Backend       | ✅ Implementado   |
| Frontend      | 🚧 En desarrollo |
| Base de datos | ✅ Modelada       |

---

# 📄 Licencia

Proyecto desarrollado con fines educativos y profesionales.

---

# 👨‍💻 Autor

**Ares Caballero**

Proyecto desarrollado como parte de un sistema de gestión para cocinas profesionales.
