@echo off
REM Run Spring Boot application with Neon PostgreSQL database

setlocal enabledelayedexpansion

REM Neon Database Configuration
set SPRING_DATASOURCE_URL=jdbc:postgresql://ep-patient-cell-anuaimud-pooler.c-6.us-east-1.aws.neon.tech/neondb?sslmode=require^&user=neondb_owner^&password=npg_OrDkcQb6p1GZ^&channel_binding=require
set SPRING_DATASOURCE_USERNAME=neondb_owner
set SPRING_DATASOURCE_PASSWORD=npg_OrDkcQb6p1GZ
set SPRING_JPA_HIBERNATE_DDL_AUTO=update

echo.
echo ========================================
echo  ShopFlow Application - Neon Database
echo ========================================
echo.
echo Connecting to Neon PostgreSQL...
echo Host: ep-patient-cell-anuaimud-pooler.c-6.us-east-1.aws.neon.tech
echo Database: neondb
echo.

.\mvnw.cmd spring-boot:run

pause
