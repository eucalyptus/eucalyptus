EGREP="egrep --color=always"
ip=192.168.51.119
ssh root@$ip /opt/eucalyptus/etc/init.d/eucalyptus-cloud stop
while ! euca-describe-services -T vmwarebroker | ${EGREP} NOTREADY; do sleep 1; done
while ! euca-describe-services -T cluster | ${EGREP} NOTREADY; do sleep 1; done;
ssh root@$ip /opt/eucalyptus/etc/init.d/eucalyptus-cloud start
while ! (euca-describe-services -T vmwarebroker; euca-describe-services -T cluster) | ${EGREP} DISABLED; do sleep 1; date; done
euca-describe-services -T cluster
euca-describe-services -T vmwarebroker
while ! euca-describe-services -T vmwarebroker | ${EGREP} ENABLED; do 
	sleep 1
	euca-describe-services -T cluster
	euca-describe-services -T vmwarebroker
	date 
done
