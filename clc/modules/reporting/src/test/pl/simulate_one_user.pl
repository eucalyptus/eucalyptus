#!/usr/bin/perl

#
# This script starts instances, allocates storage, and allocates s3 objects,
# as one user. It then continues running instances, and it allocates additional s3
# objects and storage every INTERVAL, until DURATION is reached, at which
# point it terminates every instance it started and stops running.
#
# This script is called by test.pl; see test.pl for comprehensive documentation
# of the perl test suite.
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
# author: tom.werges
#

use strict;
use warnings;
require "test_common.pl";

if ($#ARGV+1 < 5) {
	die "Usage: simulate_one_user.pl image_id num_instances instance_type interval duration\n";
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

# SUB: parse_vol_id -- parse the output of euca-create-volume
#       
sub parse_vol_id($) {
	my @fields = split("\\s+", $_[0]);
	return $fields[1];
}

#
# MAIN LOGIC
#

my $image_id = shift;
my $num_instances = shift;
my $instance_type = shift;
my $interval = shift;
my $duration = shift;

my %instance_data = ();  # instance_id => status
my @volumes = (); # volume ids of mounted volumes to which instances are writing
my $access_key = $ENV{"EC2_ACCESS_KEY"};
my $secret_key = $ENV{"EC2_SECRET_KEY"};
my $s3_url = $ENV{"S3_URL"};

print "image_id:$image_id num_instances:$num_instances type:$instance_type interval:$interval duration:$duration s3_url:$s3_url\n";

# Get an availability zone
my $output = `euca-describe-availability-zones` or die("couldn't euca-describe-availability-zones");
my @zones = parse_avail_zones($output);
print "Using zone:$zones[0]\n";

# Run instances
print "euca-run-instances -t $instance_type -n $num_instances $image_id";
$output = `euca-run-instances -t $instance_type -n $num_instances $image_id` or die("starting instance failed");
print "output:$output\n";
%instance_data = parse_instance_ids($output);
foreach (keys %instance_data) {
	print "Started instance id:$_\n";
}
if ($num_instances != keys(%instance_data)) {
	die "Incorrect number of instances started\n";
}

# Sleep to give instances time to start
print "Sleeping for " . startup_sleep_duration() . " secs so instances can start up...\n";
sleep startup_sleep_duration();

# Verify that instances are running, and mount EBS volume for each for subsequent snaps
$output = `euca-describe-instances` or die("couldn't euca-describe-instances");
print "output:$output\n";
my %instances = parse_instance_ids($output);
foreach (keys %instance_data) {
	if ($instances{$_} eq "running") {
		print "Instance $_ is running\n";
		my $vol_size = snap_usage_mb();
		my $vol_id = parse_vol_id(`euca-create-volume --size $vol_size --zone $zones[0]`);
		runcmd("euca-attach-volume -i $_ -d " . vol_device() . " $vol_id");
		push(@volumes, $vol_id);
	} else {
		die ("Instance $_ not running:$instances{$_}");
	}
}
sleep 20; # Give vol time to come up before creating snapshots


my $bucketname = "b-" . rand_str(32);

# Allocate storage and s3 for each user, every INTERVAL, sleeping between
#   Iterations should be as close to the INTERVAL time as possible, so subtract running time
my $upload_file = upload_file();
my $start_time = time(); # All storage start time
my $itime = 0; # Iteration start time
for (my $i=0; (time()-$start_time) < $duration; $i++) {
	$itime = time();
	print "iter:$i\n";
	runcmd("euca-create-volume --size " . storage_usage_mb() . " --zone $zones[0]");
	print "$i: Created volume\n";
	runcmd("./s3curl.pl --id " . $ENV{'EC2_ACCESS_KEY'} . " --key " . $ENV{'EC2_SECRET_KEY'} . " --put /dev/null -- -s -v ". $ENV{'S3_URL'} . "/$bucketname");
	runcmd("./s3curl.pl --id " . $ENV{'EC2_ACCESS_KEY'} . " --key " . $ENV{'EC2_SECRET_KEY'} . " --put random.dat -- -s -v " . $ENV{'S3_URL'} . "/$bucketname/object-$i-a");
	runcmd("./s3curl.pl --id " . $ENV{'EC2_ACCESS_KEY'} . " --key " . $ENV{'EC2_SECRET_KEY'} . " --put random.dat -- -s -v " . $ENV{'S3_URL'} . "/$bucketname/object-$i-b");
	#TODO: grab manifest path from this
	print "$i: Created bucket and objects\n";
	runcmd("euca-create-snapshot " . $volumes[$i % ($#volumes+1)]);
	sleep $interval - (time() - $itime);
}

# Terminate instances
foreach (keys %instance_data) {
	system("euca-terminate-instances $_") and die ("Couldn't terminate instance:$_");
	print "Terminated instance:$_\n";
}

