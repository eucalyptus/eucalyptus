#!/usr/bin/perl

# Copyright 2009-2012 Eucalyptus Systems, Inc.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
# Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
# CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
# additional information or have any questions.
#
# This file may incorporate work covered under the following copyright
# and permission notice:
#
#   Software License Agreement (BSD License)
#
#   Copyright (c) 2008, Regents of the University of California
#   All rights reserved.
#
#   Redistribution and use of this software in source and binary forms,
#   with or without modification, are permitted provided that the
#   following conditions are met:
#
#     Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#
#     Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer
#     in the documentation and/or other materials provided with the
#     distribution.
#
#   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
#   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
#   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
#   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
#   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
#   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
#   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
#   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
#   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
#   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
#   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
#   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
#   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
#   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
#   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
#   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
#   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
#   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
#   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.

# check_db.pl verifies that simulated usage results in correct events stored
#  in the database.
#
# This script is called by test.pl; see test.pl for comprehensive documentation
# of the perl test suite.

use strict;
use warnings;
require "test_common.pl";

if ($#ARGV+1 < 5) {
	die "Usage: check_db.pl num_instances_per_user duration_secs upload_file write_interval (account:user,user,user)+";
}


my $num_instances_per_user = shift;
my $duration_secs = shift;
my $upload_file = shift;
my $write_interval = shift;
my $num_users = 0;
my $account = "";
my $username_arg = "";
my @usernames = ();
my @accountnames = ();
my $return_code = 0;

while ($#ARGV+1>0) {
	($account,$username_arg) = split(":",shift);
	push(@accountnames, $account);
	foreach (split(",",$username_arg)) {
		push(@usernames, $_);
		$num_users++;
	}
}


#
# Verify that all events were propagated and that events were written to
#  database properly, by summing db columns and counting rows
#
my $username_csv = "'" . join("','",@usernames) . "'";
my $accountname_csv = "'" . join("','",@accountnames) . "'";

my $username = "";
my $count = 0;
my $num_rows = 0;

# Count number of instances per user to verify it's correct
foreach (execute_query("
	select
	  ru.user_name as user_name,
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
	group by ru.user_name
")) {
	($username,$count) = split("\\s+");
	print "Found instances user:$username #:$count\n";
	$return_code |= $return_code = test_eq("ins count", $num_instances_per_user, $count);
	$num_rows++;
}
$return_code |= test_eq("rows count", $num_users, $num_rows);
$num_rows=0;

use integer;
my $interval_cnt = $duration_secs / $write_interval;

# Count instance events per user and verify that disk and net are not zero
foreach (execute_query("
	select
	  count(ius.total_disk_io_megs),
	  max(ius.total_disk_io_megs),
	  max(ius.total_network_io_megs),
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
	$return_code |= test_range("ins event count", $interval_cnt, $count, 1);
	if ($disk_io==0) {
		die ("Disk == 0");
	}
	$num_rows++;
}
$return_code |= test_eq("rows count", $num_users, $num_rows);
$num_rows=0;



# Count s3 events and verify totals
my $object_size = (-s $upload_file)/1024/1024;
foreach (execute_query("
	select
	  count(s3s.buckets_num) as cnt,
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
	group by ru.user_name
")) {
	my ($max_buckets,$max_objects,$max_size)=(0,0,0);
	($count,$max_buckets,$max_objects,$max_size,$username) = split("\\s+");
	$return_code |= test_range("count", $interval_cnt, $count, 1);
	$return_code |= test_eq("max_buckets", $interval_cnt, 1);
	$return_code |= test_range("max_objects", $interval_cnt, $max_size, 1);
	$return_code |= test_range("max_size", $interval_cnt*$object_size, $max_size, $object_size);
	$num_rows++;
}
$return_code |= test_eq("rows count", $num_users, $num_rows);
$num_rows=0;


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
	$return_code |= test_range("count", $interval_cnt, $count, 1);
	$return_code |= test_range("max_snap", $interval_cnt, $max_snap, 1);
	$return_code |= test_range("max_vols", $interval_cnt, $max_vols, 1);
	$return_code |= test_range("max_vol_size", $interval_cnt*storage_usage_mb(), $max_vol_size, storage_usage_mb());
	# TODO: how do we determine what this should be???
	if ($max_snap_size < 1) {
		die ("max snap size expected: >1, got:$max_snap_size");
	}
	$num_rows++;
}
$return_code |= test_eq("rows count", $num_users, $num_rows);

exit($return_code);
