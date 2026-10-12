#!/bin/sh
cd "$(dirname "$0")"
mkdir -p out
javac -d out $(find src -name '*.java') || exit 1
echo "Starting HomeSense on http://localhost:8080"
java -cp out smarthome.web.WebApp
