#!/bin/sh

# Gather CLC IP
CLC_IP=`cat ../input/2b_tested.lst |grep '\[.*CLC.*\]'|awk '{ print $1 }'`


# Login, and get session id
wget -O /tmp/sessionId --no-check-certificate "https://$CLC_IP:8443/loginservlet?adminPw=admin"
if [ "$?" -ne "0" ]; then echo "Login failed"; exit 1; fi
export SESSIONID=`cat /tmp/sessionId`
echo "session id:" $SESSIONID


# Clear all prior data 
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.storage.FalseDataGenerator&methodName=removeAllData"
if [ "$?" -ne "0" ]; then echo "Clearing failed"; exit 1; fi


# Check that prior data is cleared
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.storage.FalseDataGenerator&methodName=containsRecentRows&methodArgs=false"
if [ "$?" -ne "0" ]; then echo "Data did not clear"; exit 1; fi


# Generate data
echo "Creating volumes"
euca-create-volume --size 1 --zone myPartition
euca-create-volume --size 1 --zone myPartition
if [ "$?" -ne "0" ]; then echo "Data generation failed"; exit 1; fi


# Check that data arrived in DB and has recent timestamp
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.storage.FalseDataGenerator&methodName=containsRecentRows&methodArgs=true"
if [ "$?" -ne "0" ]; then echo "Data didnt arrive or timestamps incorrect"; exit 1; fi


