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
# Usage: simulate_usage.pl interval duration kernel_path ramdisk_path (username/image_path)+
#
# Example: simulate_usage.pl 100 10000 kernel_path ramdisk_path usera/image_patha userb/image_pathb userc/image_pathc
#
# This example will simluate usage for 10,000 seconds, creating new s3 objects
# and storage every 100 seconds for users usera,userb,userc. It will also
# start three instances as three users: image_patha as usera, 
# image_pathb as userb, and image_pathc as userc, all using the kernel and
# ramdisk paths specified, and run them for DURATION. It will do all these
# things simultaneously.
#
# NOTE!! This script has many external dependencies which must be satisfied
#   for it to run. As follows: 1) A CLC must be running locally; 2) A
#   Walrus must be running locally; 3) You must have created all the users
#   which you pass in; 4) All credentials must be present; 5) You must have
#   created all the image paths, ramdisk paths, and kernel paths which you
#   pass in; 6) The s3curl.pl script must be present in this directory.
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
# Takes a username, an image pathname, a kernel pathname, and a ramdisk
# 	pathname; returns an instance id
sub run_instance($$$$) {
	print "run_instance user:$_[0] image:$_[1] kernel:$_[2] ramdisk:$_[3]\n";
	#system("euca-run-instances") or die("starting instance failed");
	my $time = time();
	return "fakeInstance-$_[0]-$time";
}


# SUB: terminate_instance
# Takes a list of users
sub terminate_instance($) {
	print "terminate_instance:$_[0]\n";
}


# SUB: allocate_storage
# Takes a username
sub allocate_storage($) {
	print "  allocate_storage:$_[0]\n";
	#system("euca-create-volume --size 1 --zone myPartition") or die("creating volume failed");
}


# SUB: allocate_s3 -- allocates and S3 object as a user
# Takes a username
sub allocate_s3($) {
	my $time = time();
	print "  allocate_s3:$_[0]\n";
	#system("./s3curl.pl --id $EC2_ACCESS_KEY --key $EC2_SECRET_KEY --put /dev/null -- -s -v $S3_URL/mybucket") or die("creating bucket failed");
	#system("./s3curl.pl --id $EC2_ACCESS_KEY --key $EC2_SECRET_KEY --put data.txt -- -s -v $S3_URL/mybucket/obj_$time") or die("creating s3 obj failed");
}

# SUB: switch_to_user  -- switches to a user
# Takes a username and returns EC2_ACCESS_KEY and EC2_SECRET_KEY
sub switch_to_user($) {
	print " switching to user:$_[0]\n";
}


#
# MAIN LOGIC
#

if ($#ARGV+1 < 5) {
	print "Usage: simulate_usage.pl interval duration kernel_path ramdisk_path (username/image_path)+\n";
}

my $interval = shift;
my $duration = shift;
my $kernel_path = shift;
my $ramdisk_path = shift;
my @users = ();
my %user_instance_id_hash = ();

print "interval:$interval duration:$duration kernel:$kernel_path ramdisk:$ramdisk_path\n";
# Parse users into separate list
foreach my $item (@ARGV) {
	(my $user,my $image_path)=split("/",$item);
	push(@users,$user);
}


# Run all instances and retain instance ids for termination
foreach my $item (@ARGV) {
	(my $user,my $image_path)=split("/",$item);
	my $instance_id = run_instance($user,$image_path,$kernel_path,$ramdisk_path);
	$user_instance_id_hash{$user}=$instance_id;
}


# Allocate storage and s3 for each user, every INTERVAL, sleeping between
for (my $i=0; $i < $duration; $i++) {
	print "iter:$i\n";
	foreach my $user (@users) {
		switch_to_user($user);
		allocate_storage($user);
		allocate_s3($user);
	}
	sleep $interval;
}


# Terminate all instances
while (my ($user, $instance_id) = each(%user_instance_id_hash)) {
	switch_to_user($user);
	terminate_instance($instance_id);
}


