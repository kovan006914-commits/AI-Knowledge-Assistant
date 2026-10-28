@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java not found. Install JDK 17 and reopen the terminal.
  pause
  exit /b 1
)
if not exist "target\AI-Knowledge-Assistant-1.0.0.jar" (
  call mvnw.cmd -DskipTests package
  if errorlevel 1 (
    echo Build failed. See the error above.
    pause
    exit /b 1
  )
)
echo Open http://localhost:8080 after the server has started.
java -jar "target\AI-Knowledge-Assistant-1.0.0.jar"
if errorlevel 1 pause
