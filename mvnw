#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper Startup Unix Script
# ----------------------------------------------------------------------------
set -e

PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
BASE_DIR=`dirname "$PRG"`

if [ -f "$BASE_DIR/.mvn/wrapper/maven-wrapper.jar" ]; then
    exec java -Dmaven.multiModuleProjectDirectory="$BASE_DIR" -cp "$BASE_DIR/.mvn/wrapper/maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain "$@"
else
    exec mvn "$@"
fi
