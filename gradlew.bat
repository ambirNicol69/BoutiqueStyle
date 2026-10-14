@echo off
setlocal
set "GRADLE_VERSION=8.9"
set "GRADLE_HOME=%USERPROFILE%\.gradle\wrapper\dists\boutiquestyle-gradle-%GRADLE_VERSION%\gradle-%GRADLE_VERSION%"
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  set "GRADLE_ZIP=%TEMP%\boutiquestyle-gradle-%GRADLE_VERSION%.zip"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%TEMP%\boutiquestyle-gradle-%GRADLE_VERSION%.zip'; New-Item -ItemType Directory -Force -Path '%USERPROFILE%\.gradle\wrapper\dists\boutiquestyle-gradle-%GRADLE_VERSION%' | Out-Null; Expand-Archive -LiteralPath '%TEMP%\boutiquestyle-gradle-%GRADLE_VERSION%.zip' -DestinationPath '%USERPROFILE%\.gradle\wrapper\dists\boutiquestyle-gradle-%GRADLE_VERSION%' -Force; Remove-Item -LiteralPath '%TEMP%\boutiquestyle-gradle-%GRADLE_VERSION%.zip' -Force"
  if errorlevel 1 exit /b %errorlevel%
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %errorlevel%
