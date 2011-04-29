#!/bin/sh

#
# Tests report generation for storage data. Returns 0 for success, 1 for failure.
#
# Author: Tom Werges
#


# Login, and get session id
wget -O /tmp/sessionId --no-check-certificate 'https://localhost:8443/loginservlet?adminPw=admin'
export SESSIONID=`cat /tmp/sessionId`
echo "session id:" $SESSIONID

# Clear and generate false data for storages
wget --no-check-certificate -O /tmp/nothing "https://localhost:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.storage.FalseDataGenerator&methodName=removeFalseData"
wget --no-check-certificate -O /tmp/nothing "https://localhost:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.storage.FalseDataGenerator&methodName=generateFalseData"

# Generate storage report, based upon data generated above
wget -O /tmp/storageReport.csv --no-check-certificate "https://localhost:8443/reports?session=$SESSIONID&name=user_storage&type=csv&page=0&flush=false&start=1104566400000&end=1304566400000&criterionId=2&groupById=0"

# Verify that the report is correct, using the CsvChecker tool.
java -jar CsvChecker.jar 0.2 storageReference.csv /tmp/storageReport.csv

