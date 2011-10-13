#!/usr/bin/perl

#
# This script starts instances, allocates storage, and allocates s3 objects,
# as one user. It then continues running instances, and it allocates additional s3
# objects and storage every INTERVAL, until DURATION is reached, at which
# point it terminates every instance it started and stops running.
#
# Usage: simulate_one_user.pl num_instances interval duration storage_usage_mb kernel_image ramdisk_image image
#
# Example: simulate_one_user.pl 2 100 10000 kernel_image ramdisk_image image
#  This example will start 2 instances, then run for 10,000 seconds,
#  allocating storage and S3 every 100 seconds, after which it will terminate
#  the instances it started and quit.
#
# NOTE!! This script has many external dependencies which must be satisfied
#   for it to run. As follows: 1) A CLC must be running locally; 2) A Walrus
#   must be running; 3) You must have created and uploaded all the images,
#   ramdisk images, and kernel images which you pass in; 4) eucarc must
#   be sourced; 5) A Eucalyptus user corresponding to the credentials
#   must have been created, accepted, and confirmed; 6) s3curl.pl must be in
#   the user's path.
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
# author: tom.werges
#

use strict;
use warnings;

if ($#ARGV+1 < 6) {
	print "Usage: simulate_one_user.pl num_instances interval duration storage_usage_mb kernel_image ramdisk_image image\n";
}

open LOG, ">~/log.out" or die ("Couldn't open log file");

# SUB: generate_dummy_file -- Returns a path to a dummy-data file of n megabytes; creates if necessary
# Takes a size (in MB) of dummy data, and returns a path to the resultant file
sub generate_dummy_file($) {
	my $size=$_[0];
	my $path = "~/dummy-$size-megabyte.txt";
	my $dummy_data = "f00d";
	unless (-e $path) {
		open FILE, ">$path" or die ("couldn't open dummy file for writing");
		for (my $i=0; $i<<20*$size; $i+=length($dummy_data)) {
			print FILE $dummy_data;
		}
		close FILE or die ("couldn't close dummy file");
	}
	return $path;
}

# SUB: parse_instance_ids -- parse the output of euca-run-instances or euca-describe-instances
#        and return a hash of instance_id => status
sub parse_instance_ids($) {
	my %instances = ();
	foreach (split("\n", $_[0])) {
		my @fields = split("\\s+");
		if ($fields[0] =~ /^INSTANCE/) {
			$instances{$fields[1]}=$fields[5];
		}
	}
	return $instances;
}


#
# MAIN LOGIC
#

my $num_instances = shift;
my $interval = shift;
my $duration = shift;
my $storage_usage_mb = shift;
my $kernel_image = shift;
my $ramdisk_image = shift;
my $image = shift;

my %instance_data = ();  # instance_id => status
my $access_key = $ENV{"EC2_ACCESS_KEY"};
my $secret_key = $ENV{"EC2_SECRET_KEY"};
my $s3_url = $ENV{"S3_URL"};

print LOG "num_instances:$num_instances interval:$interval duration:$duration kernel:$kernel_image ramdisk:$ramdisk_image s3_url:$s3_url image:$image\n";

# Run instances
my $output = `euca-run-instances -n $num_instances --kernel $kernel --ramdisk $ramdisk $image` or die("starting instance failed");
%instance_data = parse_instance_ids($output);
foreach (keys %instance_data) {
	print LOG "Ran instance id:$_\n";
}

# Sleep for 30 secs to give instances time to start
print "Sleeping for 30 secs...\n";
sleep 30

# Verify that instances are running
$output = `euca-describe-instances` or die("couldn't euca-describe-instances");
my %instances = parse_instance_ids($output);
foreach (keys %instance_data) {
	if ($instances{$_} eq "running") {
		print LOG "Instance $_ still running\n";
	} else {
		die ("Instance $_ not running:$instances{$_}");
	}
}

# Allocate storage and s3 for each user, every INTERVAL, sleeping between
for (my $i=0; $i < $duration; $i++) {
	print "iter:$i\n";
	system("euca-create-volume --size $storage_usage_mb --zone myPartition") or die("creating volume failed");
	my $time = time();
	my $dummy_data_path = generate_dummy_file($storage_usage_mb);
	system("s3curl.pl --id $access_key --key $secret_key --put /dev/null -- -s -v $url/mybucket-$user") or die("creating bucket failed");
	system("s3curl.pl --id $access_key --key $secret_key --put $dummy_data_path -- -s -v $url/mybucket/obj-$user-$time") or die("creating s3 obj failed");
	sleep $interval;
}


# Terminate instances
foreach my $instance_id (@instance_ids) {
	system("euca-terminate-instances $instance_id") or die ("Couldn't terminate instance");
	print LOG "Terminated instance:$instance_id\n";
}

close LOG or die ("couldn't close log");

