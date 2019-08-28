#!/bin/sh

java -Djava.util.logging.config.file=./logging.properties -Xmx128M -Xms128M -Xss128M -jar start.jar >>logs/stdout 2>&1 &
