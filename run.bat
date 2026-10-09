@echo off
rem Starts SriramMart with the built-in H2 database. Needs JDK 17+ and Maven 3.8+.
cd /d "%~dp0"
call mvn -q -DskipTests package || exit /b 1
java -jar target\srirammart-1.0.0.jar %*
