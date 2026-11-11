@echo off
setlocal
set GRADLE_VERSION=8.7
set BASE=%USERPROFILE%\.gradle\wrapper\manual-dists\gradle-%GRADLE_VERSION%
set BIN=%BASE%\gradle-%GRADLE_VERSION%\bin\gradle.bat
if not exist "%BIN%" (
  if not exist "%BASE%" mkdir "%BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip'; Invoke-WebRequest $u -OutFile '%BASE%\gradle.zip'; Expand-Archive -Force '%BASE%\gradle.zip' '%BASE%'"
)
call "%BIN%" %*
