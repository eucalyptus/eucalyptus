#!/bin/bash
#
# This script, called before starting an instance, serves two purposes:
# 1) configures the superblock to ignore timestamps and mount counts
# 2) if provided, adds a public key to the root authorized_keys

# binaries we are going to need
MOUNT=`which mount`
UMOUNT=`which umount`
MKDIR=`which mkdir`
RMDIR=`which rmdir`
CAT=`which cat`
CHOWN=`which chown`
CHMOD=`which chmod`
RM=`which rm`
MKTEMP=`which mktemp`

# the second parameter, key, is optional
usage() {
	echo
	echo "Usage: $0 xen_disk_image [ssh_public_key]"
	echo
}

if [ -z "$MOUNT" -o -z "$UMOUNT" -o -z "$MKDIR" -o -z "$RMDIR" -o -z "$CAT" -o -z "$CHOWN" -o -z "$CHMOD" -o -z "$RM" ]
then
	echo "Cannot find/execute needed binaries!"
	exit 3
fi

# some quick check
if [ "$#" -lt 1 -o "$#" -gt 2 ]
then
    usage
    exit 1
fi

if [ ! -r "$1" ]
then
	echo "Cannot read image file!"
	exit 2
fi

# tell fsck to ignore timestamps and mount counts
# it is OK for this to fail (in case the file system 
# is not ext2 or ext3) - we do our best
tune2fs -c 0 -i 0 $1 >/dev/null

if [ "$#" -ne 2 ]
then
	exit 0
fi

if [ ! -r "$2" ]
then
	echo "Cannot read key file!"
	exit 2
fi

# let's create the temporary directory
if [ -n "$MKTEMP" ]; then
	TMPFILE="`$MKTEMP -d`" || TMPFILE="/tmp/euca-tmp-$$"
else
	TMPFILE="/tmp/euca-tmp-$$"
fi

# creating temporary mount directory (if it's not there already)
if [ ! -d "$TMPFILE" ]; then
	if ! ${MKDIR} ${TMPFILE} ; then
		echo "Cannot create temp directory!"
		exit 4
	fi
fi

# we will loop for a few tries
ATTACHED=0
for attempts in 1 2 3 4 5 6 7 8 9 10
do
  # try mounting image on loop
  if ${MOUNT} -o loop $1 ${TMPFILE} 2> /dev/null
  then
      ATTACHED=1
      break
  fi

  sleep 1
done

if [ $ATTACHED -eq 0 ]
then
    echo "Cannot mount image on loop device!"
    ${RMDIR} ${TMPFILE}
    exit 5
fi

# add key
if [ ! -d ${TMPFILE}/root/.ssh ]
then
	if ! ${MKDIR} ${TMPFILE}/root/.ssh
	then
		echo "Cannot create .ssh directory!"
		${UMOUNT} ${TMPFILE}
		${RMDIR} ${TMPFILE}
		exit 6
	fi
	${CHOWN} root ${TMPFILE}/root/.ssh
	${CHMOD} 700 ${TMPFILE}/root/.ssh
fi
if ! ${CAT} $2 >> ${TMPFILE}/root/.ssh/authorized_keys
then
	echo "Cannot add key!"
	${UMOUNT} ${TMPFILE}
	${RMDIR} ${TMPFILE}
	exit 7
fi

# now let's be sure the file has the right permission/ownership: sometime
# ssh is pretty picky
${CHOWN} root ${TMPFILE}/root/.ssh/authorized_keys
${CHMOD} 600 ${TMPFILE}/root/.ssh/authorized_keys

# done
${UMOUNT} ${TMPFILE}
${RMDIR} ${TMPFILE}
