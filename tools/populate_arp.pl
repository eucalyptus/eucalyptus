#!/usr/bin/perl

# need this rule
# iptables -A FORWARD -p UDP --sport 67:68 --dport 67:68 -j LOG --log-level 0
delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

@files = ('/var/log/syslog', '/var/log/kern.log');
foreach $file (@files) {
open(FH, "$file");
while(<FH>) {
    chomp;
    if ($_ =~ /DST=(\d+\.\d+\.\d+\.\d+)/) {
	$ips{"$1"} = 1;
    }
}
close(FH);
}
foreach $key (keys(%ips)) {
    if ($key ne "255.255.255.255") {
	$cmd = "ping -q -c 1 -w 1 $key";
	system("$cmd >/dev/null &");
    }
}
exit(0);
