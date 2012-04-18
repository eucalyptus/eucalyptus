#!/bin/sh

#
# Tests report generation for storage data. Returns 0 for success, 1 for failure.
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

# Clear and generate false data for storages
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.storage.FalseDataGenerator&methodName=removeFalseData"
if [ "$?" -ne "0" ]; then echo "Data removal failed"; exit 1; fi
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.storage.FalseDataGenerator&methodName=generateFalseData"
if [ "$?" -ne "0" ]; then echo "Data generation failed"; exit 1; fi

# Generate storage report, based upon data generated above
wget -O /tmp/storageReport.csv --no-check-certificate "https://$CLC_IP:8443/reports?session=$SESSIONID&name=user_storage&type=csv&page=0&flush=false&start=1104580000000&end=1104590000000&criterionId=2&groupById=0"
if [ "$?" -ne "0" ]; then echo "Report generation failed"; exit 1; fi

# Verify that the resulting report has the correct number of entries in it
REPORT_USERS_CNT=`cat /tmp/storageReport.csv |  grep 'user-[0-9]\+' | wc -l`
if [ "$REPORT_USERS_CNT" -ne "32" ]; then
	echo "Report users count is incorrect."
	exit -1
fi

# Verify that the report is correct, using the CsvChecker tool.
java -jar CsvChecker.jar 0.2 storageReference.csv /tmp/storageReport.csv
if [ "$?" -ne "0" ]; then echo "Report failed values check"; exit 1; fi

