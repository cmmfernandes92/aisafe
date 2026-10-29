@ECHO OFF
REM auto-build if artefacts are missing
IF NOT EXIST "aisafe.app.atcc.console\target\aisafe.app.atcc.console-1.0.0-SNAPSHOT.jar" GOTO BUILD
IF NOT EXIST "aisafe.app.atcc.console\target\dependency" GOTO BUILD
GOTO RUN

:BUILD
echo Build artefacts missing; running mvnw package dependency:copy-dependencies...
call mvnw.cmd package dependency:copy-dependencies -DskipTests -Dmaven.javadoc.skip=true
IF ERRORLEVEL 1 (
    echo Build failed.
    exit /b 1
)

:RUN
SET BASE_CP=aisafe.app.atcc.console\target\aisafe.app.atcc.console-1.0.0-SNAPSHOT.jar;aisafe.app.atcc.console\target\dependency\*;
java -cp %BASE_CP% eapli.aisafe.app.atcc.console.AtccApp
