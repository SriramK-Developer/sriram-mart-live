#!/usr/bin/env sh
# Starts SriramMart with the built-in H2 database. Needs JDK 17+ and Maven 3.8+.
set -e
cd "$(dirname "$0")"
mvn -q -DskipTests package
exec java -jar target/srirammart-1.0.0.jar "$@"
