#!/usr/bin/perl

#
# check_db.pl verifies that the data in the database is correct after running
# the simulate_usage.pl script.
#
# Usage: check_db.pl \
# 	num_instance_events_per_user \
# 	num_storage_events_per_user \
#   num_s3_events_per_user \
#   total_disk_per_user \
#   total_net_per_user \
#   total_s3_obj_size_per_user \
#   total_vol_size_per_user \
#   total_snap_size_per_user \
#   (username/accountname)+
#
# Example: check_db.pl 50 50 50 100 100 200 200 300 user_a/account_a user_b/account_b
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

sub set_and_verify_larger($$) {
	my ($val, $hashref) = @_;
	my %hsh = %{$hashref};

}


#
# MAIN LOGIC
#

if ($#ARGV+1 < 9) {
	print "
 Usage: check_db.pl 
   num_instance_events_per_user 
   num_storage_events_per_user 
   num_s3_events_per_user 
   total_disk_per_user 
   total_net_per_user 
   total_s3_obj_size_per_user 
   total_vol_size_per_user 
   total_snap_size_per_user 
   (username/accountname)+

 Example: check_db.pl 50 50 50 100 100 200 200 300 user_a/account_a user_b/account_b\n";
	die ("Incorrect args");
}


# Parse args
my $num_instance_events_per_user = shift;
my $num_storage_events_per_user = shift;
my $num_s3_events_per_user = shift;
my $total_disk_per_user = shift;
my $total_net_per_user = shift;
my $total_s3_per_user = shift;
my $total_vol_per_user = shift;
my $total_snap_per_user = shift;

my @usernames = ();
my @accountnames = ();
foreach (@ARGV) {
	my ($username, $accountname) = split("/");
	push(@usernames, $username);
	push(@accountnames, $accountname);
}

my $username_csv = "'" . join("','",@usernames) . "'";
my $accountname_csv = "'" . join("','",@accountnames) . "'";




# Establish hashes to keep track of event stats per user
my %user_stats = ();
foreach (@usernames) {
	# Braces at right are a _reference_ to a hash
	$user_stats{$_} = {num_instance_events=>0,
						num_storage_events=>0,
						num_s3_events=>0,
						total_disk=>0,
						total_net=>0,
						total_s3=>0,
						total_vol=>0,
						total_snap=>0,
						buckets_num=>0,
						objs_num=>0,
						vols_num=>0,
						snaps_num=>0};
}



# Gather instance event stats per user
foreach (execute_query("
	select
	  ius.total_disk_io_megs,
	  ius.total_network_io_megs,
	  ru.user_name
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
	order by ius.timestamp_ms
")) {
	my ($disk_io,$net_io,$user_name) = split("\\s+");
	if (defined($user_stats{$user_name})) {
		my $stats = $user_stats{$user_name};
		$stats->{"num_instance_events"}++;
		#TODO: verify greater
		$stats->{"total_disk"} = $disk_io;
		$stats->{"total_net"} = $net_io;
	}
}


# Gather s3 event stats per user
foreach (execute_query("
	select
	  s3s.buckets_num,
	  s3s.objects_num,
	  s3s.objects_megs,
	  ru.user_name
	from
	  s3_usage_snapshot s3s,
	  reporting_user ru,
	  reporting_account ra
	where
	  s3s.owner_id = ru.user_id
	and s3s.account_id = ra.account_id
	and ru.user_name in ($username_csv)
	and ra.account_name in ($accountname_csv)
	order by s3s.timestamp_ms
")) {
	my ($buckets_num,$objects_num,$objects_megs,$user_name) = split("\\s+");
	if (defined($user_stats{$user_name})) {
		my $stats = $user_stats{$user_name};
		$stats->{"num_s3_events"}++;
		#TODO: verify greater
		$stats->{"buckets_num"} = $buckets_num;
		$stats->{"objects_num"} = $objects_num;
		$stats->{"total_s3"} = $objects_megs;
	}
}


# Gather storage event stats per user
foreach (execute_query("
	select
	  sus.snapshot_num,
	  sus.snapshot_megs,
	  sus.volumes_num,
	  sus.volumes_megs,
	  ru.user_name
	from
	  storage_usage_snapshot sus,
	  reporting_user ru,
	  reporting_account ra
	where
	  sus.owner_id = ru.user_id
	and sus.account_id = ra.account_id
	and ru.user_name in ($username_csv)
	and ra.account_name in ($accountname_csv)
	order by sus.timestamp_ms
")) {
	my ($snaps_num, $snaps_megs, $vols_num, $vols_megs,$user_name) = split("\\s+");
	my $stats = $user_stats{$user_name};
	if (defined($user_stats{$user_name})) {
		$stats->{"num_storage_events"}++;
		# TODO: verify greater
		$stats->{"snaps_num"} = $snaps_num;
		$stats->{"vols_num"} = $vols_num;
		$stats->{"total_snap"} = $snaps_megs;
		$stats->{"total_vol"} = $vols_megs;
	}
}



# Print gathered stats
foreach (keys %user_stats) {
	my %stats = %{$user_stats{$_}};
	print "user:$_\n";
	foreach (keys %stats) {
		print "  prop:$_ val:$stats{$_}\n";
	}
}



# Verify stats
foreach (keys %user_stats) {
	my %stats = %{$user_stats{$_}};
	if ($num_instance_events_per_user < $stats{'num_instance_events'}) {
		die("Wrong num_instance_events for user:$_");
	}
	if ($num_storage_events_per_user < $stats{'num_storage_events'}) {
		die("Wrong num_storage_events for user:$_");
	}
	if ($num_s3_events_per_user < $stats{'num_s3_events'}) {
		die("Wrong num_s3_events for user:$_");
	}
	if ($total_disk_per_user < $stats{'total_disk'}) {
		die("Wrong disk_io for user:$_");
	}
	if ($total_net_per_user < $stats{'total_net'}) {
		die("Wrong xx for user:$_");
	}
	if ($total_s3_per_user < $stats{'total_s3'}) {
		die("Wrong xx for user:$_");
	}
	if ($total_vol_per_user < $stats{'total_vol'}) {
		die("Wrong xx for user:$_");
	}
	if ($total_snap_per_user < $stats{'total_snap'}) {
		die("Wrong xx for user:$_");
	}
}

exit 0;

