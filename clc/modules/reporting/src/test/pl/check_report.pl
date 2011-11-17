#!/usr/bin/perl


use strict;
use warnings;

if ($#ARGV+1 < 2) {
	die ("Usage: check_report.pl admin_pw (account:user+)+");
}

my $password = shift;
my $session_id = "";
my $report_file = "";
my $start_ms = 0;
my $end_ms = 0;
my $num_users = 0;
my @usernames = ();
my @accountnames = ();

while ($#ARGV+1>0) {
	my ($account,$username_arg) = split(":",shift);
	push(@accountnames, $account);
	foreach (split(",",$username_arg)) {
		push(@usernames, $_);
		$num_users++;
	}
}

my $username_csv = "'" . join("','",@usernames) . "'";
my $accountname_csv = "'" . join("','",@accountnames) . "'";

sub execute_query($) {
	print "Executing query:$_[0]\n";
	my $output = `./db.sh --execute="$_[0]" -D eucalyptus_reporting --skip-column-names`;
	print "Output:$output\n";
	return split("\n",$output);
}

sub runcmd($) {
	print "Running cmd:$_[0]\n";
	my $output = `$_[0]`;
	print "Output:$output\n";
}


# login and gather session id
runcmd("wget -O /tmp/sessionId --no-check-certificate \"https://localhost:8443/loginservlet?adminPw=$password\"");
$session_id = `cat /tmp/sessionId`;

my $username="";
# Get first and last times for instance events
foreach (execute_query("
	select
	  min(ius.timestamp_ms) as min_time,
	  max(ius.timestamp_ms) as max_time,
	  ru.user_name
	from
	  reporting_instance ri,
	  instance_usage_snapshot ius,
	  reporting_user ru,
	  reporting_account ra
	where
	  ri.user_id = ru.user_id
	and ius.uuid = ri.uuid
	and ri.account_id = ra.account_id
	and ru.user_name in ($username_csv)
	and ra.account_name in ($accountname_csv)
	group by ru.user_name
")) {
	($start_ms,$end_ms,$username) = split("\\s+");
	print "Found times $start_ms - $end_ms for user:$username\n";
}

# Generate report WITHIN
$start_ms++;
$end_ms--;
foreach (("instance","storage","s3")) {
	$report_file = "/tmp/report-$_-" . time();
	runcmd("wget -O \"$report_file\" --no-check-certificate \"https://localhost:8443/reportservlet?session=$session_id&type=$_&page=0&format=csv&flush=false&start=$start_ms&end=$end_ms&criterion=User&groupByCriterion=None\"");
	# Parse report
	open(REPORT, $report_file);
	print "Report: $_\n";
	while (my $rl = <REPORT>) {
		print "  line:$rl\n";
	}
	close(REPORT);
}

# Compare report against total number of instances, instance-hours, instance types

