@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-load-test.ps1" %*
