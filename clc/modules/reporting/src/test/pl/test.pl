#!/usr/bin/perl

#
# test.pl
#
# Executes a test of the reporting system which runs instances and allocates storage, then
# then verifies that events were stored and that reports generate correctly.
#
# This test is broken down into multiple stages, which are implemented as different perl sub-scripts
# and which are called from this script. Most functionality is found in the sub-scripts:
# simulate_usage.pl, check_db.pl, and check_reports.pl. This script exists only to satisfy
# prerequisites for those scripts and then call them in sequence, using the output of one as
# arguments for the others. The sub-scripts are full command-line commands and can be run
# individually and separately if desired.
#
# This script optionally takes the following args: duration_secs num_users num_users_per_account
# 					num_instances_per_user image
# You can omit any trailing args and the earlier args specified will still take effect.
#
# This script has dependencies which must be satisfied before it's run. The following things
#   must be in the path: all euca2ools and euca commands, db.sh, and dbPass.sh. Also, the
#   env vars in eucarc must be set.
#
# (c)2011 Eucalyptus Systems, Inc. All Rights Reserved
# author: tom.werges
#

#
# Parse cmd-line args.
#
my $duration_secs = 400;
my $write_interval = 40;
my $storage_usage_mb = 2;
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
	$storage_usage_mb = shift;
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
print "Using args: duration:$duration_secs write_interval:$write_interval storage_usage_mb:$storage_usage_mb users:$num_users num_users_per_account:$num_users_per_account num_instances_per_user:$num_instances_per_user image:" . (($image eq "") ? "(none)" : $image) . "\n";


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
my $output=`./simulate_usage.pl $initrd_file $num_users $num_users_per_account $num_instances_per_user $duration_secs $image`;
chomp($output);
print "Found output:[$output]\n";
system("./check_db.pl $num_instances_per_user $duration_secs $upload_file $write_interval $storage_usage_mb $output");
system("./check_report.pl admin $storage_usage_mb $write_interval $output");

