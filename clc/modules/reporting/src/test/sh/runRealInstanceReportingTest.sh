#!/bin/sh


# Gather CLC IP
CLC_IP=`cat ../input/2b_tested.lst |grep '\[.*CLC.*\]'|awk '{ print $1 }'`


# Set timing 
WRITE_INTERVAL_MS=60000
SLEEP_TIME_SECS=$(((WRITE_INTERVAL_MS*2)/1000))

# Set image paths
EKI=`euca-describe-images|awk '{ print $2 }'|grep 'eki-'`
ERI=`euca-describe-images|awk '{ print $2 }'|grep 'eri-'`
EMI=`euca-describe-images|awk '{ print $2 }'|grep 'emi-'`


# Login, and get session id
wget -O /tmp/sessionId --no-check-certificate 'https://localhost:8443/loginservlet?adminPw=admin'
if [ "$?" -ne "0" ]; then echo "Login failed"; exit 1; fi
export SESSIONID=`cat /tmp/sessionId`
echo "session id:" $SESSIONID


# Clear all prior data 
wget --no-check-certificate -O /tmp/nothing "https://localhost:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=removeAllData"
if [ "$?" -ne "0" ]; then echo "Clearing failed"; exit 1; fi


# Check that the data is cleared
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=containsRecentRows&methodArgs=false"
if [ "$?" -ne "0" ]; then echo "Data did not clear"; exit 1; fi


# Generate data
wget --no-check-certificate -O /tmp/nothing "https://localhost:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=setWriteIntervalMs&methodArgs=$WRITE_INTERVAL_MS"
if [ "$?" -ne "0" ]; then echo "Setting interval failed"; exit 1; fi
euca-run-instances -n 2 --kernel $EKI --ramdisk $ERI $EMI
if [ "$?" -ne "0" ]; then echo "Data generation failed"; exit 1; fi


# Sleep to allow instance data to propagate to CLC and be recorded
echo "Sleeping for $SLEEP_TIME_SECS seconds..."
sleep $SLEEP_TIME_SECS


# Check that data arrived in DB and has recent timestamp
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=containsRecentRows&methodArgs=true"
if [ "$?" -ne "0" ]; then echo "Data didnt arrive or timestamps incorrect"; exit 1; fi


