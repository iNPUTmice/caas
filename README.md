CaaS (Compliance as a Service) for XMPP Servers
===============================================
This is a work in progress web application for checking and visualising compliance status of various XMPP servers, made as a part of Google Summer of Code 2018 for Conversations.im by Rishi Raj.
## Why compliance?

XMPP is an extensible and living standard. Requirments shift over time and thus new extensions (called XEPs) get developed. While server implementors usually react quite fast and are able to cater to those needs it's the server operators who don't upgrade to the latest version or don't enable certain features.

Picking the right extensions to implement or enable isn't always easy. For this reason the XSF has published [XEP-0387 XMPP Compliance Suites 2018](https://xmpp.org/extensions/xep-0387.html) listing the most important extensions to date.

This app won't just help you to assess if your server supports those compliance profiles, but also give you some instructions on how to implement the profiles which are currently not supported. 

## Build instructions

```
mvn package
```
(needs Java 8)

## Run
```
java -jar caas-app/target/caas-app.jar
```
