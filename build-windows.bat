@echo off
setlocal
cd /d "%~dp0"

echo === Axiom build preflight ===
where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java is not installed or not on PATH.
  echo Java 21 is required. IntelliJ's project SDK alone does not change the Windows PATH.
  exit /b 1
)
java -version

echo.
echo === Building Axiom ===
call gradlew.bat build --stacktrace
if errorlevel 1 (
  echo.
  echo BUILD FAILED. See the error above.
  exit /b 1
)

echo.
echo BUILD SUCCESSFUL.
echo JAR files are in build\libs\
endlocal
