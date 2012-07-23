# Copyright 2009-2012 Eucalyptus Systems, Inc.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
# Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
# CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
# additional information or have any questions.

# This script just sets up the environment necessary for test.pl to run.
# This script is specifically written to be run on Kyo's QA setup and
#   has pathnames dependent on that.

. /root/eucarc
. /root/iamrc
export EUCALYPTUS=/opt/eucalyptus
export PATH=$PATH:$EUCALYPTUS/usr/sbin
export PATH=$PATH:/root/euca_builder/eee/devel
cp /root/euca_builder/eee/devel/db.sh .
cp /root/euca_builder/eee/devel/dbPass.sh .

#Install prerequisites for s3curl, and generate s3curl.pl file with correct endpoint
yum install perl-Digest-HMAC
./fill_template.pl s3curl.template ENDPOINT=`echo $S3_URL|sed 's#http://##'|sed 's/:.*//'` > s3curl.pl
chmod 755 s3curl.pl

# Copy first initrd from /boot to ./random.dat; this is just dummy data to upload to S3
perl -e 'foreach (`ls /boot/initrd*.img`) { chomp; `cp $_ ./random.dat`; break; }'
