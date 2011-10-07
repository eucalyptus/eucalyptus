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
# Usage: simulate_usage.pl interval duration s3_url kernel_path ramdisk_path (username/image_path)+
#
# Example: simulate_usage.pl 100 10000 kernel_path ramdisk_path s3_url usera/image_patha userb/image_pathb userc/image_pathc
#  This example will simluate usage for 10,000 seconds, creating new s3 objects
#  and storage every 100 seconds for users usera,userb,userc. It will also
#  start three instances as three users: image_patha as usera, 
#  image_pathb as userb, and image_pathc as userc, all using the kernel and
#  ramdisk paths specified, and run them for DURATION. It will do all these
#  things simultaneously.
#
# NOTE!! This script has many external dependencies which must be satisfied
#   for it to run. As follows: 1) A CLC must be running locally; 2) A
#   Walrus must be running; 3) You must have created all the users
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
# Takes a an image path, a kernel path, and a ramdisk path; returns an instance id
sub run_instance($$$) {
	print "run_instance image:$_[1] kernel:$_[2] ramdisk:$_[3]\n";
	#system("euca-run-instances") or die("starting instance failed");
	my $time = time();
	return "fakeInstance-$_[0]-$time";
}


# SUB: terminate_instance
# Takes an instance id
sub terminate_instance($) {
	print "terminate_instance:$_[0]\n";
	#system("euca-terminate-instance $_[0]") or die("starting instance failed");
}


# SUB: allocate_storage
# 
sub allocate_storage() {
	print "  allocate_storage:$_[0]\n";
	#system("euca-create-volume --size 1 --zone myPartition") or die("creating volume failed");
}


# SUB: allocate_s3 -- allocates and S3 object as a user
# Takes a username, EC2_ACCESS_KEY, EC2_SECRET_KEY, S3_URL
sub allocate_s3($$$$) {
	my $time = time();
	my ($user,$access_key,$secret_key,$url) = ($_[0],$_[1],$_[2],$_[3]);
	print "  allocate_s3:$_[0]\n";
	#system("./s3curl.pl --id $access_key --key $secret_key --put /dev/null -- -s -v $url/mybucket") or die("creating bucket failed");
	#system("./s3curl.pl --id $access_key --key $secret_key --put data.txt -- -s -v $url/mybucket/obj-$user-$time") or die("creating s3 obj failed");
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
	print "Usage: simulate_usage.pl interval duration s3_url kernel_path ramdisk_path (username/image_path)+\n";
}

my $interval = shift;
my $duration = shift;
my $s3_url = shift;
my $kernel_path = shift;
my $ramdisk_path = shift;
my @users = ();
my %user_instance_id_hash = ();

print "interval:$interval duration:$duration s3_url:$s3_url kernel:$kernel_path ramdisk:$ramdisk_path\n";
# Parse users into separate list
foreach my $item (@ARGV) {
	(my $user,my $image_path)=split("/",$item);
	push(@users,$user);
}


# Run all instances as users and retain instance ids for termination
foreach my $item (@ARGV) {
	(my $user,my $image_path)=split("/",$item);
	switch_to_user($user);
	my $instance_id = run_instance($image_path,$kernel_path,$ramdisk_path);
	$user_instance_id_hash{$user}=$instance_id;
}


# Allocate storage and s3 for each user, every INTERVAL, sleeping between
for (my $i=0; $i < $duration; $i++) {
	print "iter:$i\n";
	foreach my $user (@users) {
		my ($ec2_access_key, $ec2_secret_key) = switch_to_user($user);
		allocate_storage();
		allocate_s3($user,$ec2_access_key,$ec2_secret_key,$s3_url);
	}
	sleep $interval;
}


# Terminate all instances
while (my ($user, $instance_id) = each(%user_instance_id_hash)) {
	switch_to_user($user);
	terminate_instance($instance_id);
}

