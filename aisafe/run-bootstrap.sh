#!/usr/bin/env bash
set -e
if [ ! -f aisafe.app.bootstrap/target/aisafe.app.bootstrap-1.0.0-SNAPSHOT.jar ] \
   || [ ! -d aisafe.app.bootstrap/target/dependency ]; then
    echo "Build artefacts missing; running mvnw package dependency:copy-dependencies..."
    ./mvnw package dependency:copy-dependencies -DskipTests -Dmaven.javadoc.skip=true -Dmaven.javadoc.skip=true
fi

export BASE_CP="aisafe.app.bootstrap/target/aisafe.app.bootstrap-1.0.0-SNAPSHOT.jar:aisafe.app.bootstrap/target/dependency/*"
java -cp "$BASE_CP" eapli.aisafe.app.bootstrap.AisafeBootstrap
