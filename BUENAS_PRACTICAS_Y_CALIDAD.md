# Estándares de Ingeniería, Buenas Prácticas y Calidad de Código
### Directrices Técnicas Obligatorias para el Ecosistema TRANSIGOVAI

Este documento establece las políticas de desarrollo, reglas de seguridad y criterios de aceptación que todo desarrollador debe acatar para mantener la integridad, seguridad y mantenibilidad de la plataforma.

---

## 1. Seguridad Estricta y Prevención de SQL Injection

En el sistema TRANSIGOVAI **queda terminantemente prohibida** la concatenación directa de cadenas para construir sentencias SQL (`"WHERE id = " + id`). Cualquier código que vulnere esta directriz será rechazado de inmediato en las revisiones de código y bloqueado por SonarQube.

### Reglas Obligatorias para la Capa DAO:
1. **Uso Exclusivo de `JdbcTemplate` con Consultas Parametrizadas:** Todos los parámetros deben pasarse mediante marcadores de posición (`?`), delegando el enlace al motor `PreparedStatement` del driver JDBC de MySQL.
2. **Text Blocks Multilínea de Java 21 (`"""`):** Las consultas extensas deben declararse mediante bloques de texto limpios e indentados, facilitando el análisis visual y estático.
3. **Mapeo de Tipos Estricto:** Utilizar wrappers de tipos primitivos (`Long`, `Integer`, `Double`) para admitir valores nulos de forma controlada.

### Ejemplo de Implementación Correcta:
```java
@Repository
public class PermisoDaoImpl implements IPermisoDao {

    private final JdbcTemplate jdbcTemplate;

    public PermisoDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Preconditions.checkNotNull(jdbcTemplate, "JdbcTemplate no puede ser nulo");
    }

    @Override
    public Optional<Map<String, Object>> obtenerPorNumeroPermiso(String numeroPermiso) {
        String sql = """
                SELECT p.id, p.numero_permiso, p.estado, p.fecha_emision, p.fecha_vencimiento,
                       v.placa, e.razon_social
                FROM permits p
                INNER JOIN vehicles v ON p.vehicle_id = v.id
                INNER JOIN companies e ON p.company_id = e.id
                WHERE p.numero_permiso = ?
                """;

        List<Map<String, Object>> resultados = jdbcTemplate.queryForList(sql, numeroPermiso);
        return resultados.isEmpty() ? Optional.empty() : Optional.of(resultados.get(0));
    }
}
```

---

## 2. Librerías de Soporte Obligatorias y Justificación

Para evitar reinventar la rueda y garantizar código defensivo y libre de `NullPointerException`, el proyecto estandariza el uso de tres librerías fundamentales:

### A. Google Guava (Validación Defensiva con `Preconditions`)
Todo constructor de servicio, DAO o componente debe validar que sus dependencias y argumentos no sean nulos ni inválidos al momento de instanciarse (Fail-Fast Principle):

```java
// Correcto: Falla inmediatamente si una dependencia o argumento es nulo
public UsuarioServiceImpl(IUsuarioDao usuarioDao) {
    this.usuarioDao = Preconditions.checkNotNull(usuarioDao, "El DAO de usuario no puede ser nulo");
}

public void cambiarEstado(Long id, String nuevoEstado) {
    Preconditions.checkNotNull(id, "El identificador no puede ser nulo");
    Preconditions.checkArgument(!nuevoEstado.isBlank(), "El nuevo estado no puede estar vacío");
}
```

### B. Apache Commons (`StringUtils`, `IOUtils`, `Codec`)
* **`StringUtils` (`commons-lang3`):** Se utiliza para comprobaciones de cadenas nulas o vacías (`StringUtils.isNotBlank()`), recorte de espacios y normalización de texto sin riesgo de arrojar excepciones.
* **`IOUtils` (`commons-io`):** Manejo seguro de flujos de bytes (ej. lectura de imágenes de códigos QR y plantillas de reportes Excel).
* **`DigestUtils` (`commons-codec`):** Generación de firmas criptográficas y hashes SHA-256 para validación de integridad documental.

### C. SLF4J y Logback (Prohibición de `System.out.println`)
**Queda estrictamente prohibido** el uso de `System.out.println()` o `e.printStackTrace()` en el código de producción. Todos los mensajes deben emitirse a través del logger de SLF4J:

```java
@Service
public class CirculacionServiceImpl implements ICirculacionService {

    private static final Logger log = LoggerFactory.getLogger(CirculacionServiceImpl.class);

    @Override
    public void registrarPaso(String placa, String ubicacion) {
        log.info("Registrando paso de vehículo con placa: [{}] en ubicación: [{}]", placa, ubicacion);
        try {
            // Lógica transaccional...
        } catch (Exception ex) {
            log.error("Fallo al registrar paso vehicular para la placa [{}]: {}", placa, ex.getMessage(), ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al registrar paso");
        }
    }
}
```

---

## 3. Calidad de Código, Cobertura y Análisis Continuo

El proyecto integra un pipeline automatizado de aseguramiento de calidad configurado en el archivo [pom.xml](file:///backend/pom.xml):

### A. Cobertura de Pruebas con JaCoCo (`jacoco-maven-plugin`)
* **Versión del Plugin:** `0.8.12` (compatible con el motor de ejecución de Java 21 LTS).
* **Fases de Ejecución:**
  * `prepare-agent`: Instrumenta el bytecode de la JVM durante la fase de inicialización de pruebas.
  * `report`: Genera un reporte exhaustivo de cobertura en formato XML en `target/site/jacoco/jacoco.xml`.
* **Coexistencia con Mockito:**
  Para evitar que la bandera `-javaagent` de Mockito (requerida en JDK 21+) anule el agente de JaCoCo, `maven-surefire-plugin` se encuentra configurado con la variable compuesta:
  ```xml
  <configuration>
    <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar} -Xshare:off</argLine>
  </configuration>
  ```

### B. Análisis Estático con SonarQube (`sonar-maven-plugin`)
El proyecto está configurado para evaluarse con SonarQube Scanner bajo el identificador:
* **Project Key:** `transigovai-backend`
* **Project Name:** `Transigovai Backend (MTC Marcona)`

#### Criterios de Aceptación (Quality Gate):
1. **Zero Vulnerabilities:** Cero vulnerabilidades de seguridad críticas o de inyección SQL.
2. **Zero Security Hotspots sin revisar:** Toda manipulación de criptografía o rutas de archivos debe estar auditada.
3. **Mantenibilidad:** Menos de un 5% de duplicación de código y grado A en mantenibilidad (baja deuda técnica).
4. **Cobertura Mínima:** Cobertura de pruebas unitarias superior al 80% sobre clases de servicio y controladores.

---

## 4. Convención de Commits y Versionado

Para mantener un historial de Git limpio y trazable, el equipo sigue el estándar **Conventional Commits**:

| Tipo de Commit | Cuándo Utilizarlo | Ejemplo |
|---|---|---|
| `feat` | Nueva característica o capacidad funcional | `feat(permisos): agregar renovacion automatica con vigencia MTC` |
| `fix` | Corrección de un defecto o error de software | `fix(auth): corregir bloqueo de ip en rate limiter tras 5 intentos` |
| `refactor` | Modificación de código sin alterar su comportamiento externo | `refactor(dao): migrar consultas de empresas hacia IEmpresaDao` |
| `test` | Incorporación o ajuste de pruebas unitarias | `test(service): agregar pruebas de verificacion de codigo QR` |
| `docs` | Modificación de documentación o manuales | `docs: redactar guia de arquitectura y despliegue local` |
| `chore` | Tareas de mantenimiento, dependencias o configuración | `chore(deps): actualizar jacoco-maven-plugin a v0.8.12` |
