#!/bin/sh

java -Xmx128M -Xms128M -Xss128M -jar start.jar >>logs/stdout 2>&1 &
