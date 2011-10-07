#!/usr/bin/perl

#
# This script simulates usage of instances, storage, and s3. It starts
# instances, allocates storage, and allocates s3 objects, as different
# users. It then continues running instances, and it allocates additional s3
# objects and storage every INTERVAL, until DURATION is reached, at which
# point it terminates every instance it started and stops running.
#
# The purpose of this is to verify that reporting events are sent to the
# reporting system properly when usage occurs, as part of a test.
#
# Usage: simulate_usage.pl interval duration s3_url kernel_image ramdisk_image (username/image)+
#
# Example: simulate_usage.pl 100 10000 kernel_image ramdisk_image s3_url usera/imagea userb/imageb userc/imagec
#  This example will simluate usage for 10,000 seconds, creating new s3 objects
#  and storage every 100 seconds for users usera,userb,userc. It will also
#  start three instances as three users: imagea as usera, imageb as userb,
#  and imagec as userc, all using the kernel and ramdisk paths specified,
#  and run them for DURATION. It will do all these things simultaneously.
#
# NOTE!! This script has many external dependencies which must be satisfied
#   for it to run. As follows: 1) A CLC must be running locally; 2) A
#   Walrus must be running; 3) You must have created all the users
#   which you pass in; 4) All credentials must be present; 5) You must have
#   created and uploaded all the images, ramdisk images, and kernel images
#   which you pass in; 6) The s3curl.pl script must be present in this
#   directory; 7) we must have write access to /tmp in order to generate dummy
#   files for upload to s3.
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
# author: tom.werges
#

use strict;
use warnings;


#
# SUBROUTINES
#


# SUB: run_instance
# Takes a an image, a kernel image, and a ramdisk image; returns an instance id
sub run_instance($$$) {
	my ($image,$kernel,$ramdisk) = ($_[0],$_[1],$_[2]);
	print "run_instance image:$image kernel:$kernel ramdisk:$ramdisk\n";
	#$output = `euca-run-instances --kernel $kernel --ramdisk $ramdisk $image` or die("starting instance failed");
	my $time = time();
	return "fakeInstance-$_[0]-$time";
}


# SUB: terminate_instance
# Takes an instance id
sub terminate_instance($) {
	print "terminate_instance:$_[0]\n";
	#system("euca-terminate-instance $_[0]") or die("starting instance failed");
}


# SUB: allocate_storage -- creates an EBS volume
# 
sub allocate_storage() {
	print "  allocate_storage:$_[0]\n";
	#system("euca-create-volume --size 1 --zone myPartition") or die("creating volume failed");
}


# SUB: generate_dummy_file -- Returns a path to a dummy-data file of n kilobytes; creates if necessary
# Takes a size (in KB) of dummy data, and returns a path to the resultant file
sub generate_dummy_file($) {
	my $size=$_[0];
	my $path = "/tmp/dummy-$size-kilobyte.txt";
	my $dummy_data = "foo";
	unless (-e $path) {
		open FILE, ">$path" or die ("couldn't open dummy file for writing");
		for (my $i=0; $i<1024*$size; $i+=length($dummy_data)) {
			print FILE $dummy_data;
		}
		close FILE or die ("couldn't close dummy file");
	}
	return $path;
}


# SUB: allocate_s3 -- allocates and S3 object 
# Takes a username, EC2_ACCESS_KEY, EC2_SECRET_KEY, S3_URL, sizeKb
#    sizeKb is the size of the data to upload to the s3 object
sub allocate_s3($$$$$) {
	print "  allocate_s3:$_[0]\n";
	my $time = time();
	my ($user,$access_key,$secret_key,$url,$sizeKb) = ($_[0],$_[1],$_[2],$_[3],$_[4]);
	my $dummy_data_path = generate_dummy_file($sizeKb);
	#system("./s3curl.pl --id $access_key --key $secret_key --put /dev/null -- -s -v $url/mybucket-$user") or die("creating bucket failed");
	#system("./s3curl.pl --id $access_key --key $secret_key --put $dummy_data_path -- -s -v $url/mybucket/obj-$user-$time") or die("creating s3 obj failed");
}

# SUB: switch_to_user  -- switches to a user
# Takes a username and returns EC2_ACCESS_KEY and EC2_SECRET_KEY
sub switch_to_user($) {
	print " switching to user:$_[0]\n";
	return ("ec2-access-key","ec2-secret-key");
}


#
# MAIN LOGIC
#

if ($#ARGV+1 < 6) {
	print "Usage: simulate_usage.pl interval duration s3_url kernel_image ramdisk_image (username/image)+\n";
}

my $interval = shift;
my $duration = shift;
my $s3_url = shift;
my $kernel_image = shift;
my $ramdisk_image = shift;
my @users = ();
my %user_instance_id_hash = ();

print "interval:$interval duration:$duration s3_url:$s3_url kernel:$kernel_image ramdisk:$ramdisk_image\n";

# Parse users into separate list
foreach my $item (@ARGV) {
	(my $user,my $image)=split("/",$item);
	push(@users,$user);
}


# Run all instances as users, and retain instance ids for termination
foreach my $item (@ARGV) {
	(my $user,my $image)=split("/",$item);
	switch_to_user($user);
	my $instance_id = run_instance($image,$kernel_image,$ramdisk_image);
	$user_instance_id_hash{$user}=$instance_id;
}


# Allocate storage and s3 for each user, every INTERVAL, sleeping between
for (my $i=0; $i < $duration; $i++) {
	print "iter:$i\n";
	my $usernum = 0;
	foreach my $user (@users) {
		my ($ec2_access_key, $ec2_secret_key) = switch_to_user($user);
		allocate_storage();
		allocate_s3($user,$ec2_access_key,$ec2_secret_key,$s3_url,$usernum); # allocate usernum kilobytes; different sizes for each user
		$usernum++;
	}
	sleep $interval;
}


# Terminate all instances
while (my ($user, $instance_id) = each(%user_instance_id_hash)) {
	switch_to_user($user);
	terminate_instance($instance_id);
}

