#!/bin/sh

cd "$(dirname "$0")"
mvn install -DskipTests=true
rm -rf jetty/webapps/root/WEB-INF/lib/javax.servlet-api-4.0.0-b07.jar
cp jetty/log4j.properties jetty/webapps/root/WEB-INF/classes
cp jetty/webapps/root/WEB-INF/web.xml jetty/webapps/root/WEB-INF/web.xml.bak
cp resources/web.xml jetty/webapps/root/WEB-INF
mvn test
mv jetty/webapps/root/WEB-INF/web.xml.bak jetty/webapps/root/WEB-INF/web.xml
