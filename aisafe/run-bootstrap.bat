@ECHO OFF
REM auto-build if artefacts are missing
IF NOT EXIST "aisafe.app.bootstrap\target\aisafe.app.bootstrap-1.0.0-SNAPSHOT.jar" GOTO BUILD
IF NOT EXIST "aisafe.app.bootstrap\target\dependency" GOTO BUILD
GOTO RUN

:BUILD
echo Build artefacts missing; running mvnw package dependency:copy-dependencies...
call mvnw.cmd package dependency:copy-dependencies -DskipTests -Dmaven.javadoc.skip=true
IF ERRORLEVEL 1 (
    echo Build failed.
    exit /b 1
)

:RUN
SET BASE_CP=aisafe.app.bootstrap\target\aisafe.app.bootstrap-1.0.0-SNAPSHOT.jar;aisafe.app.bootstrap\target\dependency\*;
java -cp %BASE_CP% eapli.aisafe.app.bootstrap.AisafeBootstrap
