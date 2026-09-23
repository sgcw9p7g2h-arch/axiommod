@echo off
setlocal
set "GRADLE_VERSION=9.2.0"
set "ROOT=%~dp0"
set "DIST=%ROOT%.gradle-dist\gradle-%GRADLE_VERSION%"
if not exist "%DIST%\bin\gradle.bat" (
  echo Downloading Gradle %GRADLE_VERSION%...
  if not exist "%ROOT%.gradle-dist" mkdir "%ROOT%.gradle-dist"
  curl.exe -fL --retry 3 -o "%TEMP%\gradle-%GRADLE_VERSION%.zip" "https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
  if errorlevel 1 exit /b 1
  tar.exe -xf "%TEMP%\gradle-%GRADLE_VERSION%.zip" -C "%ROOT%.gradle-dist"
  if errorlevel 1 exit /b 1
)
call "%DIST%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
