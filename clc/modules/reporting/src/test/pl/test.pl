#!/usr/bin/perl

#
# test.pl tests the reporting system, by simulating resource usage and then
# verifying that resultant reports are correct.
#
# Test.pl decomposes the test into a series of phases. Each phase is
# implemented as a separate perl script which this script calls in order. First
# it calls inject.pl to inject a script into the startup sequence of an image
# it will use for testing. Then it calls "simulate_usage.pl" which simulates
# resource usage, using the injected image. Then it calls "check_db.pl" which
# verifies that the values in the database are correct according to the
# simulated usage. Then it calls "check_report.pl" which verifies that
# generated reports are correct according to the simulated usage. Those
# commands can be run manually and separately if desired, however running those
# commands manually would require setting up images etc which this script does
# automatically.
#
# This script accepts several optional arguments: duration_secs,
# write_interval, num_users, num_users_per_account, and num_instances_per_user. 
#
# This script has dependencies which can be satisfied by sourcing the "test.sh"
# file.
#
# (c)2011 Eucalyptus Systems, Inc. All Rights Reserved
# author: tom.werges
#
use strict;
use warnings;
require "test_common.pl";

#
# Parse cmd-line args.
#
my $duration_secs = 400;
my $write_interval = 40;
my $num_users = 1;
my $num_users_per_account = 1;
my $num_instances_per_user = 1;
if ($#ARGV>-1) {
	$duration_secs = shift;
}
if ($#ARGV>-1) {
	$write_interval = shift;
}
if ($#ARGV>-1) {
	$num_users = shift;
}
if ($#ARGV>-1) {
	$num_users_per_account = shift;
}
if ($#ARGV>-1) {
	$num_instances_per_user = shift;
}
my $return_code = 0;
print "Using args: duration:$duration_secs write_interval:$write_interval users:$num_users num_users_per_account:$num_users_per_account num_instances_per_user:$num_instances_per_user\n";


#
# Terminate all prior instances which may be running
#
my @running_instances = ();
my @fields=();
foreach (split("\n",`euca-describe-instances`)) {
	@fields = split("\\s+");
	if ($#fields>4 and ($fields[5] eq "running" or $fields[5] eq "pending")) {
		push(@running_instances,$fields[1]);
	}
}
print "terminating instances\n";
if ($#running_instances > -1) {
	system("euca-terminate-instances " . join(" ",@running_instances)) and die("Couldn't terminate instances:" . join(" ",@running_instances));
}


#
# Inject a perl script into the image, then bundle and register the image. The perl script is injected in such a way
#  that it will run automatically when the image boots. The perl script is generated to use
#  resources based upon arguments passed to this script. The purpose is to generate false
#  resource usage from within an image (disk, net, etc) when the test runs.
#
my $injected_image_file = injected_image_file();
printf("./fill_template.pl use_resources.template INTERVAL=%s IO_MEGS=%d DEVICE=%s SLEEP_DURATION=%s > use_resources.pl\n", $write_interval, storage_usage_mb(), vol_device(), startup_sleep_duration());
system(sprintf("./fill_template.pl use_resources.template INTERVAL=%s IO_MEGS=%d DEVICE=%s SLEEP_DURATION=%s > use_resources.pl", $write_interval, storage_usage_mb(), vol_device(), startup_sleep_duration()));
runcmd(sprintf("cp %s %s", image_file(), $injected_image_file));
runcmd(sprintf("./inject.pl %s use_resources.pl", $injected_image_file));
runcmd(sprintf("euca-bundle-image -i %s", $injected_image_file));
runcmd(sprintf("euca-upload-bundle -b injectedimage -m /tmp/%s.manifest.xml", $injected_image_file));
my $output = `euca-register injectedimage/$injected_image_file.manifest.xml`;
@fields = split("\\s+", $output);
my $image_id = $fields[1];
print "IMAGE ID:" . $image_id . "\n";


#
# Run simulate_usage, then check results
#
my $initrd_file = upload_file();
print "Executing:./simulate_usage.pl $image_id $num_users $num_users_per_account $num_instances_per_user $duration_secs $write_interval\n";
$output=`./simulate_usage.pl $image_id $num_users $num_users_per_account $num_instances_per_user $duration_secs $write_interval`;
chomp($output);
print "Found output:[$output]\n";
print "Executing: ./check_db.pl $num_instances_per_user $duration_secs $initrd_file $write_interval $output\n";
$return_code |= (system("./check_db.pl $num_instances_per_user $duration_secs $initrd_file $write_interval $output")/256);
print "Executing: ./check_report.pl admin $write_interval $num_instances_per_user $output\n";
$return_code |= (system("./check_report.pl admin $write_interval $num_instances_per_user $output")/256);

exit($return_code);

