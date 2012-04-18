#!/bin/sh

#
# Runs an exposed command through the CommandServlet
# 
# Usage: runCmd.sh className methodName arg1,arg2,arg3
# Example: ./runCmd.sh com.eucalyptus.reporting.s3.FalseDataGenerator printUsageSummaryMap 1104580000000,1104590000000
#
# Author: Tom Werges
#


# Login, and get session id
wget -O /tmp/sessionId --no-check-certificate 'https://localhost:8443/loginservlet?adminPw=admin'
export SESSIONID=`cat /tmp/sessionId`
echo "session id:" $SESSIONID

# Run cmd
wget --no-check-certificate -O /tmp/nothing "https://localhost:8443/commandservlet?sessionId=$SESSIONID&className=$1&methodName=$2&methodArgs=$3"


