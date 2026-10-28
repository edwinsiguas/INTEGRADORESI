# Guía de Ejecución y Puesta en Marcha Local
### Manual de Despliegue para Entornos de Desarrollo

Esta guía describe el procedimiento técnico detallado para instalar dependencias, inicializar la base de datos, compilar y ejecutar tanto el **Backend (Java Spring Boot)** como el **Frontend (React + Vite)** en estaciones de trabajo locales bajo un esquema compatible con **XAMPP**.

---

## 1. Prerrequisitos del Sistema

Antes de iniciar, asegúrate de contar con el siguiente software instalado en tu equipo:

| Componente | Versión Mínima Recomendada | Propósito |
|---|---|---|
| **Java JDK** | Java 21 LTS (Eclipse Temurin / OpenJDK) | Entorno de ejecución y compilación del backend |
| **Node.js** | Node.js v20.x o v24.x LTS (con npm 10+) | Entorno de ejecución para dependencias y bundling del frontend |
| **XAMPP** | Versión 8.0 o superior (con MySQL y Apache) | Servidor de base de datos relacional local |
| **Apache Maven** | Versión 3.9+ (o utilizar el Maven integrado en `backend/tools`) | Gestor de dependencias y empaquetado Java |
| **Docker Desktop** | Opcional (Requerido solo para SonarQube) | Auditoría de calidad de código y análisis estático |

---

## 2. Paso 1: Preparación de la Base de Datos (MySQL vía XAMPP)

El sistema utiliza la base de datos `marcona_permisos` en el puerto estándar `3306`.

### A. Encendido de Servicios
1. Abre el panel de control **XAMPP Control Panel**.
2. Presiona el botón **Start** junto al módulo **Apache**.
3. Presiona el botón **Start** junto al módulo **MySQL**.
4. Ambos módulos deben mostrarse con fondo verde y los puertos asignados (`80`, `443` para Apache y `3306` para MySQL).

### B. Inicialización del Esquema y Credenciales

Tienes dos alternativas para cargar la base de datos:

#### Opción 1: Ejecución Automatizada en 1 Clic (Recomendada para Windows)
Navega a la carpeta `infraestructura/scripts/` y haz doble clic en el archivo:
```text
infraestructura/scripts/IMPORTAR_BD_XAMPP.bat
```
Este script realiza las siguientes acciones de forma desatendida:
1. Comprueba la conexión activa con `C:\xampp\mysql\bin\mysql.exe`.
2. Crea la base de datos `marcona_permisos` con juego de caracteres `utf8mb4`.
3. Ejecuta `setup.sql` creando todas las tablas, índices y llaves foráneas.
4. Ejecuta `init-admin.sql` creando el usuario maestro `admin@marcona.pe` con contraseña encriptada en BCrypt.
5. Itera y aplica automáticamente los scripts contenidos en `migrations/`.

#### Opción 2: Carga Manual vía phpMyAdmin
1. Abre tu navegador web e ingresa a: [http://localhost/phpmyadmin/](http://localhost/phpmyadmin/).
2. Haz clic en **Nueva** en la barra lateral izquierda y crea una base de datos llamada:
   * **Nombre:** `marcona_permisos`
   * **Cotejamiento:** `utf8mb4_unicode_ci`
3. Selecciona la base de datos recién creada y dirígete a la pestaña **Importar**.
4. Carga y ejecuta los scripts ubicados en `infraestructura/scripts/` en el **orden estricto** siguiente:
   * **Primero:** `setup.sql` (Estructura base del sistema).
   * **Segundo:** `init-admin.sql` (Usuario administrador de arranque).
   * **Tercero:** Todos los archivos `.sql` dentro de `migrations/` en orden cronológico.

---

## 3. Paso 2: Ejecución del Backend (Java Spring Boot)

El backend ya está preconfigurado en `backend/src/main/resources/application.properties` para conectarse a XAMPP en `localhost:3306` con usuario `root` y clave en blanco, exponiendo sus endpoints en el puerto `8080`.

### A. Ejecución Rápida en Windows (Script Inteligente)
Dentro de la carpeta `backend/`, haz doble clic en:
```text
backend/INICIAR_JAVA.bat
```
*Este script detecta automáticamente tu instalación de Java 21 LTS y utiliza el binario de Maven integrado en `backend/tools/apache-maven-3.9.9`, iniciando la aplicación sin requerir configuración manual de variables de entorno.*

### B. Ejecución Manual por Línea de Comandos
Si prefieres usar una terminal (PowerShell, Bash o CMD):
```bash
# 1. Navegar a la carpeta del backend
cd backend

# 2. Levantar la aplicación con Maven
mvn spring-boot:run
```

### C. Validación de Arranque
Cuando el backend haya iniciado correctamente, verás en la consola:
```text
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.5.3)

... Started Application in X.XXX seconds (process running for X.XXX)
```
* **Endpoint de Salud / API:** `http://localhost:8080/api`
* **Puerto de Escucha:** `8080`

---

## 4. Paso 3: Ejecución del Frontend (React 19 + Vite)

El frontend opera como una Single Page Application moderna sobre Vite, configurada para resolver peticiones API a través de su proxy reverso en el puerto `5173`.

### A. Instalación de Dependencias
Abre una **nueva ventana de terminal** y ejecuta:
```bash
# 1. Navegar a la carpeta del frontend
cd frontend

# 2. Instalar los paquetes de Node.js (solo necesario la primera vez o tras cambios en package.json)
npm install
```

### B. Inicio del Servidor de Desarrollo
```bash
# Iniciar servidor de desarrollo con Hot Module Replacement (HMR)
npm run dev
```

### C. Validación del Frontend
La terminal confirmará que el servidor local está activo:
```text
  VITE v8.3.4  ready in 250 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

Abre tu navegador en:
👉 **[http://localhost:5173](http://localhost:5173)**

### D. Resolución de CORS y Proxy Reverso
* Durante el desarrollo local, Vite intercepta cualquier petición a `/api/*` y la reenvía internamente al backend (`http://localhost:8080`).
* Adicionalmente, el backend cuenta con [CorsConfiguration.java](file:///backend/src/main/java/pe/marcona/mtc/config/CorsConfiguration.java) autorizando explícitamente los orígenes `http://localhost:5173` y `http://127.0.0.1:5173` con credenciales activas, evitando cualquier bloqueo por políticas de mismo origen en los navegadores.

---

## 5. Paso 4: Inicio de Sesión y Verificación de Flujo

1. Dirígete a `http://localhost:5173`.
2. Se desplegará la pantalla de autenticación corporativa.
3. Ingresa las credenciales maestras:
   * **Correo:** `admin@marcona.pe`
   * **Contraseña:** `Admin123*`
4. Al hacer clic en **Iniciar Sesión**, el backend verificará el hash BCrypt, emitirá el token de sesión y el frontend cargará el Dashboard General con las estadísticas del parque automotor.

---

## 6. Paso 5: Auditoría de Calidad Local con SonarQube (Opcional)

Para ejecutar análisis estático de código, detección de vulnerabilidades y verificación de cobertura de pruebas JaCoCo:

### A. Levantar SonarQube en Docker
Desde la raíz del proyecto, ejecuta:
```bash
docker compose -f infraestructura/docker-compose.sonar.yml up -d
```
*Espera aproximadamente 60 a 90 segundos a que el contenedor inicialice su motor de Elasticsearch embebido.*
* **Consola Web de SonarQube:** `http://localhost:9000`
* **Credenciales por defecto:** Usuario `admin`, Contraseña `admin` (te solicitará cambiar la contraseña en el primer acceso).

### B. Ejecución de Pruebas Unitarias y Generación de Reporte JaCoCo
Desde la carpeta `backend/`:
```bash
cd backend
mvn clean test
```
*Esto ejecutará las pruebas unitarias y generará el archivo de cobertura XML en:*
`backend/target/site/jacoco/jacoco.xml`.

### C. Envío del Análisis Estático a SonarQube
Ejecuta el escáner de Sonar apuntando a tu instancia local:
```bash
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=admin -Dsonar.password=admin
```
*(O reemplaza `-Dsonar.login` / `-Dsonar.password` por `-Dsonar.token=TU_TOKEN` si generaste un token en SonarQube).*

Revisa los resultados inmediatamente en `http://localhost:9000/dashboard?id=transigovai-backend`.
