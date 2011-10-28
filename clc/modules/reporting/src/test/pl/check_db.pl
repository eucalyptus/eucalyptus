#!/usr/bin/perl

#
# check_db.pl verifies that the data in the database is correct after running
# the simulate_usage.pl script.
#
# Usage: check_db.pl (username/accountname)+
#
# NOTE: This script assumes that the db.sh script runs and is in the current
#  path.
#
# (c) 2011, Eucalyptus Systems Inc. All Rights Reserved.
# author: Tom Werges
#


use strict;

sub execute_query($) {
	return split("\n",`db.sh --execute="$_[0]" -D eucalyptus_reporting --skip-column-names`);
}

#
# MAIN LOGIC
#

my @usernames = ();
my @accountnames = ();

foreach (@ARGV) {
	my ($username, $accountname) = split("/");
	push(@usernames, $username);
	push(@accountnames, $accountname);
}

my $username_csv = "'" . join("','",@usernames) . "'";
my $accountname_csv = "'" . join("','",@accountnames) . "'";

my $instance_num_rows = 0;
my $s3_num_rows = 0;
my $storage_num_rows = 0;


foreach (execute_query("
	select
	  ius.total_disk_io_megs,
	  ius.total_network_io_megs
	from
	  instance_usage_snapshot ius,
	  reporting_instance ri,
	  reporting_user ru,
	  reporting_account ra
	where
	  ius.uuid = ri.uuid
	and ri.user_id = ru.user_id
	and ri.account_id = ra.account_id
	and ru.user_name in ($username_csv)
	and ra.account_name in ($accountname_csv)
")) {
	my ($disk_io,$net_io) = split("\\s+");
	print "disk_io:$disk_io net_io:$net_io\n";
	$instance_num_rows++;
}


foreach (execute_query("
	select
	  s3s.buckets_num,
	  s3s.objects_num,
	  s3s.objects_megs
	from
	  s3_usage_snapshot s3s,
	  reporting_user ru,
	  reporting_account ra
	where
	  s3s.owner_id = ru.user_id
	and s3s.account_id = ra.account_id
	and ru.user_name in ($username_csv)
	and ra.account_name in ($accountname_csv)
")) {
	my ($buckets_num,$objects_num,$objects_megs) = split("\\s+");
	print "buckets_num:$buckets_num objects_num:$objects_num objects_megs:$objects_megs\n";
	$s3_num_rows++;
}


foreach (execute_query("
	select
	  sus.snapshot_num,
	  sus.snapshot_megs,
	  sus.volumes_num,
	  sus.volumes_megs
	from
	  storage_usage_snapshot sus,
	  reporting_user ru,
	  reporting_account ra
	where
	  sus.owner_id = ru.user_id
	and sus.account_id = ra.account_id
	and ru.user_name in ($username_csv)
	and ra.account_name in ($accountname_csv)
")) {
	my ($snaps_num, $snaps_megs, $vols_num, $vols_megs) = split("\\s+");
	print "snaps_num:$snaps_num snaps_megs:$snaps_megs vols_num:$vols_num vols_megs:$vols_megs\n";
	$storage_num_rows++;
}


# Verify num of rows for ius, s3s, sus
# Verify maxes for s3s, sus
# What about instance?


