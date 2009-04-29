#!/usr/bin/perl

use Net::Ping;

# need this rule
# iptables -A FORWARD -p UDP --sport 67:68 --dport 67:68 -j LOG --log-level 0
delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

@files = ('/var/log/messages', '/var/log/firewall', '/var/log/syslog', '/var/log/kern.log');
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
        $p = Net::Ping->new();
        $p->{"timeout"} = 0.005;
        $rc = $p->ping("$key");
    }
}
exit(0);
