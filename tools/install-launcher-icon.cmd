@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-launcher-icon.ps1" %*
exit /b %errorlevel%
