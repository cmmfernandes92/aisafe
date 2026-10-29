#!/usr/bin/env bash
set -e
if [ ! -f aisafe.app.atcc.console/target/aisafe.app.atcc.console-1.0.0-SNAPSHOT.jar ] \
   || [ ! -d aisafe.app.atcc.console/target/dependency ]; then
    echo "Build artefacts missing; running mvnw package dependency:copy-dependencies..."
    ./mvnw package dependency:copy-dependencies -DskipTests -Dmaven.javadoc.skip=true
fi

export BASE_CP="aisafe.app.atcc.console/target/aisafe.app.atcc.console-1.0.0-SNAPSHOT.jar:aisafe.app.atcc.console/target/dependency/*"
java -cp "$BASE_CP" eapli.aisafe.app.atcc.console.AtccApp
