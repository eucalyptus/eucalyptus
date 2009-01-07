#!/bin/bash


echo -n "REGISTERING IMAGE..."
echo "ec2-register -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus ttylinux"
AMIID=`ec2-register -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus ttylinux | awk '{print $2}'`
echo "DONE: got AmiId $AMIID"
echo "--------------------------------------------------------------------------------"
echo

echo "DESCRIBING INSTANCES..."
echo "ec2-describe-instances -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus "
ec2-describe-instances -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus 
echo "DONE."
echo "--------------------------------------------------------------------------------"
echo

echo "RUNNING INSTANCES..."
echo "ec2-run-instances -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus $AMIID -n 4"
ec2-run-instances -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus $AMIID -n 4

echo "DONE."
echo "--------------------------------------------------------------------------------"
echo

echo "DESCRIBING INSTANCES..."
echo "ec2-describe-instances -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus "
ec2-describe-instances -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus 
echo "DONE."
echo "--------------------------------------------------------------------------------"
echo

export TSTR=""
for i in `ec2-describe-instances -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus | grep INSTANCE | awk '{print $2}'`
do
  TSTR="$TSTR $i"
done

echo "TERMINATING INSTANCES: $TSTR"
echo "ec2-terminate-instances -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus $TSTR"
ec2-terminate-instances -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus $TSTR

echo "DESCRIBING INSTANCES..."
echo "ec2-describe-instances -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus "
ec2-describe-instances -U http://angelcrest.cs.ucsb.edu:9090/services/Eucalyptus 
echo "DONE."
echo "--------------------------------------------------------------------------------"
echo

echo "DONE."
