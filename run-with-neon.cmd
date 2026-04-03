@echo off
REM Run Spring Boot application with local PostgreSQL (Docker)

setlocal enabledelayedexpansion

REM Local Docker PostgreSQL Configuration
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/shopflow
set SPRING_DATASOURCE_USERNAME=postgres
set SPRING_DATASOURCE_PASSWORD=changeme
set SPRING_JPA_HIBERNATE_DDL_AUTO=update

echo.
echo ========================================
echo  ShopFlow Application - Local Docker DB
echo ========================================
echo.
echo Connecting to local PostgreSQL...
echo Host: localhost:5432
echo Database: shopflow
echo.
echo Make sure PostgreSQL container is running:
echo   docker compose up postgres -d
echo.

.\mvnw.cmd spring-boot:run

pause
