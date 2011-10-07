#!/usr/bin/perl

#
# This script starts instances, allocates storage, and allocates s3 objects,
# as different users. It continues running instances, and it allocates
# additional s3 objects and storage every INTERVAL, until DURATION is reached,
# at which point it terminates every instance it started and stops running.
#
# The purpose of this is to verify that reporting events are sent to the
# reporting system properly when usage occurs, as part of a test.
#
# Usage: simulate_usage.pl interval duration kernel_file ramdisk_file (username/image_file)+
#
# Example: simulate_usage.pl 100 10000 kernel_file ramdisk_file usera/image_filea userb/image_fileb userc/image_filec
#
# This example will simluate usage for 10,000 seconds, creating new s3 objects
# and storage every 100 seconds for users usera,userb,userc. It will also
# start three instances as three users: image_filea as usera, 
# image_fileb as userb, and image_filec as userc, all using the kernel and
# ramdisk files specified, and run them for DURATION. It will do all these
# things simultaneously. It thereby simulates different users running
# instances and performing various storage operations for a protracted period.
#
# NOTE!! This script has many external dependencies which must be satisfied
#   for it to run. As follows: 1) A CLC must be running locally; 2) A
#   Walrus must be running locally; 3) You must have created all the users
#   which you pass in, and they all must have password "foobar";
#   4) All credentials must be present; 5) You must have created all the
#   image files, ramdisk files, and kernel files which you pass in;
#   6) The s3curl.pl script must be present in this directory.
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
# author: tom.werges
#

use strict;

if ($#ARGV+1 < 2) {
	print "Usage: simulate_usage.pl interval duration kernel_file ramdisk_file (username/image_file)+\n";
}

my $interval = shift;
my $duration = shift;
my @users = ();
my @instance_ids = ();

print "interval:$interval duration:$duration\n";


# Parse users into separate list
foreach my $user (@ARGV) {
	push(@users,$user);
}


# Run all instances
foreach my $item (@ARGV) {
	(my $user,my $image)=split("/",$item);
	my $instance_id = run_instance($user,$image);
	push(@instance_ids,$instance_id);
}


# Allocate storage and s3 for each user, every INTERVAL, sleeping between
for (my $i=0; $i < $duration; $i++) {
	print "iter:$i\n";
	foreach my $user (@users) {
		allocate_storage($user);
		allocate_s3($user);
	}
	sleep $interval;
}


# Terminate all instances
foreach my $instance_id (@instance_ids) {
	terminate_instance($instance_id);
}


# SUB: run_instance -- runs an instance as a user
# Takes a username, an image filename, a kernel filename, and a ramdisk
# 	filename; returns an instance id
sub run_instance($$$$) {
	switch_to_user($_[0]);
	print "run_instance:$_[0]/$_[1]\n";
	system("euca-run-instances") or die("starting instance failed");
}


# SUB: terminate_instance -- terminates an instance as a user
# Takes a list of users
sub terminate_instance($) {
	switch_to_user($_[0]);
	print "terminate_instance:$_[0]\n";
}


# SUB: allocate_storage -- allocates a volume as a user
# Takes a username
sub allocate_storage($) {
	switch_to_user($_[0]);
	print "  allocate_storage:$_[0]\n";
	system("euca-create-volume --size 1 --zone myPartition") or die("creating volume failed");
}


# SUB: allocate_s3 -- allocates and S3 object as a user
# Takes a username
sub allocate_s3($) {
	switch_to_user($_[0]);
	my $time = time();
	print "  allocate_s3:$_[0]\n";
	system("./s3curl.pl --id $EC2_ACCESS_KEY --key $EC2_SECRET_KEY --put /dev/null -- -s -v $S3_URL/mybucket") or die("creating bucket failed");
	system("./s3curl.pl --id $EC2_ACCESS_KEY --key $EC2_SECRET_KEY --put data.txt -- -s -v $S3_URL/mybucket/obj_$time") or die("creating s3 obj failed");
}

# SUB: switch_to_user  -- switches to a user
# Takes a username and returns EC2_ACCESS_KEY and EC2_SECRET_KEY
sub switch_to_user($) {
}

