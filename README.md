# Distributed Systems Project - Bitbox
Bitbox is a simple Peer-to-Peer file sharing system. Uses either UDP or TCP to transfer files between peers and this can be selected by editing the properties file.

## Dependencies
* [Java 1.8] (https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Maven] (https://maven.apache.org/download.cgi)

## Usage
Build:
```
mvn clean install
```
Run:
The location of *bitbox.jar* varies depending on installation location.
* To run the Bitbox Peer:
```
java -cp bitbox.jar unimelb.bitbox.Peer
```

* To run the Bitbox Client:
``` 
java -cp bitbox.jar unimelb.bitbox.Client
```

## Configuration
Configuration of the Bitbox Peer behaviour can be modified by editing the configuration.properties file. 
