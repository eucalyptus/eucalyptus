#!/usr/bin/perl

#
# check_db.pl verifies that the data in the database is correct after running
# the simulate_usage.pl script.
#
# Usage: check_db.pl num_intervals storage_size_mb user+
#
# NOTE: This script assumes that the db.sh script runs and is in the current
#  path.
#
# (c) 2011, Eucalyptus Systems Inc. All Rights Reserved.
# author: Tom Werges
#


use strict;


#
# SUBS
#

sub execute_query($) {
	return split("\n",`db.sh --execute='$_[0]' -D eucalyptus_reporting --skip-column-names`);
}

sub row_cnt($) {
	my @lines = execute_query($_[0]);
	return $#lines+1;
}

sub col_sum($) {
	my $sum = 0;
	foreach (execute_query($_[0])) {
		$sum += $_;
	}
	return $sum;
}

sub col_max($) {
	my $max = 0;
	foreach (execute_query($_[0])) {
		($max = $_) if ($_ > max);
	}
	return $max;
}

#
# MAIN LOGIC
#

my $num_intervals = shift;
my $storage_size_mb = shift;
my $x = 0;


my $usernum = 0;
while ($#ARGV+1 > 0) {
	my $user = shift;

	# Check the number of rows which were created for instance usage
	$x = row_cnt("select from reporting_instance, reporting_user, instance_usage_snapshot where");
	# TODO: what about timestamps? We must verify that those are correct?
	if ($x < $num_intervals-2) {
		die("Row count failed for user:$_, expected:" . $num_intervals-2 . " found:$x");
	} else {
		print "Row count succeeded for user:$_\n";
	}

	# Check that disk io is a sensible value
	$x = col_max("select from reporting_instance, reporting_user, instance_usage_snapshot where");

	# Check that net io is a sensible value
	$x = col_max("select from reporting_instance, reporting_user, instance_usage_snapshot where");


	# Check that s3 bucket cnt is a sensible value

	# Check that s3 object cnt is a sensible value

	# Check that s3 object sizes are sensible values

	# Check that volume cnt is a sensible value

	# Check that volume sizes are sensible values

	$usernum++;
}

print "All tests succeeded.\n";
return 0;

