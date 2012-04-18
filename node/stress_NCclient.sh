CL=./NCclient 
WS=localhost:9090
RU=runInstance
TR=terminateInstance
RE=describeResource
IN=describeInstances

if [ ! -n "$1" ] ; then
	echo "need a registered imageId, e.g.:"
    ls -1 /mayhem/obsidian/euca_stor/registered/eucalyptus
	exit 1
fi

$CL $WS $RU i1 $1 aa:dd:00:11:22:00
$CL $WS $IN
$CL $WS $RE
$CL $WS $TR i1
$CL $WS $IN
$CL $WS $RE
echo "pausing..."
echo
sleep 2

$CL $WS $RU i2 $1 aa:dd:00:11:22:01
$CL $WS $RU i3 $1 aa:dd:00:11:22:02
$CL $WS $IN
$CL $WS $RE
$CL $WS $TR i2
$CL $WS $TR i3
$CL $WS $IN
$CL $WS $RE
echo "pausing..."
echo
sleep 2

$CL $WS $RU i4 $1 aa:dd:00:11:22:03
$CL $WS $RU i5 $1 aa:dd:00:11:22:04
$CL $WS $RU i6 $1 aa:dd:00:11:22:05
$CL $WS $RU i7 $1 aa:dd:00:11:22:06
$CL $WS $RU i8 $1 aa:dd:00:11:22:07
$CL $WS $RU i9 $1 aa:dd:00:11:22:08
$CL $WS $IN
$CL $WS $RE
echo "press ENTER to continue..."
read any_key

$CL $WS $TR i4
$CL $WS $TR i5
$CL $WS $TR i6
$CL $WS $TR i7
$CL $WS $TR i8
$CL $WS $TR i9
$CL $WS $IN
$CL $WS $RE


