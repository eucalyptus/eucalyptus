#!/bin/sh

#
# Generates fake reporting data
#
# Be sure to build the test classes and install them before running this as it
#  relies upon them. From /clc, execute ant build-test install
#
# Author: Tom Werges
#


CLC_IP="localhost"

if [ -z "$1" ]
then
	echo "arg missing: admin pw"
	exit -1
fi

# Login, and get session id
wget -O /tmp/sessionId --no-check-certificate "https://$CLC_IP:8443/loginservlet?adminPw=$1"
if [ "$?" -ne "0" ]; then echo "Login failed"; exit 1; fi
SESSIONID=`cat /tmp/sessionId`
echo "session id:" $SESSIONID

# Clear and generate false data for instances
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=removeFalseData"
if [ "$?" -ne "0" ]; then echo "Instance data removal failed"; exit 1; fi
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=generateFalseData"
if [ "$?" -ne "0" ]; then echo "Instance data generation failed"; exit 1; fi


# Clear and generate false data for storages
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.storage.FalseDataGenerator&methodName=removeFalseData"
if [ "$?" -ne "0" ]; then echo "Storage data removal failed"; exit 1; fi
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.storage.FalseDataGenerator&methodName=generateFalseData"
if [ "$?" -ne "0" ]; then echo "Storage data generation failed"; exit 1; fi


# Clear and generate false data for s3
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.s3.FalseDataGenerator&methodName=removeFalseData"
if [ "$?" -ne "0" ]; then echo "S3 data removal failed"; exit 1; fi
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.s3.FalseDataGenerator&methodName=generateFalseData"
if [ "$?" -ne "0" ]; then echo "S3 data generation failed"; exit 1; fi


