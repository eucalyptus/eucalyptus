#!/bin/sh

#
# Tests report generation for instance data. Returns 0 for success, 1 for failure.
#
# Author: Tom Werges
#


# Gather CLC IP
CLC_IP=`cat ../input/2b_tested.lst |grep '\[.*CLC.*\]'|awk '{ print $1 }'`

# Login, and get session id
wget -O /tmp/sessionId --no-check-certificate "https://$CLC_IP:8443/loginservlet?adminPw=admin"
if [ "$?" -ne "0" ]; then echo "Login failed"; exit 1; fi
export SESSIONID=`cat /tmp/sessionId`
echo "session id:" $SESSIONID

# Clear and generate false data for instances
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=removeFalseData"
if [ "$?" -ne "0" ]; then echo "Data removal failed"; exit 1; fi
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=generateFalseData"
if [ "$?" -ne "0" ]; then echo "Data generation failed"; exit 1; fi

# Generate instance report, based upon data generated above
wget -O /tmp/instanceReport.csv --no-check-certificate "https://$CLC_IP:8443/reports?session=$SESSIONID&name=user_vms&type=csv&page=0&flush=false&start=1104566480000&end=1104571200000&criterionId=2&groupById=0"
if [ "$?" -ne "0" ]; then echo "Report generation failed"; exit 1; fi

# Verify that the resulting report has the correct number of entries in it
REPORT_USERS_CNT=`cat /tmp/instanceReport.csv |  grep 'user-[0-9]\+' | wc -l`
if [ "$REPORT_USERS_CNT" -ne "8" ]; then
	echo "Report users count is incorrect."
	exit -1
fi

# Verify that the report is correct, using the CsvChecker tool.
java -jar CsvChecker.jar 0.2 instanceReference.csv /tmp/instanceReport.csv
if [ "$?" -ne "0" ]; then echo "Report failed values check"; exit 1; fi

