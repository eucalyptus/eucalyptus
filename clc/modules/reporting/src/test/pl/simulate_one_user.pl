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
#   must have been created, accepted, and confirmed.
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
# author: tom.werges
#

use strict;
use warnings;

if ($#ARGV+1 < 6) {
	die "Usage: simulate_one_user.pl num_instances interval duration storage_usage_mb kernel_image ramdisk_image image\n";
}

# SUB: generate_dummy_file -- Returns a path to a dummy-data file of n megabytes; creates if necessary
# Takes a size (in MB) of dummy data, and returns a path to the resultant file
sub generate_dummy_file($) {
	my $size=$_[0];
	my $time = time();
	my $path = "dummy-$size-megabyte-$time.txt";
	my $dummy_data = "f00d";
	unless (-e $path) {
		open FILE, ">$path" or die ("couldn't open dummy file for writing");
		for (my $i=0; $i < $size<<20; $i+=length($dummy_data)) {
			print FILE $dummy_data;
		}
		close FILE or die ("couldn't close dummy file");
	}
	sleep 2;
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
	return %instances;
}

# SUB: parse_avail_zones -- parse the output of euca-describe-availability_zones
#        and return a list of availability zones
sub parse_avail_zones($) {
	my @zones = ();
	foreach (split("\n", $_[0])) {
		my @fields = split("\\s+");
		if ($fields[0] =~ /^AVAILABILITYZONE/) {
			push(@zones,$fields[1]);
		}
	}
	return @zones;
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
my $user = $ENV{"USER"};

print "num_instances:$num_instances interval:$interval duration:$duration kernel:$kernel_image ramdisk:$ramdisk_image s3_url:$s3_url image:$image\n";

# Run instances
my $output = `euca-run-instances -n $num_instances --kernel $kernel_image --ramdisk $ramdisk_image $image` or die("starting instance failed");
%instance_data = parse_instance_ids($output);
foreach (keys %instance_data) {
	print "Started instance id:$_\n";
}
if ($num_instances != keys(%instance_data)) {
	die "Incorrect number of instances started\n";
}

# Sleep to give instances time to start
my $sleep_duration = 120;
print "Sleeping for $sleep_duration secs so instances can start up...\n";
sleep $sleep_duration;

# Verify that instances are running
$output = `euca-describe-instances` or die("couldn't euca-describe-instances");
my %instances = parse_instance_ids($output);
foreach (keys %instance_data) {
	if ($instances{$_} eq "running") {
		print "Instance $_ is running\n";
	} else {
		die ("Instance $_ not running:$instances{$_}");
	}
}

# Get an availability zone
$output = `euca-describe-availability-zones` or die("couldn't euca-describe-availability-zones");
my @zones = parse_avail_zones($output);
print "Using zone:$zones[0]\n";


# Allocate storage and s3 for each user, every INTERVAL, sleeping between
for (my $i=0; ($i*$interval) < $duration; $i++) {
	print "iter:$i\n";
	system("euca-create-volume --size $storage_usage_mb --zone $zones[0]") and die("creating volume failed");
	print "$i: Created volume\n";
	my $time = time();
	my $dummy_data_path = generate_dummy_file($storage_usage_mb);
	print "$i: Created bucket\n";
	system("euca-bundle-image -i $dummy_data_path");
	system("euca-upload-bundle -b mybucket -m /tmp/$dummy_data_path.manifest.xml");
	print "$i: Uploaded bundle\n";
	sleep $interval;
}

# Terminate instances
foreach (keys %instance_data) {
	system("euca-terminate-instances $_") and die ("Couldn't terminate instance:$_");
	print "Terminated instance:$_\n";
}

# Clean up dummy files
system("rm -f dummy-*-megabyte*") or die("Couldn't clean up files");
