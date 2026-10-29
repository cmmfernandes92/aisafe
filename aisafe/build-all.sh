#!/usr/bin/env bash
echo make sure JAVA_HOME is set to JDK folder
./mvnw $1 package dependency:copy-dependencies surefire-report:report -Daggregate=true checkstyle:checkstyle-aggregate
