ECHO OFF
ECHO make sure JAVA_HOME is set to JDK folder
call mvnw.cmd %1 package dependency:copy-dependencies surefire-report:report -Daggregate=true checkstyle:checkstyle-aggregate
