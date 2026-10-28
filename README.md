# TRANSIGOVAI — Sistema de Gestión y Fiscalización de Permisos de Transporte
### Municipalidad Distrital de Marcona — Subgerencia de Transportes y Seguridad Vial

grupo 5
integrantes
1.	Siguas Astorga, Edwin Emir- 
2.	Huaccho Perales, Neil Victor 
3.	Chavez Sanchez, Zarella 
4.	Rafaele Matos, Leidy Diana


![Java](https://img.shields.io/badge/Backend-Java%2021%20LTS-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Framework-Spring%20Boot%203.5.3-brightgreen?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/Frontend-React%2019%20%7C%20TypeScript-blue?style=for-the-badge&logo=react)
![Vite](https://img.shields.io/badge/Bundler-Vite%208-purple?style=for-the-badge&logo=vite)
![MySQL](https://img.shields.io/badge/Database-MySQL%208.0%20(XAMPP)-lightblue?style=for-the-badge&logo=mysql)
![SonarQube](https://img.shields.io/badge/Quality-SonarQube%20%2B%20JaCoCo-4C9BD4?style=for-the-badge&logo=sonarqube)

---

## 1. Visión General del Proyecto

**TRANSIGOVAI** es la plataforma tecnológica empresarial desarrollada para la **Municipalidad Distrital de Marcona (MTC)**, orientada a la administración, emisión, renovación, revocación y fiscalización en vía pública de títulos habilitantes (autorizaciones de servicio y permisos de circulación de mototaxis y transporte urbano).

El sistema integra:
* Control estricto de flota vehicular, empresas concesionarias y conductores autorizados.
* Fiscalización en tiempo real mediante **códigos QR criptográficos** legibles desde dispositivos móviles en vía pública.
* Trazabilidad documental completa (SOAT, Revisiones Técnicas, Tarjetas de Propiedad y Licencias de Conducir).
* Registro inmutable de acciones de usuarios con auditoría dual (base de datos relacional y archivo persistente).
* Motor de reportería analítica y exportación masiva en formato **Microsoft Excel (.xlsx)**.

---

## 2. Stack Tecnológico

El proyecto opera bajo una arquitectura desacoplada de alto rendimiento dividida en tres capas principales:

### Frontend (SPA Web Moderna)
* **Librería Core:** [React 19](https://react.dev/) + [TypeScript 5.7](https://www.typescriptlang.org/)
* **Herramienta de Construcción:** [Vite 8](https://vite.dev/) con proxy reverso para integración transparente con la API.
* **Diseño y UI:** [Tailwind CSS 3.4](https://tailwindcss.com/), Radix UI Primitives, Lucide Icons y Sonner (notificaciones toast).
* **Consumo HTTP:** Axios con interceptores automáticos de autenticación JWT y manejo centralizado de errores.
* **Escaneo en Tiempo Real:** Librería `html5-qrcode` para lectura física de códigos QR por inspectores municipales.

### Backend (API REST Empresarial)
* **Plataforma:** [Java 21 LTS](https://adoptium.net/) (Eclipse Temurin / OpenJDK).
* **Framework:** [Spring Boot 3.5.3](https://spring.io/projects/spring-boot).
* **Persistencia:** Spring JDBC (`JdbcTemplate`) con consultas 100% parametrizadas (prevención de SQL Injection).
* **Seguridad:** Spring Security Crypto (algoritmo BCrypt con costo 12), tokens de sesión y limitador de peticiones (Rate Limiter).
* **Generación Documental:** [Apache POI 5.4](https://poi.apache.org/) (generación de hojas de cálculo) y [ZXing 3.5](https://github.com/zxing/zxing) (renderizado de códigos QR vectoriales y de mapa de bits).
* **Utilidades Core:** Google Guava (validación con `Preconditions`) y Apache Commons (`Lang3`, `IO`, `Codec`).

### Infraestructura y Calidad
* **Base de Datos Principal:** MySQL 8.0 gestionado localmente a través de **XAMPP** (puerto estándar `3306`).
* **Calidad y Cobertura:** [JaCoCo 0.8.12](https://www.jacoco.org/jacoco/) para métricas de cobertura de código.
* **Auditoría Estática:** [SonarQube LTS Community](https://www.sonarqube.org/) orquestado mediante **Docker Compose** (puerto `9000`).

---

## 3. Estructura del Monorepo Limpio

El repositorio implementa una estricta separación de responsabilidades a nivel de sistema de archivos:

```text
TRANSIGOVAI/
├── backend/                       # Núcleo API REST Java Spring Boot
│   ├── src/main/java/pe/marcona/  # Código fuente empresarial (Patrón DAO + SOLID)
│   │   ├── config/                # CORS, seguridad y configuraciones de Spring
│   │   ├── controller/            # Controladores REST (/api/*)
│   │   ├── dao/                   # Interfaces y repositorios JDBC (Data Access Objects)
│   │   ├── dto/                   # Objetos de transferencia y respuestas API estandarizadas
│   │   ├── exception/             # Manejador global de excepciones
│   │   └── service/               # Lógica de negocio (Interfaces e Implementaciones)
│   ├── src/main/resources/        # application.properties y recursos del servidor
│   ├── src/test/java/             # Suite de pruebas unitarias y de integración
│   ├── tools/                     # Maven portátil integrado para ejecución autónoma
│   ├── INICIAR_JAVA.bat           # Script de arranque en 1 clic para Windows
│   └── pom.xml                    # Configuración de dependencias, JaCoCo y SonarQube
│
├── frontend/                      # Cliente SPA React 19 + Vite
│   ├── src/                       # Código fuente de componentes, vistas y hooks
│   │   ├── components/            # Componentes visuales modulares y reutilizables
│   │   ├── hooks/                 # Hooks de React (autenticación, estado global)
│   │   ├── lib/                   # Cliente API (Axios), utilidades y adaptadores
│   │   └── pages/ o App.tsx       # Vistas y enrutamiento del sistema
│   ├── .env.example               # Plantilla de variables de entorno (VITE_API_URL)
│   ├── package.json               # Dependencias de npm y scripts de ejecución
│   └── vite.config.ts             # Configuración del servidor y proxy (/api -> 8080)
│
├── infraestructura/               # Recursos de base de datos, despliegue y auditoría
│   ├── scripts/                   # Scripts SQL de inicialización y migraciones
│   │   ├── migrations/            # Scripts de evolución del esquema
│   │   ├── IMPORTAR_BD_XAMPP.bat  # Script para crear y migrar la BD en XAMPP con 1 clic
│   │   ├── init-admin.sql         # Inserción de credenciales maestras iniciales
│   │   └── setup.sql              # Creación de tablas, llaves foráneas e índices
│   ├── docker-compose.sonar.yml   # Contenedor aislado de SonarQube para calidad de código
│   ├── docker-compose.yml         # Alternativa opcional contenerizada para MySQL/phpMyAdmin
│   └── sync_to_xampp.sh           # Script histórico de sincronización
│
├── README.md                      # Documento principal y visión general
├── GUIA_EJECUCION.md              # Manual de puesta en marcha paso a paso
├── ARQUITECTURA_Y_PATRONES.md     # Especificación de arquitectura, SOLID y CQRS
├── DOMINIOS_Y_FUNCIONES.md        # Diccionario detallado de módulos y servicios
└── BUENAS_PRACTICAS_Y_CALIDAD.md  # Estándares de seguridad, dependencias y auditoría
```

---

## 4. Índice de Documentación Oficial

Para profundizar en cualquier aspecto del ciclo de vida del software, consulta las guías específicas:

1. 🚀 **[GUIA_EJECUCION.md](./GUIA_EJECUCION.md):** Manual detallado paso a paso para encender XAMPP, compilar el backend, ejecutar el frontend y levantar el auditor SonarQube.
2. 📐 **[ARQUITECTURA_Y_PATRONES.md](./ARQUITECTURA_Y_PATRONES.md):** Fundamentos del MVC moderno, aplicación del Patrón DAO, segregación CQRS en vehículos y la convención de "Spanglish Arquitectónico".
3. 📖 **[DOMINIOS_Y_FUNCIONES.md](./DOMINIOS_Y_FUNCIONES.md):** Diccionario funcional de servicios de negocio, exportación con Apache POI y trazabilidad de auditoría.
4. 🛡️ **[BUENAS_PRACTICAS_Y_CALIDAD.md](./BUENAS_PRACTICAS_Y_CALIDAD.md):** Estándares de prevención de inyección SQL, uso de librerías base (Guava/Commons/Logback) y auditoría con JaCoCo y SonarQube.

---

## 5. Credenciales Maestras por Defecto

* **URL de Acceso Web:** `http://localhost:5173`
* **Correo Electrónico:** `admin@marcona.pe`
* **Contraseña:** `Admin123*`
* **Rol:** Administrador General (`admin`)
