@ECHO OFF
REM auto-build if artefacts are missing
IF NOT EXIST "aisafe.app.logserver\target\aisafe.app.logserver-1.0.0-SNAPSHOT.jar" GOTO BUILD
IF NOT EXIST "aisafe.app.logserver\target\dependency" GOTO BUILD
GOTO RUN

:BUILD
echo Build artefacts missing; running mvnw package dependency:copy-dependencies...
call mvnw.cmd package dependency:copy-dependencies -DskipTests -Dmaven.javadoc.skip=true
IF ERRORLEVEL 1 (
    echo Build failed.
    exit /b 1
)

:RUN
SET BASE_CP=aisafe.app.logserver\target\aisafe.app.logserver-1.0.0-SNAPSHOT.jar;aisafe.app.logserver\target\dependency\*;
java -cp %BASE_CP% eapli.aisafe.app.logserver.LogServerApp %*
