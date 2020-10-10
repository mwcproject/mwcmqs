#!/bin/bash

# Build and deploy Explorer
#set -x

set -e

FQDN=$1

if [ -z "$FQDN" ]
then
  echo "Usage:  provision_mqs.sh <this host fqdn>"
  exit 1
fi

# Checking if mwc713 is already installed
if [ -z `which mwc713` ]; then
  echo "ERROR. First mwc713 binary need to be installed and be on the standard path. Try command '> mwc713 --version' to verify your install"
  exit 1
fi


# Current directory is expected to be the project root
DEV_ROOT=`pwd`

mvn clean install -DskipTests

#provisioning the config...
cp jetty/webapps/root/WEB-INF/web.xml.ref  jetty/webapps/root/WEB-INF/web.xml

sed -i "s/YOUR_SERVER_FQDN/$FQDN/g" jetty/webapps/root/WEB-INF/web.xml

# Updating autostart
if [ -f "/etc/rc.local" ]; then
    sudo sed -i "/jetty/d" /etc/rc.local
else
  # provision rc.local
  echo "#!/bin/sh -e"  | sudo tee -a /etc/rc.local
  sudo chmod +x /etc/rc.local
fi
echo "Updating rc.local with autostart record"
echo "runuser -l ubuntu -c 'cd $DEV_ROOT/jetty; ./start.sh' &" | sudo tee -a /etc/rc.local

echo "Deployment is done finished. Jetty will listen on the 0.0.0.0:8080. It is expected that Nginx will sit on top as a proxy."
echo "Command line: cd $DEV_ROOT/jetty; ./start.sh"

