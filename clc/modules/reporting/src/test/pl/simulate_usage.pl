#!/usr/bin/perl

#
# This script simulates usage of instances, storage, and s3. It starts
# instances, allocates storage, and allocates s3 objects, as different
# users, concurrently. It then continues running instances, and it allocates
# additional s3 objects and storage every INTERVAL, until DURATION is reached,
# at which point it terminates every instance it started and stops running.
#
# The purpose of this is to verify that reporting events are sent to the
# reporting system properly when usage occurs.
#
# Usage: simulate_usage.pl interval_secs duration_secs num_instances_per_user kernel_image ramdisk_image (username image)+
# Example: simulate_usage.pl 10 100 2 eki-111 eri-111 usera emi-for-usera userb emi-for-userb ...
#
# This script relies upon many run-time dependencies. Before executing it, you
# must:
#  1. Run a CLC on the local machine.
#  2. Register and run a Walrus.
#  3. For each user you pass in, you must:
#    a. Add the user the the Eucalyptus UI, then approve and confirm him
#    b. Add the user to the OS using /usr/sbin/useradd
#    c. Download credentials and put them in his home dir
#    d. Source eucarc automatically by adding a line to his .bashrc
#    e. Add s3curl.pl to his path by adding a line to his .bashrc
#    f. Bundle, upload, and register the associated image
#  4. Bundle and upload the kernel and ramdisk images
#  5. Run this script as root. This script must run as root because it su's
#       and changes users.
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
# author: tom.werges
#

use strict;
use warnings;

if ($#ARGV+1 < 5) {
	die "Usage: simulate_usage.pl num_instances_per_user interval_secs duration_secs kernel_image ramdisk_image (username image)+\n";
}

my $num_instances_per_user = shift;
my $interval = shift;
my $duration = shift;
my $kernel_image = shift;
my $ramdisk_image = shift;

my @pids = ();

my $user_num = 1;
while ($#ARGV>0) {
	my $user = shift;
	my $image = shift;

	# fork and run each simulation as a separate user
	my $pid = fork();
	if ($pid==0) {
		# We must shell out to change users
		exec("su - $user -c \"./simulate_one_user.pl $num_instances_per_user $interval $duration $user_num $kernel_image $ramdisk_image $image > log\"")
			or die ("couldn't exec");
	}
	push(@pids, $pid);
	print "Started pid:$pid\n";
	$user_num++;
}

print "Done forking.\n";

foreach (@pids) {
	print "Waiting for:$_\n";
	waitpid($_,0);
	if ($? != 0) {
		die("Child exited with error code:$_");
	}
}

print "Done.\n";

