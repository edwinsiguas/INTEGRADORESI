@echo off
setlocal
cd /d "%~dp0"

echo =======================================================
echo   IMPORTADOR AUTOMATICO DE BASE DE DATOS PARA XAMPP
echo =======================================================
echo.

set MYSQL_EXE=C:\xampp\mysql\bin\mysql.exe

if not exist "%MYSQL_EXE%" (
    echo [ERROR] No se encontro MySQL en C:\xampp\mysql\bin\mysql.exe.
    echo Asegurate de que XAMPP este instalado en C:\xampp.
    pause
    exit /b 1
)

echo 1. Verificando conexion con MySQL en localhost:3306...
"%MYSQL_EXE%" -u root -e "SELECT 1;" >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] No se pudo conectar a MySQL.
    echo Asegurate de que el modulo MySQL este encendido (Start) en el panel de XAMPP.
    echo.
    pause
    exit /b 1
)

echo [OK] MySQL detectado en linea.
echo.

echo 2. Creando base de datos marcona_permisos si no existe...
"%MYSQL_EXE%" -u root -e "CREATE DATABASE IF NOT EXISTS marcona_permisos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if %errorlevel% neq 0 (
    echo [ERROR] No se pudo crear la base de datos marcona_permisos.
    pause
    exit /b 1
)
echo [OK] Base de datos lista.
echo.

echo 3. Ejecutando setup.sql (Estructura de tablas)...
"%MYSQL_EXE%" -u root marcona_permisos < "setup.sql"
if %errorlevel% neq 0 (
    echo [ERROR] Fallo al ejecutar setup.sql.
    pause
    exit /b 1
)
echo [OK] Tablas creadas exitosamente.
echo.

echo 4. Ejecutando init-admin.sql (Usuario Administrador)...
"%MYSQL_EXE%" -u root marcona_permisos < "init-admin.sql"
if %errorlevel% neq 0 (
    echo [ERROR] Fallo al ejecutar init-admin.sql.
    pause
    exit /b 1
)
echo [OK] Administrador insertado: admin@marcona.pe / Admin123*
echo.

echo 5. Aplicando migraciones pendientes...
for %%f in ("migrations\*.sql") do (
    echo    - Aplicando %%~nxf...
    "%MYSQL_EXE%" -u root marcona_permisos < "%%f"
)

echo.
echo =======================================================
echo   BASE DE DATOS IMPORTADA CON EXITO (marcona_permisos)
echo =======================================================
echo.
pause
