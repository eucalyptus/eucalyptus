#!/usr/bin/perl

#
# test.pl tests the reporting system, by simulating resource usage and then verifying
# that resultant reports are correct.
#
# This script decomposes the test into a series of phases. Each phase is implemented as a
# separate perl script which this script calls in order. First this calls
# "simulate_usage.pl" which simulates resource usage. Then it calls "check_db.pl" which
# verifies that the values in the database are correct according to the simulated usage.
# Then it calls "check_report.pl" which verifies that generated reports are correct
# according to the simulated usage. Those commands can be run manually and separately
# if desired, however running those commands manually would require setting up images etc
# which this script does automatically.
#
# This script accepts several optional arguments: duration_secs, write_interval, num_users,
# num_users_per_account, num_instances_per_user, and image.
#
# This script has dependencies which can be satisfied by sourcing the "test.sh" file.
#
# (c)2011 Eucalyptus Systems, Inc. All Rights Reserved
# author: tom.werges
#

#
# Parse cmd-line args.
#
my $duration_secs = 400;
my $write_interval = 40;
my $num_users = 1;
my $num_users_per_account = 1;
my $num_instances_per_user = 1;
my $image = "";
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
if ($#ARGV>-1) {
	$image = shift;
}
my $return_code = 0;
print "Using args: duration:$duration_secs write_interval:$write_interval users:$num_users num_users_per_account:$num_users_per_account num_instances_per_user:$num_instances_per_user image:" . (($image eq "") ? "(none)" : $image) . "\n";


#
# Terminate all prior instances which may be running
#
my @running_instances = ();
foreach (split("\n",`euca-describe-instances`)) {
	my @fields = split("\\s+");
	if ($fields[5] eq "running" or $fields[5] eq "pending") {
		push(@running_instances,$fields[1]);
	}
}
print "terminating instances\n";
if ($#running_instances > -1) {
	system("euca-terminate-instances " . join(" ",@running_instances)) and die("Couldn't terminate instances:" . join(" ",@running_instances));
}


#
# TODO: This is silly. We'll bundle an image and pass it in.
#  Where to bundle. simulate_usage.pl? Here?
# Bundle image here.
# Find a suitable image to run this test; if none was specified in the cmd-line args then
#  gather an image using euca-describe-images
#
if ($image eq "") {
	foreach(split("\n",`euca-describe-images`)) {
		my @fields = split("\\s+");
		if ($fields[1] =~ /^emi-.*/) {
			$image = $fields[1];
			last;
		}
	}
}
print "using image:$image\n";


#
# Run simulate_usage, then check results
#
print "Executing:./simulate_usage.pl $num_users $num_users_per_account $num_instances_per_user $duration_secs $write_interval $image\n";
my $output=`./simulate_usage.pl $num_users $num_users_per_account $num_instances_per_user $duration_secs $write_interval $image`;
chomp($output);
print "Found output:[$output]\n";
print "Executing: ./check_db.pl $num_instances_per_user $duration_secs $initrd_file $write_interval $output\n";
$return_code |= (system("./check_db.pl $num_instances_per_user $duration_secs $initrd_file $write_interval $output")/256);
print "Executing: ./check_report.pl admin $write_interval $num_instances_per_user $output\n";
$return_code |= (system("./check_report.pl admin $write_interval $num_instances_per_user $output")/256);

exit($return_code);

