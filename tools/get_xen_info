#!/usr/bin/perl

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

chomp($str = untaint(`xm info | grep total_memory`));
$str =~ s/\s+:\s+/=/g;
print "$str\n";

chomp($str = untaint(`xm info | grep free_memory`));
$str =~ s/\s+:\s+/=/g;
print "$str\n";

chomp($str = untaint(`xm info | grep nr_cpus`));
$str =~ s/\s+:\s+/=/g;
print "$str\n";

chomp($str = untaint(`xm info | grep nr_nodes`));
$str =~ s/\s+:\s+/=/g;
print "$str\n";

open(FH, "/etc/xen/xend-config.sxp");
while(<FH>) {
    chomp;
    if ($_ =~ /\(dom0-min-mem (\d+)\)/) {
	print "dom0-min-mem=$1\n";
    }
}
close(FH);
exit 0;

sub untaint() {
    $str = shift;
    if ($str =~ /^([ &:#-\@\w.]+)$/) {
	$str = $1; #data is now untainted
    } else {
	print STDERR "inputs are tainted\n";
	$str = "";
    }
    return($str);
}
