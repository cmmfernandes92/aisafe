#!/usr/bin/env bash
set -e
if [ ! -f aisafe.app.logserver/target/aisafe.app.logserver-1.0.0-SNAPSHOT.jar ] \
   || [ ! -d aisafe.app.logserver/target/dependency ]; then
    echo "Build artefacts missing; running mvnw package dependency:copy-dependencies..."
    ./mvnw package dependency:copy-dependencies -DskipTests -Dmaven.javadoc.skip=true
fi

export BASE_CP="aisafe.app.logserver/target/aisafe.app.logserver-1.0.0-SNAPSHOT.jar:aisafe.app.logserver/target/dependency/*"
# UDP port 9090 for log events, HTTP port 8090 for the viewer (override: ./run-logserver.sh <udp> <http>)
java -cp "$BASE_CP" eapli.aisafe.app.logserver.LogServerApp "$@"
