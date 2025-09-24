@echo off
echo ===============================================
echo NETTOYAGE COMPLET ET LANCEMENT DU SERVEUR
echo ===============================================

REM Arreter tous les processus Java
echo Arret des processus Java...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM javaw.exe 2>nul
taskkill /F /IM gradlew.exe 2>nul
timeout /t 3 /nobreak >nul

REM Nettoyage complet et agressif
echo Nettoyage complet des caches...
if exist ".gradle" (
    echo Suppression de .gradle...
    attrib -R -H -S ".gradle\*.*" /S /D 2>nul
    rmdir /S /Q ".gradle" 2>nul
)

if exist "build" (
    echo Suppression de build...
    rmdir /S /Q "build" 2>nul
)

REM Supprimer tous les fichiers de verrouillage
echo Suppression des verrous...
del /F /Q "*.lock" 2>nul
del /F /Q "**\*.lock" 2>nul

REM Nettoyer le dossier temporaire Windows
echo Nettoyage du cache temporaire...
if exist "%TEMP%\gradle*" rmdir /S /Q "%TEMP%\gradle*" 2>nul

echo ===============================================
echo COMPILATION PROPRE
echo ===============================================

REM Compiler d'abord avec un cache propre
echo Compilation du mod...
set GRADLE_OPTS=-Dorg.gradle.daemon=false -Dorg.gradle.caching=false -Dorg.gradle.parallel=false
gradlew.bat clean build --no-daemon --refresh-dependencies

if errorlevel 1 (
    echo ERREUR: Echec de la compilation
    echo Tentative avec nettoyage encore plus agressif...
    
    REM Nettoyage encore plus agressif
    if exist ".gradle" rmdir /S /Q ".gradle" 2>nul
    if exist "build" rmdir /S /Q "build" 2>nul
    
    REM Recompiler
    gradlew.bat clean build --no-daemon --rerun-tasks
    
    if errorlevel 1 (
        echo ERREUR CRITIQUE: Impossible de compiler
        pause
        exit /b 1
    )
)

echo ===============================================
echo LANCEMENT DU SERVEUR
echo ===============================================

REM Arguments JVM optimises
set JAVA_OPTS=-Xmx8G -Xms4G
set JAVA_OPTS=%JAVA_OPTS% -server
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC
set JAVA_OPTS=%JAVA_OPTS% -XX:+UnlockExperimentalVMOptions
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxGCPauseMillis=37
set JAVA_OPTS=%JAVA_OPTS% -XX:+DisableExplicitGC
set JAVA_OPTS=%JAVA_OPTS% -XX:TargetSurvivorRatio=90
set JAVA_OPTS=%JAVA_OPTS% -XX:G1NewSizePercent=50
set JAVA_OPTS=%JAVA_OPTS% -XX:G1MaxNewSizePercent=80
set JAVA_OPTS=%JAVA_OPTS% -XX:G1MixedGCLiveThresholdPercent=50
set JAVA_OPTS=%JAVA_OPTS% -XX:+AlwaysPreTouch
set JAVA_OPTS=%JAVA_OPTS% -XX:G1HeapRegionSize=16M
set JAVA_OPTS=%JAVA_OPTS% -XX:G1ReservePercent=20

REM Options Gradle pour empecher la corruption
set GRADLE_OPTS=-Dorg.gradle.daemon=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.configureondemand=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.parallel=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.caching=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.unsafe.cache.state=false
set GRADLE_OPTS=%GRADLE_OPTS% %JAVA_OPTS%

echo Lancement du serveur avec optimisations...
gradlew.bat runServer --no-daemon

echo.
echo Serveur arrete.
pause
