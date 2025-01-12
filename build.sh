#!/bin/bash
javac -cp /usr/local/share/micro-manager/jars/MMCoreJ.jar:/usr/local/share/micro-manager/jars/MMAcqEngine.jar:/usr/local/share/micro-manager/jars/MMJ_.jar:/usr/local/share/micro-manager/jars/scijava-common-2.83.3.jar:/usr/local/share/micro-manager/jars/ij-1.53c.jar -d ./build -source 8 -target 8 ./src/fr/telecom/physique/*.java
cd build
jar cvf LiveSPTPlugin.jar *
sudo rm /usr/local/share/micro-manager/mmplugins/LiveSPTPlugin.jar
sudo cp /home/ubuntu/Documents/LiveSPT/build/LiveSPTPlugin.jar /usr/local/share/micro-manager/mmplugins