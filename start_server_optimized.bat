@echo off
:start
echo Demarrage du serveur Minecraft avec optimisations renforcees...

REM Arreter tous les processus Java existants pour eviter les conflits
echo Arret des processus Java existants...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM javaw.exe 2>nul

REM Attendre un peu pour que les processus se terminent
timeout /t 2 /nobreak >nul

REM Nettoyer completement le cache Gradle corrompu
echo Nettoyage complet du cache Gradle...
if exist ".gradle" (
    attrib -R -H -S ".gradle\*.*" /S /D 2>nul
    rmdir /S /Q ".gradle" 2>nul
)

REM Nettoyer les fichiers de lock temporaires
del /F /Q "*.lock" 2>nul
del /F /Q "run\*.lock" 2>nul

echo Lancement du serveur avec parametres optimises et stabilises...

REM Arguments JVM optimises pour la stabilite et les performances
set JAVA_OPTS=-Xmx6G -Xms3G
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC
set JAVA_OPTS=%JAVA_OPTS% -XX:+UnlockExperimentalVMOptions
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxGCPauseMillis=50
set JAVA_OPTS=%JAVA_OPTS% -XX:+DisableExplicitGC
set JAVA_OPTS=%JAVA_OPTS% -XX:TargetSurvivorRatio=90
set JAVA_OPTS=%JAVA_OPTS% -XX:G1NewSizePercent=40
set JAVA_OPTS=%JAVA_OPTS% -XX:G1MaxNewSizePercent=75
set JAVA_OPTS=%JAVA_OPTS% -XX:G1MixedGCLiveThresholdPercent=50
set JAVA_OPTS=%JAVA_OPTS% -XX:+AlwaysPreTouch
set JAVA_OPTS=%JAVA_OPTS% -XX:+ParallelRefProcEnabled
set JAVA_OPTS=%JAVA_OPTS% -XX:G1HeapRegionSize=16M
set JAVA_OPTS=%JAVA_OPTS% -Dfml.queryResult=confirm

echo Demarrage avec: %JAVA_OPTS%

REM Options Gradle pour empecher la corruption du cache
set GRADLE_OPTS=-Dorg.gradle.daemon=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.configureondemand=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.parallel=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.caching=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.unsafe.cache.state=false
set GRADLE_OPTS=%GRADLE_OPTS% %JAVA_OPTS%

REM Executer gradlew avec protection anti-corruption
gradlew.bat runServer --no-daemon --refresh-dependencies --rerun-tasks

if errorlevel 1 (
    echo ERREUR: Le serveur a plante. Nettoyage et redemarrage dans 10 secondes...
    if exist ".gradle" rmdir /S /Q ".gradle" 2>nul
    timeout /t 10 /nobreak >nul
    goto :start
)

pause
