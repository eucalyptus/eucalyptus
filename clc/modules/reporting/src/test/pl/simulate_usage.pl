#!/usr/bin/perl

#
# simulate_usage.pl runs a test of the reporting system. It simulates usage
#   of several users simultaneously by spawning processes and generating usage
#   as various users simultaneously. Then it verifies that events were sent and
#   recorded in the database correctly and that reports are generated correctly.
#
# Usage: simulate_usage.pl admin_pw num_users num_users_per_Account
#             num_instances_per_user duration_secs_secs image+
#
# author: tom.werges
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
#

use strict;
use warnings;

if ($#ARGV+1 < 8) {
	die "Usage: simulate_usage.pl upload_file num_users num_users_per_account num_instances_per_user duration_secs write_interval storage_usage_mb image+";
}


my $upload_file = shift;
my $num_users = shift;
my $num_users_per_account = shift;
my $num_instances_per_user = shift;
my $duration_secs = shift;
my $write_interval = shift;
my $storage_usage_mb = shift;
my @images = ();
my @types = ("m1.small","c1.medium","m1.large");
my %types_num = (); # type=>n
while ($#ARGV+1>0) {
	push(@images,shift);
}

sub rand_str($) {
	return sprintf("%x",rand(2<<$_[0]));
}

sub runcmd($) {
	print STDERR "Running cmd:$_[0]\n";
	my $ret = system("$_[0] 1>&2");
	return $ret;
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
my @usernames = ();
my @pids = ();

runcmd("euca-modify-property -p reporting.default_write_interval_secs=$write_interval") and die("Couldn't set write interval");

for (my $i=0; $i<$num_users; $i++) {
	if ($i % $num_users_per_account == 0) {
		$account_num = rand_str(16);
		if ($account_name ne "") {
			print "$account_name" . join(",",@usernames) . " ";
			@usernames=();
		}
		$account_name = "account-$account_num";
		$group_name = "group-$account_num";
		runcmd("euare-accountcreate -a $account_name") and die("Couldn't create account:$account_name");
		runcmd("euare-groupcreate --delegate $account_name -g $group_name") and die("Couldn't create group:$account_name");
		runcmd("euare-groupuploadpolicy --delegate $account_name -g $group_name -p policy-$account_num -o '{ \"Statement\": [ { \"Sid\": \"Stmt1320458221062\", \"Action\": \"*\", \"Effect\": \"Allow\", \"Resource\": \"*\" } ] }'") and die("Couldn't upload policy:$account_name");
	}
	my $user_name = "user-$account_num-" . rand_str(32);
	runcmd("euare-usercreate --delegate $account_name -p / -u $user_name") and die("Couldn't create user:$user_name");
	runcmd("euare-groupadduser --delegate $account_name -g $group_name -u $user_name") and die("Couldn't add user:$user_name to group:$group_name");
	runcmd("euca-get-credentials -a $account_name -u $user_name creds-$user_name.zip") and die("Couldn't get credentials:$user_name");
	runcmd("(mkdir credsdir-$user_name; cd credsdir-$user_name; unzip ../creds-$user_name.zip)") and die("Couldn't unzip credentials:$user_name");
	push(@usernames, $user_name);
	my $pid = fork();
	# Fork and run simulate_one_user.pl as this euca user
	if ($pid==0) {
		# Run usage simulation as euca user within subshell within separate process; rotate thru images and types
		#exec("(cd \$PWD/credsdir-$user_name; \$PWD/simulate_one_user.pl $num_instances_per_user " . $types[$i % ($#types+1)] . " $duration_secs $num_users " . $images[$i % ($#images+1)] . " > log-$user_name 2>&1)") and die ("Couldn't exec simulate_one_user for: $user_name");
		$types_num{$types[$i % ($#types+1)]}++; # Keep track of num of instance types started
		runcmd("(. \$PWD/credsdir-$user_name/eucarc; . \$PWD/credsdir-$user_name/iamrc; \$PWD/simulate_one_user.pl $num_instances_per_user " . $types[$i % ($#types+1)] . " $write_interval $duration_secs $upload_file $storage_usage_mb " . $images[$i % ($#images+1)] . ") > log-$user_name 2>&1") and die ("Couldn't exec simulate_one_user for: $user_name"); exit(0);
	}
	push(@pids, $pid);
}
print "$account_name:" . join(",",@usernames) . "\n";

print STDERR "Done forking.\n";
foreach (@pids) {
	print STDERR "Waiting for:$_\n";
	waitpid($_,0);
	if ($? != 0) {
		die("Child exited with error code:$_");
	}
}

