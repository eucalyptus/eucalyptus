#!/usr/bin/perl

#
# check_db.pl runs a sanity check of the database, after events have been
#   stored. It verifies that there is an event stored in the database
#   for every event which was supposed to have been sent, and that the
#   events are plausible (meaning that increments of vols, snapshots,
#   buckets, and objects include plausible increments of the associated
#   values only).
#
# Usage: check_db.pl \
#   num_instances_per_user 
#   range_num_instance_events_per_user (nn-nn)
#   range_num_storage_events_per_user (nn-nn)
#   range_num_s3_events_per_user (nn-nn)
#   (username/accountname)+
#
# Example: check_db.pl 2 50-80 50-80 50-80 user_a/account_a user_b/account_b\n";
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

if ($#ARGV+1 < 5) {
	print "
 Usage: check_db.pl 
   num_instances_per_user 
   range_num_instance_events_per_user (nn-nn)
   range_num_storage_events_per_user (nn-nn)
   range_num_s3_events_per_user (nn-nn)
   (username/accountname)+

 Example: check_db.pl 2 50-80 50-80 50-80 user_a/account_a user_b/account_b\n";
	die ("Incorrect args");
}


# Parse args
my $num_instances_per_user = shift;
my ($min_instance_events,$max_instance_events) = split("-",shift);
my ($min_storage_events,$max_storage_events) = split("-",shift);
my ($min_s3_events,$max_s3_events) = split("-",shift);

my @usernames = ();
my @accountnames = ();
foreach (@ARGV) {
	my ($username, $accountname) = split("/");
	push(@usernames, $username);
	push(@accountnames, $accountname);
}

my $username_csv = "'" . join("','",@usernames) . "'";
my $accountname_csv = "'" . join("','",@accountnames) . "'";


# Count number of instances per user to verify it's correct
foreach (execute_query("
	select
	  ri.user_id as user_id,
	  count(ri.instance_id) as cnt
	from
	  reporting_instance ri,
	  reporting_user ru,
	  reporting_account ra
	where
	  ri.user_id = ru.user_id
	and ri.account_id = ra.account_id
	and ru.user_name in ($username_csv)
	and ra.account_name in ($accountname_csv)
	group by ri.user_id
")) {
	my ($user_name,$count) = split("\\s+");
	print "Found instances user:$user_name #:$count\n";
	if ($count != $num_instances_per_user) {
		die ("Incorrect ins count, expected:$num_instances_per_user found:$count for user:$user_name";
	}
}


# Count instance events per user and verify that disk and net are not zero
foreach (execute_query("
	select
	  count(ius.total_disk_io_megs),
	  sum(ius.total_disk_io_megs),
	  sum(ius.total_network_io_megs),
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
	group by ru.user_name
")) {
	my ($count,$disk_io,$net_io,$user_name) = split("\\s+");
	print "Found instance events user:$user_name #:$count disk:$disk_io net:$net_io\n";
	if ($count < $min_instance_events || $count > $max_instance_events) {
		die ("Incorrect ins event count, expected:$min_instance_events - $max_instance_events , found:$count for user:$user_name";
	}
	if ($disk_io==0 || $net_io==0) {
		die ("Disk or net == 0, user:$user_name";
	}
}



# Count s3 events and verify totals
foreach (execute_query("
	select
	  cnt(s3s.buckets_num) as cnt,
	  max(s3s.buckets_num) as max_buckets,
	  max(s3s.objects_num) as max_objects,
	  max(s3s.objects_megs) as max_size,
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
	group by s3s.timestamp_ms
")) {
	my ($count,$max_buckets,$max_objects,$max_size,$user_name) = split("\\s+");
	print "Found s3 events user:$user_name #:$count max_buckets:$max_buckets max_objects:$max_objects max_size:$max_size\n";
	if ($count < $min_s3_events || $count > $max_s3_events) {
		die ("Incorrect s3 event count, expected:$min_s3_events - $max_s3_events , found:$count for user:$user_name";
	}
	if ($max_buckets == 0 || $max_objects == 0 || $max_size == 0) {
		die ("max_buckets or max_objects or max_size == 0, user:$user_name";
	}
}





# Verify that s3 event properties increment properly: always an additional bucket, or object with size
my $old_user = "";
my $old_buckets_num = 0;
my $old_objects_num = 0;
my $old_objects_megs = 0;
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
	order by user_name, timestamp_ms ASC
")) {
	my ($num_buckets,$num_objects,$obj_megs,$user_name) = split("\\s+");
	print "Found s3 event user:$user_name num_buckets:$num_buckets num_objects:$num_objects obj_megs:$obj_megs\n";
	if ($old_user eq $user_name) {
		# Verify that either total buckets or objects has incremented by one for this event
		if (($num_buckets != $old_buckets_num+1) || ($num_objects != $old_objects_num+1)) {
			die ("buckets or objects not incremented for user:$user_name";
		}
		# Verify that bucket events don't lead to size changes
		if (($num_objects == $old_objects_num) && ($obj_megs != $old_objects_megs)) {
			die ("objects size changed without additional object for user:$user_name";
		}
		# Verify that object count events do lead to size changes in the correct direction
		if (($num_objects != $old_objects_num) && ($obj_megs <= $old_objects_megs)) {
			die ("objects size increased without additional size allocation for user:$user_name";
		}
	}
	$old_buckets_num = $num_buckets;
	$old_objects_num = $num_objects;
	$old_objects_megs = $obj_megs;
	$old_user = $user_name;
}


# Count storage events and verify totals
foreach (execute_query("
	select
	  count(sus.snapshot_num) as cnt,
	  max(sus.snapshot_num) as max_snapshot,
	  max(sus.snapshot_megs) as max_snap_size,
	  max(sus.volumes_num) as max_vols,
	  max(sus.volumes_megs) as max_vol_size,
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
	group by ru.user_name
")) {
	my ($count, $max_snap, $max_snap_size, $max_vols, $max_vol_size, $user_name) = split("\\s+");
	print "Found storage events user:$user_name #:$count max_snap:$max_snap max_snap_size:$max_snap_size max_vols:$max_vols max_vol_size:$max_vol_size\n";
	if ($count < $num_storage_events_per_user) {
		die ("Incorrect storage event count, expected:$num_storage_events_per_user found:$count for user:$user_name";
	}
	if ($max_snap == 0 || $max_snap_size == 0 || $max_vols == 0 || $max_vol_size == 0) {
		die ("max_snap || max_snap_size || max_vols || max_vol_size == 0, user:$user_name";
	}
}



# Verify that storage event properties increment properly: always an additional volume or snapshot, with appropriate size incremented
$old_user = "";
my $old_vol_num = 0;
my $old_vol_size = 0;
my $old_snap_num = 0;
my $old_snap_size = 0;
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
	order by user_name, timestamp_ms ASC
")) {
	my ($snap_num, $snap_size, $vol_num, $vol_size, $user_name) = split("\\s+");
	print "Found storage event, username:$user_name snap_num:$snap_num snap_size:$snap_size vol_num:$vol_num vol_size:$vol_size\n";
	if ($old_user eq $user_name) {
		# Verify that either total buckets or objects has incremented by one for this event
		if (($snap_num != $old_snap_num+1) || ($vol_num != $old_vol_num+1)) {
			die ("snaps or vols not incremented for user:$user_name";
		}
		# Verify that additional size is allocated if snap event
		if (($snap_num == $old_snap_num+1) && ($snap_size <= $old_snap_size)) {
			die ("snaps num increased without additional size allocation for user:$user_name");
		}
		# Verify that additional size is allocated if vol event
		if (($vol_num == $old_vol_num+1) && ($vol_size <= $old_vol_size)) {
			die ("vols num increased without additional size allocation for user:$user_name");
		}
	}
	$old_vol_num = $vol_num;
	$old_vol_size = $vol_size;
	$old_snap_num = $snap_num;
	$old_snap_size = $snap_size;
	$old_user = $user_name;
}


exit 0;

