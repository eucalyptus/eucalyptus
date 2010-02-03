#!/bin/bash
#

if [ "$1" = "build" ]; then
	echo "$0 in build"
	rm -f clc/modules/vmware-broker/src/main/java/edu/ucsb/eucalyptus/cloud/ws/BrokerLicenseTool.java
fi

if [ "$1" = "install" ]; then
	echo "$0 in install"
	rm -rf /usr/src/ZKM/images
	java -jar /usr/src/ZKM/ZKM.jar /usr/src/ZKM/1.6-vmware-broker-script.txt
	cp /usr/src/ZKM/images/eucalyptus-vmware-broker*.jar /usr/share/eucalyptus
	vmware_client_class="`grep "^Class:" /tmp/ChangeLog.txt|grep VMwareClient|cut -f 3`"
	sed -i "s/edu.ucsb.eucalyptus.cloud.ws.VMwareClient/$vmware_client_class/g" /usr/share/eucalyptus/euca_vmware
fi
