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
# Usage: simulate_usage.pl interval duration (username/image_file)+
#
# Example: simulate_usage.pl 100 10000 usera/image_filea userb/image_fileb userc/image_filec
#
# This example will simluate usage for 10,000 seconds, creating new s3 objects
# and storage every 100 seconds for users usera,userb,userc. It will also
# start three instances as three users: image_filea as usera, 
# image_fileb as userb, and image_filec as userc, and run them for DURATION.
# It will do all these things simultaneously. It thereby simulates different
# users running instances and performing various storage operations for
# a protracted period.
#
# NOTE!! this script assumes that you have already created the users you pass
# and that they all have the password "foobar". Also, you must have created
# your own image files. Ideally, you will have created images that
# automatically use network I/O and disk I/O repeatedly, thereby testing
# those reporting features as well, since network and disk I/O are statistics
# which are tracked by reporting.
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
# author: tom.werges
#

use strict;

if ($#ARGV+1 < 2) {
	print "Usage: simulate_usage.pl interval duration (username/image_file)+\n";
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


# SUB: run_instance
# Takes a username and an image filename; returns an instance id
sub run_instance($$) {
	print "run_instance:$_[0]/$_[1]\n";
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
}


# SUB: allocate_s3
# Takes a username
sub allocate_s3($) {
	print "  allocate_s3:$_[0]\n";
}

