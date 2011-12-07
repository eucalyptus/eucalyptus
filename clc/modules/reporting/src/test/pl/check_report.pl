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
my $return_code = 0;
# userhash is used to determine quickly if a user is contained in the users list;
#   if ($userhash{$user}) { ... }
my %userhash = ();

while ($#ARGV+1>0) {
	my ($account,$username_arg) = split(":",shift);
	push(@accountnames, $account);
	foreach (split(",",$username_arg)) {
		push(@usernames, $_);
		$userhash{$_}=1;
		$num_users++;
	}
}

my $username_csv = "'" . join("','",@usernames) . "'";
my $accountname_csv = "'" . join("','",@accountnames) . "'";

sub execute_query($) {
	print "Executing query:$_[0]\n";
	my $output = `db.sh --execute="$_[0]" -D eucalyptus_reporting --skip-column-names`;
	print "Output:$output\n";
	return split("\n",$output);
}

sub runcmd($) {
	print "Running cmd:$_[0]\n";
	my $output = `$_[0]`;
	print "Output:$output\n";
}


#
# Calculate the number of resources used over i intervals, where new resources are allocated 
# each interval and no resources from previous intervals are released.
#
# For example, if 10 new resources are allocated in each of 6 intervals, the total number 
# is equal to (10+20+30+40+50+60). This total can be calculated using the more general
# formula: ((i+1)*a)*(i/2), where i is the number of intervals and a is the amount of
# resource allocated in each interval.
#
# This formula can be inferred by re-arranging terms:
#  (10+20+30+40+50+60)
#  = (60+10)+(50+20)+(40+30)
#  = 70+70+70
#  = 70*3
#  ...but 70 is equal to (i+1)*a, and 3 is the number of intervals / 2.
#
# This arithmetic trick was suggested by Gauss' solution to adding all numbers in a range.
#
sub calculate_total_acc_usage($$) {
	my ($i, $a) = @_;
	return (($i+1)*$a)*($i/2);
}

# set report units to smaller units for test; GB-days would not show up at all
system("euca-modify-property -p reporting.default_size_time_size_unit=MB") and die("Couldn't set size time size unit");
system("euca-modify-property -p reporting.default_size_time_time_unit=SECS") and die("Couldn't set size time time unit");
system("euca-modify-property -p reporting.default_size_unit=MB") and die("Couldn't set size unit");
system("euca-modify-property -p reporting.default_time_unit=SECS") and die("Couldn't set time unit");


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

# Find number of intervals


# Generate and verify instance report, by calling the report servlet using wget
#   and comparing results with expected values.
$report_file = "/tmp/report-instance-" . time();
runcmd("wget -O \"$report_file\" --no-check-certificate \"https://localhost:8443/" .
		"reportservlet?session=$session_id&type=instance&page=0&format=csv&flush=false" .
		"&start=$start_ms&end=$end_ms&criterion=User&groupByCriterion=None\"");
open(REPORT, $report_file);
print "Report: instance\n";
while (my $rl = <REPORT>) {
	my ($blank,$user,$m1Small,$m1SmallTime,$c1Medium,$c1MediumTime,$m1Large,$m1LargeTime,
			$m1XLarge,$m1XLargeTime,$blank2,$c1XLarge,$c1XLargeTime,$net,$disk)
		= split(",",$rl);
	if (($user !~ /^user-/) or (!$userhash{$user})) {
		next; #skip rows in the report which are column headers or for other users
	}
	print "user:$user m1Small:$m1Small m1SmallTime:$m1SmallTime c1Medium:$c1Medium " .
		"c1MediumTime:$c1MediumTime m1Large:$m1Large m1LargeTime:$m1LargeTime " .
		"m1Xlarge:$m1XLarge m1XLargeTime:$m1XLargeTime c1XLarge:$c1XLarge " .
		"c1XLargeTime:$c1XLargeTime net:$net disk:$disk\n";
	# Number should be 2 small instances per user or 
	# Net and disk should be above zero
	# Time should be 2 small instances times duration
}
print "\n\n";
close(REPORT);

# Generate and verify storage report
$report_file = "/tmp/report-storage-" . time();
runcmd("wget -O \"$report_file\" --no-check-certificate \"https://localhost:8443/" .
		"reportservlet?session=$session_id&type=storage&page=0&format=csv&flush=false" .
		"&start=$start_ms&end=$end_ms&criterion=User&groupByCriterion=None\"");
open(REPORT, $report_file);
print "Report: storage\n";
while (my $rl = <REPORT>) {
	my ($blank,$user,$volMaxSize,$volSizeTime,$snapMaxSize,$blank2,$snapSizeTime)
		= split(",",$rl);
	if (($user !~ /^user-/) or (!$userhash{$user})) {
		next; #skip rows in the report which are column headers or for other users
	}
	print "user:$user volMaxSize:$volMaxSize volSizeTime:$volSizeTime " .
			"snapMaxSize:$snapMaxSize snapSizeTime:$snapSizeTime\n";
	# VolMaxSize and snapMaxSize should be total amount accumulated, intervals * allocation amount
	# VolSizeTime and snapSizeTime should be a math algorithm
}
print "\n\n";
close(REPORT);

# Generate and verify s3 report
$report_file = "/tmp/report-s3-" . time();
runcmd("wget -O \"$report_file\" --no-check-certificate \"https://localhost:8443/" .
		"reportservlet?session=$session_id&type=s3&page=0&format=csv&flush=false" .
		"&start=$start_ms&end=$end_ms&criterion=User&groupByCriterion=None\"");
open(REPORT, $report_file);
print "Report: s3\n";
while (my $rl = <REPORT>) {
	my ($blank,$user,$bucketsMaxNum,$objectsMaxSize,$blank2,$objectsMaxTime) = split(",",$rl);
	if (($user !~ /^user-/) or (!$userhash{$user})) {
		next; #skip rows in the report which are column headers or for other users
	}
	print "user:$user bucketsMaxNum:$bucketsMaxNum objectsMaxSize:$objectsMaxSize " .
		"objectsMaxTime:$objectsMaxTime\n";
	# BucketsNumMax should be equal to number of intervals?
	# ObjectsMaxSize should be equal to intervals*objectSize
	# objectsMaxTime should be math alg
}
close(REPORT);

exit($return_code);

