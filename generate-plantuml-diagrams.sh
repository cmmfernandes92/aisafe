#!/bin/sh
# Regenerates the SVGs of every PlantUML diagram under docs/.
# Uses a local `plantuml` if installed; otherwise downloads the PlantUML jar once into libs/ (gitignored).
echo "LOG: Generate Plantuml Diagrams"
exportFormat="svg"
extra="-SdefaultFontSize=20"
PLANTUML_VERSION=1.2026.2
JAR="libs/plantuml-${PLANTUML_VERSION}.jar"

if command -v plantuml >/dev/null 2>&1; then
  RUN="plantuml"
else
  if [ ! -f "$JAR" ]; then
    mkdir -p libs
    echo "Downloading PlantUML ${PLANTUML_VERSION}..."
    curl -fsSL -o "$JAR" "https://github.com/plantuml/plantuml/releases/download/v${PLANTUML_VERSION}/plantuml-${PLANTUML_VERSION}.jar" || exit 1
  fi
  RUN="java -jar $JAR"
fi

for aFile in $(find docs -name "*.puml" -type f); do
  echo "Processing file: $aFile"
  $RUN $extra -t$exportFormat "$aFile"
done
echo "Finished"
