#!/usr/bin/perl

#
# test.pl tests the reporting system, by simulating resource usage and then verifying
# that results are correct.
#
# This script delegates much of its functionality to other perl scripts which it calls.
# It calls "simulate_usage.pl" which simulates resource usage. Then it calls "check_db.pl"
# which verifies that the values in the database are correct according to the simulated
# usage. Then it calls "check_report.pl" which verifies that generated reports are correct
# according to the simulated usage. Those commands can be called manually and separately
# if desired; however running those commands manually will require setting up images
# etc which this script does automatically.
#
# This script accepts several optional arguments: duration_secs, write_interval, num_users,
# num_users_per_account, num_instances_per_user, and image.
#
# This script has dependencies which must be satisfied for it to run. Those dependencies
# can be satisfied by first sourcing the "test.sh" file.
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
print "Using args: duration:$duration_secs write_interval:$write_interval users:$num_users num_users_per_account:$num_users_per_account num_instances_per_user:$num_instances_per_user image:" . (($image eq "") ? "(none)" : $image) . "\n";


#
# Gather a single initrd file from /boot and copy it here.
# This file is used only for the purpose of having something of known size
# which is uncompressible and which we can upload to S3 and ebs.
#
my $initrd_file = "";
opendir(BOOTDIR, "/boot") or die("couldn't open /boot");
while (my $file = readdir(BOOTDIR)) {
	if ($file =~ /initrd-.*img/) {
		$initrd_file = $file;
		last;
	}
}
closedir(BOOTDIR) or die("couldn't close /boot");
print "found file:$initrd_file\n";
system("cp /boot/$initrd_file .") and die("couldn't copy /boot/$initrd_file to .");


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
print "Executing:./simulate_usage.pl $initrd_file $num_users $num_users_per_account $num_instances_per_user $duration_secs $write_interval $image\n";
my $output=`./simulate_usage.pl $initrd_file $num_users $num_users_per_account $num_instances_per_user $duration_secs $write_interval $image`;
chomp($output);
print "Found output:[$output]\n";
print "Executing: ./check_db.pl $num_instances_per_user $duration_secs $initrd_file $write_interval $output\n";
system("./check_db.pl $num_instances_per_user $duration_secs $initrd_file $write_interval $output");
print "Executing: ./check_report.pl admin $write_interval $num_instances_per_user $output\n";
system("./check_report.pl admin $write_interval $num_instances_per_user $output");

