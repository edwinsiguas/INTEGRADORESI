@echo off
setlocal
cd /d "%~dp0"
echo ========================================================
echo   Iniciando servicio Backend Java Spring Boot MTC Marcona
echo ========================================================
echo.

:: 1. Deteccion automatica de Java 21 (Eclipse Adoptium Temurin u otro)
where java >nul 2>nul
if %errorlevel% neq 0 (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
        set "PATH=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin;%PATH%"
    ) else (
        for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do (
            if exist "%%D\bin\java.exe" (
                set "JAVA_HOME=%%D"
                set "PATH=%%D\bin;%PATH%"
            )
        )
    )
)

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] No se detecto Java JDK 21 en el sistema ni en PATH.
    pause
    exit /b 1
)

:: 2. Deteccion automatica de Maven (Portable integrado en tools o sistema)
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    if exist "%~dp0tools\apache-maven-3.9.9\bin\mvn.cmd" (
        set "PATH=%~dp0tools\apache-maven-3.9.9\bin;%PATH%"
    )
)

where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] No se detecto Apache Maven.
    pause
    exit /b 1
)

echo [OK] Java detectado:
java -version
echo.
echo [OK] Iniciando Spring Boot en puerto 8080 (presiona Ctrl+C para detener)...
echo.
mvn spring-boot:run
pause
