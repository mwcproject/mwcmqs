# MWCMQS

## Overview

mwcmqs (Mimblewimble Coin Message Queue (s) or Mimblewimble Coin Message Queue Secure) is the backed server for mwc713. It supports federation so any number of servers can be setup. Users only need to connect to a single mwcmqs server. All messages will be forwarded to appropriate mwcmqs server. The default server and domain for the mwc713 wallet is mqs.mwc.mw:443, but anyone can setup another server and wallets will be able to connect to it.

## Setup

The prerequisites are java and mvn. Please install both before begining.

- Checkout project:

```# mvn clone https://github.com/mwcproject/mwcmqs```
- cd into project:

```# cd mwcmqs```
- build project:

```# mvn install```

Note: first time running tests will fail because you do not have the jar file created. To bypass this issue, first run:
```# mvn install -DskipTests=true```
On subsequent runs you can just use mvn install.

- remove servlet jar (mvn installs a duplicate):

```# rm -rf jetty/webapps/root/WEB-INF/libs/javax*```

- cd jetty
```# cd jetty```

- copy web.xml.ref to web.xml
```# cp webapps/root/WEB-INF/web.xml.ref webapps/root/WEB-INF/web.xml```

- open web.xml (jetty/webapps/root/WEB-INF/web.xml.ref) and modify the line that says YOUR_SERVER_FQDN. Change this value to the fully qualified domain name of your server. For example:

```
<web-app version="3.1" xmlns="http://xmlns.jcp.org/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd">
    <servlet>
        <servlet-name>listener</servlet-name>
        <servlet-class>servlets.listener</servlet-class>
        <async-supported>true</async-supported>
    </servlet>
    <servlet-mapping>
        <servlet-name>listener</servlet-name>
        <url-pattern>/listener</url-pattern>
    </servlet-mapping>

    <servlet>
         <servlet-name>sender</servlet-name>
        <servlet-class>servlets.sender</servlet-class>
        <init-param>
            <param-name>mwcmqs_domain</param-name>
            <param-value>example.com</param-value>
        </init-param>
    </servlet>
    <servlet-mapping>
        <servlet-name>sender</servlet-name>
        <url-pattern>/sender</url-pattern>
    </servlet-mapping>
</web-app>
```

- start jetty
``` # ./start.sh```

You're done. Now you need to configure an SSL reverse proxy like nginx and install a certificate in it and forward requests to this jetty server which runs on port 8090 or configure jetty to run on port 443 with ssl and install a certificate in jetty. You can go to the next section to configure mwc713 wallets.

## Configuring Wallets to use your mwcmqs server

You can open your wallet713.toml config file. By default, this file is located in ~/.mwc713/floo/wallet713.toml for floonet or ~/.mwc713/main/wallet713.toml for mainnet. Add the following line:

```mwcmqs_domain = "example.com"```

where "example.com" is the domain of the server you installed. You will now be able to send requests to wallets connected to this server. See https://github.com/mwcproject/mwc713/blob/master/docs/mwcmqs_feature.md for more details about using mwc713 to send via mwcmqs.
