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

sub row_sum($) {
	my $sum = 0;
	foreach (execute_query($_[0])) {
		$sum += $_;
	}
	return $sum;
}

#
# MAIN LOGIC
#

my $num_intervals = shift;
my $storage_size_mb = shift;
my @users = ();

while ($#ARGV+1>0) {
    push(@users, shift);
}



foreach (execute_query("select user_id, user_name from reporting_user")) {
	my @fields = split("\\s+");
	foreach (@fields) {
		print "field: $_ ";
	}
	print "\n";
}

print "row cnt:" . row_cnt("select user_name from reporting_user") . "\n";

