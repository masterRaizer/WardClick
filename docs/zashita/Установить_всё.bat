@echo off
echo Zapusk ustanovki. Podtverdite zapros prav administratora.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install.ps1"
