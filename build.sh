#!/bin/sh

cd "$(dirname "$0")"
mvn install -DskipTests=true
rm -rf jetty/webapps/root/WEB-INF/lib/javax.servlet-api-4.0.0-b07.jar
cp jetty/log4j.properties jetty/webapps/root/WEB-INF/classes
