#!/usr/bin/perl

#
# test_reporting.pl runs a test of the reporting system. It simulates usage
#   of several users simultaneously by spawning processes and generating usage
#   as various users simultaneously. Then it verifies that events were sent and
#   recorded in the database correctly and that reports are generated correctly.
#
# Usage: test_reporting.pl admin_pw num_users num_users_per_Account
#             num_instances_per_user duration_secs_secs image+
#
# author: tom.werges
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
#

use strict;
use warnings;

if ($#ARGV+1 < 5) {
	die "Usage: test_reporting.pl admin_pw num_users num_users_per_account num_instances_per_user duration_secs_secs image+";
}


my $admin_pw = shift;
my $num_users = shift;
my $num_users_per_account = shift;
my $num_instances_per_user = shift;
my $duration_secs = shift;
my @images = ();
my @types = ("m1.small","c1.medium","m1.large");
my %types_num = (); # type=>n
while ($#ARGV+1>0) {
	push(@images,shift);
}

my @usernames = ();
my @accountnames = ();

sub rand_str($) {
	return sprintf("%x",rand(2<<$_[0]));
}

sub execute_query($) {
	return split("\n",`db.sh --execute="$_[0]" -D eucalyptus_reporting --skip-column-names`);
}

sub runcmd($) {
	print "Running cmd:$_[0]\n";
	my $ret = system($_[0]);
	return $ret;
}

# Takes a PW and returns a session id
sub login($) {
	my $password = $_[0];
	runcmd("wget -O /tmp/sessionId --no-check-certificate \"https://localhost:8443/loginservlet?adminPw=$password\"") and die("couldn't login thru wget");
	my $session_id = `cat /tmp/sessionId`;
	return $session_id;
}

sub generate_report($$$$$) {
	my ($session_id, $type, $criterion, $start_ms, $end_ms) = @_;
	my $outfile = "/tmp/out-" . time();
	sleep 1;
	runcmd("wget -O \"$outfile\" --no-check-certificate \"https://localhost:8443/reports?session=$session_id&name=$type&type=CSV&page=0&flush=false&start=$start_ms&end=$end_ms&criterionId=$criterion&groupById=None\"") and die("Couldn't generate CSV report");
	return $outfile;
}


#
# MAIN LOGIC
#

#
# For each user: create an account/user within eucalyptus, download
#  credentials for that account/user, setup credentials dir, and fork a
#  process to run a simulation as that user. Simultaneously run N
#  forked processes of "simulate_one_user.pl" which performs
#  various instance/s3/EBS operations to simulate usage.
#
my $account_num = "";
my $account_name = "";
my $group_name = "";
my $type = "";
my @pids = ();



runcmd("euca-modify-property -p reporting.default_write_interval_secs=90") and die("Couldn't set write interval");

for (my $i=0; $i<$num_users; $i++) {
	if ($i % $num_users_per_account == 0) {
		$account_num = rand_str(16);
		$account_name = "account-$account_num";
		$group_name = "group-$account_num";
		runcmd("euare-accountcreate -a $account_name") and die("Couldn't create account:$account_name");
		runcmd("euare-groupcreate --delegate $account_name -g $group_name") and die("Couldn't create group:$account_name");
		runcmd("euare-groupuploadpolicy --delegate $account_name -g $group_name -p policy-$account_num -o '{ \"Statement\": [ { \"Sid\": \"Stmt1320458221062\", \"Action\": \"*\", \"Effect\": \"Allow\", \"Resource\": \"*\" } ] }'") and die("Couldn't upload policy:$account_name");
	}
	push(@accountnames, $account_name);
	my $user_name = "user-$account_num-" . rand_str(32);
	push(@usernames, $user_name);
	runcmd("euare-usercreate --delegate $account_name -p / -u $user_name") and die("Couldn't create user:$user_name");
	runcmd("euca-get-credentials -a $account_name -u $user_name creds-$user_name.zip") and die("Couldn't get credentials:$user_name");
	runcmd("(mkdir credsdir-$user_name; cd credsdir-$user_name; unzip ../creds-$user_name.zip)") and die("Couldn't unzip credentials:$user_name");
	my $pid = fork();
	# Fork and run simulate_one_user.pl as this euca user
	if ($pid==0) {
		# Run usage simulation as euca user within subshell within separate process; rotate thru images and types
		#exec("(cd \$PWD/credsdir-$user_name; \$PWD/simulate_one_user.pl $num_instances_per_user " . $types[$i % ($#types+1)] . " $duration_secs $num_users " . $images[$i % ($#images+1)] . " > log-$user_name 2>&1)") and die ("Couldn't exec simulate_one_user for: $user_name");
		$types_num{$types[$i % ($#types+1)]}++; # Keep track of num of instance types started
		runcmd("(. \$PWD/credsdir-$user_name/eucarc; \$PWD/simulate_one_user.pl $num_instances_per_user " . $types[$i % ($#types+1)] . " $duration_secs $num_users " . $images[$i % ($#images+1)] . ") > log-$user_name 2>&1") and die ("Couldn't exec simulate_one_user for: $user_name"); exit(0);
	}
	push(@pids, $pid);
}

print "Done forking.\n";
foreach (@pids) {
	print "Waiting for:$_\n";
	waitpid($_,0);
	if ($? != 0) {
		die("Child exited with error code:$_");
	}
}



die("done");



#
# Verify that all events were propagated and that events were written to
#  database properly. This involves counting the number of rows for the
#  recently-created test users in the event history tables, and also
#  verifying that the rows have plausible values.
#
my $username_csv = "'" . join("','",@usernames) . "'";
my $accountname_csv = "'" . join("','",@accountnames) . "'";

my $username = "";
my $count = 0;

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
	($username,$count) = split("\\s+");
	print "Found instances user:$username #:$count\n";
	if ($count != $num_instances_per_user) {
		die ("Incorrect ins count, expected:$num_instances_per_user found:$count for user:$username");
	}
}


my $min_instance_events = ($duration_secs / 15) - 2;
my $max_instance_events = ($duration_secs / 15) + 2;

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
	my ($disk_io,$net_io) = (0,0);
	($count,$disk_io,$net_io,$username) = split("\\s+");
	print "Found instance events user:$username #:$count disk:$disk_io net:$net_io\n";
	if ($count < $min_instance_events || $count > $max_instance_events) {
		die ("Incorrect ins event count, expected:$min_instance_events - $max_instance_events , found:$count for user:$username");
	}
	if ($disk_io==0 || $net_io==0) {
		die ("Disk or net == 0, user:$username");
	}
}

my $min_s3_events = ($duration_secs / 15) - 2;
my $max_s3_events = ($duration_secs / 15) + 2;

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
	my ($max_buckets,$max_objects,$max_size)=(0,0,0);
	($count,$max_buckets,$max_objects,$max_size,$username) = split("\\s+");
	print "Found s3 events user:$username #:$count max_buckets:$max_buckets max_objects:$max_objects max_size:$max_size\n";
	if ($count < $min_s3_events || $count > $max_s3_events) {
		die ("Incorrect s3 event count, expected:$min_s3_events - $max_s3_events , found:$count for user:$username");
	}
	if ($max_buckets == 0 || $max_objects == 0 || $max_size == 0) {
		die ("max_buckets or max_objects or max_size == 0, user:$username");
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
	my ($num_buckets,$num_objects,$obj_megs) = (0,0,0);
	($num_buckets,$num_objects,$obj_megs,$username) = split("\\s+");
	print "Found s3 event user:$username num_buckets:$num_buckets num_objects:$num_objects obj_megs:$obj_megs\n";
	if ($old_user eq $username) {
		# Verify that either total buckets or objects has incremented by one for this event
		if (($num_buckets != $old_buckets_num+1) || ($num_objects != $old_objects_num+1)) {
			die ("buckets or objects not incremented for user:$username");
		}
		# Verify that bucket events don't lead to size changes
		if (($num_objects == $old_objects_num) && ($obj_megs != $old_objects_megs)) {
			die ("objects size changed without additional object for user:$username");
		}
		# Verify that object count events do lead to size changes in the correct direction
		if (($num_objects != $old_objects_num) && ($obj_megs <= $old_objects_megs)) {
			die ("objects size increased without additional size allocation for user:$username");
		}
	}
	$old_buckets_num = $num_buckets;
	$old_objects_num = $num_objects;
	$old_objects_megs = $obj_megs;
	$old_user = $username;
}

my $min_storage_events = ($duration_secs / 15) - 2;
my $max_storage_events = ($duration_secs / 15) - 2;


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
	my ($max_snap,$max_snap_size,$max_vols,$max_vol_size) = (0,0,0,0);
	($count, $max_snap, $max_snap_size, $max_vols, $max_vol_size, $username) = split("\\s+");
	print "Found storage events user:$username #:$count max_snap:$max_snap max_snap_size:$max_snap_size max_vols:$max_vols max_vol_size:$max_vol_size\n";
	if ($count < $min_storage_events || $count > $max_storage_events) {
		die ("Incorrect storage event count, expected:$min_storage_events - $max_storage_events , found:$count for user:$username");
	}
	if ($max_snap == 0 || $max_snap_size == 0 || $max_vols == 0 || $max_vol_size == 0) {
		die ("max_snap || max_snap_size || max_vols || max_vol_size == 0, user:$username");
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
	my ($snap_num, $snap_size, $vol_num, $vol_size) = (0,0,0,0);
	($snap_num, $snap_size, $vol_num, $vol_size, $username) = split("\\s+");
	print "Found storage event, username:$username snap_num:$snap_num snap_size:$snap_size vol_num:$vol_num vol_size:$vol_size\n";
	if ($old_user eq $username) {
		# Verify that either total buckets or objects has incremented by one for this event
		if (($snap_num != $old_snap_num+1) || ($vol_num != $old_vol_num+1)) {
			die ("snaps or vols not incremented for user:$username");
		}
		# Verify that additional size is allocated if snap event
		if (($snap_num == $old_snap_num+1) && ($snap_size <= $old_snap_size)) {
			die ("snaps num increased without additional size allocation for user:$username");
		}
		# Verify that additional size is allocated if vol event
		if (($vol_num == $old_vol_num+1) && ($vol_size <= $old_vol_size)) {
			die ("vols num increased without additional size allocation for user:$username");
		}
	}
	$old_vol_num = $vol_num;
	$old_vol_size = $vol_size;
	$old_snap_num = $snap_num;
	$old_snap_size = $snap_size;
	$old_user = $username;
}

# Generate CSV reports
#   Verify instance CSV
#   Verify S3 CSV
#   Verify Storage CSV
#   How to get correct values?
#
# Record intervals?
# Generate CSV reports at various intervals for all report types
# Surrounding, within, before beginning, after end
# TODO: replace /tmp/sessionId?


# Run simulate_negative_usage with value
# Gather timestamp

# Run negative_check_db with values
# Use timestamp

