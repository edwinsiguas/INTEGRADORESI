# Diccionario de Dominios, Servicios y Funcionalidades
### Catálogo de Capacidades del Sistema TRANSIGOVAI

Este documento actúa como un inventario exhaustivo de los módulos funcionales, contratos de servicio y flujos especiales que componen el núcleo de la aplicación MTC Marcona.

---

## 1. Mapeo Integral de Dominios de Negocio

El sistema organiza sus capacidades en dominios altamente cohesivos y desacoplados:

| Módulo / Dominio | Contratos de Servicio | Contratos DAO | Responsabilidad Funcional Principal |
|---|---|---|---|
| **Usuarios y Seguridad** | `IUsuarioService`<br>`IAutenticacionService`<br>`ITokenLegadoService` | `IUsuarioDao`<br>`IAutenticacionDao` | Gestión de cuentas de usuario, asignación de roles (`admin`, `fiscalizador`, `operador`), encriptación con BCrypt y gestión de sesiones JWT. |
| **Permisos de Circulación** | `IPermisoService`<br>`ICirculacionService` | `IPermisoDao`<br>`ICirculacionDao` | Emisión, renovación, revocación y control de vigencia legal de los permisos de circulación municipal de mototaxis y vehículos urbanos. |
| **Asignaciones** | `IAsignacionService` | `IAsignacionDao` | Vinculación formal de vehículos y conductores con empresas concesionarias autorizadas por la municipalidad. |
| **Empresas Concesionarias** | `IEmpresaService` | `IEmpresaDao` | Registro tributario (RUC), razón social, vigencia de resoluciones de autorización de servicio y flota autorizada. |
| **Conductores** | `IConductorService` | `IConductorDao` | Padrón de conductores, número de DNI, categoría de licencia MTC, fecha de vencimiento y récord de infracciones. |
| **Vehículos (CQRS)** | `IVehiculoConsultaService`<br>`IVehiculoMutacionService` | `IVehiculoConsultaDao`<br>`IVehiculoMutacionDao` | Consulta de unidades por placa/empresa y registro/modificación de características técnicas vehiculares (marca, año, motor). |
| **Fiscalización y Verificación** | `IVerificacionService`<br>`ICodigoQrService` | `IVerificacionDao` | Verificación en vía pública mediante lectura de códigos QR, inspección de vigencia documental y generación de estampas visuales. |
| **Auditoría y Rate Limiting** | `IAuditoriaService`<br>`ILimitePeticionesService` | `IAuditoriaDao`<br>`ILimitePeticionesDao` | Trazabilidad inmutable de operaciones administrativas y mitigación de ataques de fuerza bruta / denegación de servicio. |
| **Reportes y Estadísticas** | `IReporteService`<br>`IEstadisticasService` | `IEstadisticasDao` | Generación de métricas de gestión, estadísticas del parque automotor y exportación masiva de expedientes en Microsoft Excel. |

---

## 2. Flujo Especial: Generación y Exportación de Reportes con Apache POI

La exportación de datos a gran escala hacia hojas de cálculo requiere evitar el consumo desmedido de memoria heap y asegurar una presentación corporativa estandarizada. Este proceso lo gestiona [IReporteService](file:///backend/src/main/java/pe/marcona/mtc/service/IReporteService.java) a través de [ReporteServiceImpl](file:///backend/src/main/java/pe/marcona/mtc/service/impl/ReporteServiceImpl.java) utilizando **Apache POI 5.4.1**.

```mermaid
sequenceDiagram
    autonumber
    actor Operador as Operador / Administrador
    participant Controller as ReportController
    participant Service as ReporteServiceImpl
    participant POI as Apache POI (XSSFWorkbook)
    participant Response as HttpServletResponse

    Operador->>Controller: GET /api/reports/permits/export.php
    Controller->>Service: exportarPermisosExcel(filtro, response)
    Service->>Service: Consultar datos limpios en la capa DAO
    Service->>POI: Instanciar XSSFWorkbook & CreateSheet("Permisos")
    Service->>POI: Crear estilos (Fuentes Calibri Bold, Colores Institucionales, Bordes Finos)
    Service->>POI: Escribir fila de encabezados con metadatos de auditoría
    loop Por cada registro en el conjunto de datos
        Service->>POI: Crear Row y asignar Cell values formateados (Fechas, Moneda, Cadenas)
    end
    Service->>POI: AutoSizeColumn() en columnas clave
    Service->>Response: Definir Content-Type (application/vnd.openxmlformats-officedocument...)
    Service->>Response: Definir Header Content-Disposition (attachment; filename="...")
    Service->>Response: workbook.write(response.getOutputStream())
    Response-->>Operador: Descarga inmediata del archivo .xlsx nativo
```

### Características Técnicas del Motor de Reportes:
1. **Prevención de Fugas de Memoria:** El flujo utiliza streaming directo hacia el `ServletOutputStream`, cerrando el `Workbook` en un bloque `try-with-resources`.
2. **Formateo Tipado:** Los campos de fecha y números no se escriben como cadenas de texto, sino con estilos numéricos reales (`CellStyle.setDataFormat`) para que el usuario pueda aplicar fórmulas y filtros nativos en Excel.
3. **Encabezado y Metadatos Institucionales:** Cada libro exportado incluye metadatos de autoría, fecha de generación y filtros aplicados para control de auditoría interna.

---

## 3. Trazabilidad Inmutable con `IAuditoriaService`

Para cumplir con las normas de control gubernamental y transparencia de la administración pública, toda acción que cree, modifique o inactive registros en el sistema genera un log de auditoría inmutable gestionado por [IAuditoriaService](file:///backend/src/main/java/pe/marcona/mtc/service/IAuditoriaService.java) y [AuditoriaServiceImpl](file:///backend/src/main/java/pe/marcona/mtc/service/impl/AuditoriaServiceImpl.java).

### Estructura de un Evento de Auditoría:
Cada registro almacena los siguientes campos mínimos:
* **`user_id`:** Identificador del usuario que ejecutó la transacción.
* **`action`:** Verbo del evento (ej. `crear_permiso`, `revocar_concesion`, `login_intento`, `cambio_estado_vehiculo`).
* **`entity_type`:** Entidad afectada (`permisos`, `vehicles`, `companies`, `users`).
* **`entity_id`:** Llave primaria del registro alterado.
* **`details`:** Resumen legible del cambio o payload JSON con el estado anterior/nuevo.
* **`ip_address`:** Dirección IP origen del cliente (IPv4 / IPv6).
* **`user_agent`:** Navegador o cliente HTTP utilizado.
* **`created_at`:** Marca de tiempo oficial del servidor (Zona Horaria: `America/Lima`).

### Persistencia Dual para Máxima Seguridad:
1. **Persistencia Relacional (`action_logs`):** Permite consultas rápidas, filtros por usuario y generación de historiales visibles desde la interfaz web administrativa.
2. **Persistencia en Archivo Append-Only (`database-audit.jsonl`):**
   * Configurado en `application.properties` vía `app.audit-file=logs/database-audit.jsonl`.
   * Opera en modo *solo anexar* (append-only), lo que garantiza que incluso si un atacante comprometiera la base de datos relacional, el registro en disco permanece inalterado para peritajes forenses.

---

## 4. Módulo de Seguridad y Limitador de Peticiones (`Rate Limiter`)

El servicio [ILimitePeticionesService](file:///backend/src/main/java/pe/marcona/mtc/service/ILimitePeticionesService.java) previene abusos y denegación de servicio en endpoints sensibles (ej. `/api/auth/login.php`, `/api/verification/verify.php`):

```java
// Ejemplo de invocación en AutenticacionServiceImpl
limitePeticionesService.verificar("login", solicitud.getRemoteAddr(), 5, 900);
```

* **Comportamiento:** Si una misma IP realiza más de `5` peticiones para la acción `login` en una ventana de `900` segundos (15 minutos), el servicio interrumpe la ejecución arrojando una excepción HTTP `429 Too Many Requests` indicando con precisión cuántos minutos debe esperar el usuario antes de reintentar.
* **Persistencia Eficiente:** La tabla `rate_limits` almacena únicamente tuplas de `(ip, action, created_at)` indexadas temporalmente, asegurando lecturas en milisegundos.
