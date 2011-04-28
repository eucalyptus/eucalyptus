#!/bin/sh

#
# Tests report generation for instance data. Returns 0 for success, 1 for failure.
#
# Author: Tom Werges
#


# Login, and get session id
wget -O /tmp/sessionId --no-check-certificate 'https://localhost:8443/loginservlet?adminPw=dogfyd'
export SESSIONID=`cat /tmp/sessionId`
echo "session id:" $SESSIONID

# Clear and generate false data for instances
wget --no-check-certificate "https://localhost:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=removeFalseData"
wget --no-check-certificate "https://localhost:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=generateFalseData"

# Generate instance report, based upon data generated above
wget -O /tmp/report.csv --no-check-certificate "https://localhost:8443/reports?session=$SESSIONID&name=user_vms&type=csv&page=0&flush=false&start=1302915187213&end=1303519987213&criterionId=2&groupById=0"

# Compile report verification program; this program reads through the report generated above,
#   then compares the data it finds with expected values
#javac TestInstanceReport.java

# Verify that the report is correct
#return java -cp . TestInstanceReport /tmp/report.csv

