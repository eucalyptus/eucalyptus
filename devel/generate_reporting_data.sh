#!/bin/sh 

#
# Script to generate fake reporting data for testing of reporting.
#
# Usage: generate_reporting_data adminPassword (create|delete) (instance|storage|s3)+
#
# This calls the FalseDataGenerator classes in a running Eucalyptus instance,
#  by using the CommandServlet. The FalseDataGenerator classes then generate
#  fake reporting data for testing. None of this is deployed in non-test
#  Eucalyptus installations.
#
# NOTE: You must first deploy the test classes by stopping Euca, then:
#   "cd $SRC/clc; ant build build-test install", then starting Euca again.
#
# (c)2011 Eucalyptus Systems, inc. All rights reserved.
# author: tom.werges
#


# Verify number of params, and display usage if wrong number

if [ "$#" -lt "3" ]
then
	echo "Usage: generate_false_data adminPassword (create|delete) (instance|storage|s3)+"
	exit 1
fi


# Parse password and command params, and get method name

password=$1
command=$2
shift 2

case "$command" in
	"create" )
		method="generateFalseData"
	;;
	"delete" )
		method="removeFalseData"
	;;
	* )
		echo "No such command:$command"
		echo "Usage: generate_false_data adminPassword (create|delete) (instance|storage|s3)+"
		exit 1
esac


# Login using LoginServlet

wget -O /tmp/sessionId --no-check-certificate "https://localhost:8443/loginservlet?adminPw=$password"
if [ "$?" -ne "0" ]
then
	echo "Login failed"
	exit 1
fi
export session=`cat /tmp/sessionId`
echo "session id:" $session


# Parse type params and execute commands for instance, storage, and/or s3

for xx in ${@}
do
	case "$xx" in
		"instance" )
			class="com.eucalyptus.reporting.instance.FalseDataGenerator"
		;;
		"storage" )
			class="com.eucalyptus.reporting.storage.FalseDataGenerator"
		;;
		"s3" )
			class="com.eucalyptus.reporting.s3.FalseDataGenerator"
		;;
	esac
	wget --no-check-certificate -O /tmp/nothing "https://localhost:8443/commandservlet?sessionId=$session&className=$class&methodName=$method"
	if [ "$?" -ne "0" ]
	then
		echo "Command failed; session:$session class:$class method:$method"
		exit 1
	 fi
done

