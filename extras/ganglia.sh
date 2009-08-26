#!/bin/sh

TYPE="node"
NC_STAT="/var/run/eucalyptus/nc-stat"
SC_STAT="/var/run/eucalyptus/sc-stat"
WALRUS_STAT="/var/run/eucalyptus/walrus-stat"
GMETRIC="`which gmetric 2> /dev/null`"
EUCALYPTUS="/"

usage () {

	echo "$0 [options]"
	echo
	echo "   -help                       this message"
	echo "   -type [nc|walrus|sc]       which stat file to look for"
	echo "   -d <eucalyptus_dir>         where eucalyptus is installed"
	echo
}

# let's parse the command line
while [ $# -gt 0 ]; do
	if [ "$1" = "-h" -o "$1" = "-help" -o "$1" = "?" -o "$1" =
"--help" ]; then
		usage
		exit 0
	fi

	if [ "$1" = "-type" ]; then
		if [ -z "$2" ]; then
			echo "Need the type!"
			exit 1
		fi
		TYPE="$1"
		shift
		continue
	fi

	if [ "$1" = "-d" ]; then
		if [ -z "$2" ]; then
			echo "Need eucalyptus directory!"
			exit 1
		fi
		EUCALYPTUS="$1"
		shift
		continue
	fi
	usage
	exit 1
done

# some checks
if [ -z "$GMETRIC" ]; then
	echo "Cannot find gmetric: do you have ganglia installed?"
	exit 1
fi
if [ ! -e "$EUCALYPTUS/etc/eucalyptus/eucalyptus.conf" ]; then
	echo "Is Eucalyptus installed in $EUCALYPTUS?"
	exit 1
fi
if [ "$TYPE" = "nc" ]; then
	# let's check we have the stat file
	if [ ! -e ${EUCALYPTUS}${NC_STAT} ]; then
		echo "Cannot find NC stat file!"
		exit 1
	fi
elif [ "$TYPE" = "sc" ]; then
	# let's check we have the stat file
	if [ ! -e ${EUCALYPTUS}${SC_STAT} ]; then
		echo "Cannot find SC stat file!"
		exit 1
	fi
elif [ "$TYPE" = "walrus" ]; then
	# let's check we have the stat file
	if [ ! -e ${EUCALYPTUS}${WALRUS_STAT} ]; then
		echo "Cannot find wlarus stat file!"
		exit 1
	fi
else
	echo "Unknown type!"
	exit 1
fi

