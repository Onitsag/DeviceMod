@echo off
:start
echo Demarrage du serveur dedie Minecraft FariTech...

REM Arreter tous les processus Java existants
echo Arret des processus Java...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM javaw.exe 2>nul
timeout /t 3 /nobreak >nul

REM Nettoyer les caches
echo Nettoyage des caches...
if exist ".gradle" rmdir /S /Q ".gradle" 2>nul
del /F /Q "*.lock" 2>nul

REM Creer le JAR si necessaire
echo Compilation du mod...
call gradlew.bat build --no-daemon

if errorlevel 1 (
    echo ERREUR: Echec de la compilation
    pause
    exit /b 1
)

REM Demarrer le serveur avec optimisations maximales
echo Demarrage du serveur avec optimisations...

set JAVA_OPTS=-server
set JAVA_OPTS=%JAVA_OPTS% -Xmx8G -Xms4G
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxGCPauseMillis=37
set JAVA_OPTS=%JAVA_OPTS% -XX:+DisableExplicitGC
set JAVA_OPTS=%JAVA_OPTS% -XX:G1HeapRegionSize=16M
set JAVA_OPTS=%JAVA_OPTS% -XX:G1NewSizePercent=23
set JAVA_OPTS=%JAVA_OPTS% -XX:G1ReservePercent=20
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxTenuringThreshold=1
set JAVA_OPTS=%JAVA_OPTS% -XX:G1HeapWastePercent=5
set JAVA_OPTS=%JAVA_OPTS% -XX:+UnlockDiagnosticVMOptions
set JAVA_OPTS=%JAVA_OPTS% -XX:G1MixedGCCountTarget=4
set JAVA_OPTS=%JAVA_OPTS% -XX:InitiatingHeapOccupancyPercent=15
set JAVA_OPTS=%JAVA_OPTS% -XX:G1MixedGCLiveThresholdPercent=90
set JAVA_OPTS=%JAVA_OPTS% -XX:G1RSetUpdatingPauseTimePercent=5
set JAVA_OPTS=%JAVA_OPTS% -XX:SurvivorRatio=32
set JAVA_OPTS=%JAVA_OPTS% -XX:+PerfDisableSharedMem
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxTenuringThreshold=1
set JAVA_OPTS=%JAVA_OPTS% -Dusing.aikars.flags=https://mcflags.emc.gs
set JAVA_OPTS=%JAVA_OPTS% -Daikars.new.flags=true
set JAVA_OPTS=%JAVA_OPTS% -Dfml.queryResult=confirm
set JAVA_OPTS=%JAVA_OPTS% -Dlog4j2.formatMsgNoLookups=true

echo JVM Flags: %JAVA_OPTS%

cd run
java %JAVA_OPTS% -jar ..\build\libs\faritech-0.4.1-1.12.2.jar nogui

if errorlevel 1 (
    echo Le serveur a crash. Redemarrage dans 15 secondes...
    timeout /t 15 /nobreak >nul
    cd ..
    goto :start
)

cd ..
echo Serveur arrete normalement.
pause
