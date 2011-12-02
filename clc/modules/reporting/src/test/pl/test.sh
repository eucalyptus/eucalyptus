#!/bin/sh

# This script just sets up the environment necessary for test.pl to run.
# This script is specifically written to be run on Kyo's QA setup and
#   has pathnames dependent on that.

. /root/eucarc
. /root/iamrc
export EUCALYPTUS=/opt/eucalyptus
export PATH=$PATH:$EUCALYPTUS/usr/sbin
export PATH=$PATH:/root/euca_builder/eee/devel
./test.pl $@

