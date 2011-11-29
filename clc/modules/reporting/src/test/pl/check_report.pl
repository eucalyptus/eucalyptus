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
	# TODO: all users
	print "Found times $start_ms - $end_ms for user:$username\n";
}
if ($end_ms==0) {
	die ("unreasonable end_ms value found; need to make db.sh executable?");
}

# Generate report WITHIN
$start_ms++;
$end_ms--;

# Generate and verify instance report
$report_file = "/tmp/report-instance-" . time();
runcmd("wget -O \"$report_file\" --no-check-certificate \"https://localhost:8443/" .
		"reportservlet?session=$session_id&type=instance&page=0&format=csv&flush=false" .
		"&start=$start_ms&end=$end_ms&criterion=User&groupByCriterion=None\"");
# Parse report
open(REPORT, $report_file);
print "Report: instance\n";
while (my $rl = <REPORT>) {
	my ($blank,$user,$m1Small,$m1SmallTime,$c1Medium,$c1MediumTime,$m1Large,$m1LargeTime,
			$m1XLarge,$m1XLargeTime,$blank2,$c1XLarge,$c1XLargeTime,$net,$disk)
		= split(",",$rl);
	if ($user !~ /^user-/) {
		next;
	}
	print "user:$user m1Small:$m1Small m1SmallTime:$m1SmallTime c1Medium:$c1Medium " .
		"c1MediumTime:$c1MediumTime m1Large:$m1Large m1LargeTime:$m1LargeTime " .
		"m1Xlarge:$m1XLarge m1XLargeTime:$m1XLargeTime c1XLarge:$c1XLarge " .
		"c1XLargeTime:$c1XLargeTime net:$net disk:$disk\n";
}
print "\n\n";
close(REPORT);

# Generate and verify storage report
$report_file = "/tmp/report-storage-" . time();
runcmd("wget -O \"$report_file\" --no-check-certificate \"https://localhost:8443/" .
		"reportservlet?session=$session_id&type=storage&page=0&format=csv&flush=false" .
		"&start=$start_ms&end=$end_ms&criterion=User&groupByCriterion=None\"");
# Parse report
open(REPORT, $report_file);
print "Report: storage\n";
while (my $rl = <REPORT>) {
	my ($blank,$user,$volMaxSize,$volSizeTime,$snapMaxSize,$blank2,$snapSizeTime)
		= split(",",$rl);
	if ($user !~ /^user-/) {
		next;
	}
	print "user:$user volMaxSize:$volMaxSize volSizeTime:$volSizeTime " .
			"snapMaxSize:$snapMaxSize snapSizeTime:$snapSizeTime\n";
}
print "\n\n";
close(REPORT);

# Generate and verify s3 report
$report_file = "/tmp/report-s3-" . time();
runcmd("wget -O \"$report_file\" --no-check-certificate \"https://localhost:8443/" .
		"reportservlet?session=$session_id&type=s3&page=0&format=csv&flush=false" .
		"&start=$start_ms&end=$end_ms&criterion=User&groupByCriterion=None\"");
# Parse report
open(REPORT, $report_file);
print "Report: s3\n";
while (my $rl = <REPORT>) {
	my ($blank,$user,$bucketsMaxNum,$objectsMaxSize,$blank2,$objectsMaxTime) = split(",",$rl);
	if ($user !~ /^user-/) {
		next;
	}
	print "user:$user bucketsMaxNum:$bucketsMaxNum objectsMaxSize:$objectsMaxSize " .
		"objectsMaxTime:$objectsMaxTime\n";
}
close(REPORT);


