@echo off
title Minecraft 1.20.1 Forge Client
cd /d "%~dp0"
set "JAVA_HOME=D:\Java\jdk-17.0.20.8-hotspot"
set "GRADLE_USER_HOME=D:\.gradle"
echo Launching Minecraft Forge 1.20.1 Client...
call gradlew.bat runClient
pause
