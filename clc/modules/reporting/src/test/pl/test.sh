
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
