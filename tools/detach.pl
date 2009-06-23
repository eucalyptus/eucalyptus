#!/usr/bin/perl

use POSIX;

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

my $rootwrap = untaint(shift @ARGV);
my $virsh = untaint(shift @ARGV);
my $instanceId = untaint(shift @ARGV);
my $localdev = untaint(shift @ARGV);
my $virshxmlfile = untaint(shift @ARGV);

my $distro = "GENERIC";
my $user = getuid();

$inputfail = 1;
$detachfail = 2;
system("cp $virshxmlfile /tmp/wtf");
$distro = detect_distro();
print STDERR "DISTRO: $distro\n";
if ( ! -x "$rootwrap" ) {
    print STDERR "ERROR: cannot find root wrapper '$rootwrap'\n";
    exit $inputfail;
}

if ($distro eq "GENERIC") {
    # try both workarounds
    if ( ! -f "$virshxmlfile" ) {
	print STDERR "ERROR: cannot locate virsh XML file\n";
	exit $inputfail;
    }
    
    $cmd = "$rootwrap $virsh detach-device $instanceId $virshxmlfile";
    $rc = system($cmd);
    if ($rc) {
	print STDERR "ERROR: cmd failed '$cmd'\n";
	$cmd = "sudo xm block-detach $instanceId $localdev";
	$rc = system($cmd);
	if ($rc) {
	    print STDERR "ERROR: cmd failed '$cmd'\n";
	}
    }
    
    if ($rc) {
	exit ($detachfail);
    }
    
} elsif ($distro eq "DEBIAN") {
    # do the debian magic
    $cmd = "sudo xm block-detach $instanceId $localdev";
    $rc = system($cmd);
    if ($rc) {
	print STDERR "ERROR: cmd failed '$cmd'\n";
	exit ($detachfail);
    }
} else {
    print STDERR "ERROR: unknown distribution\n";
    exit $inputfail;
}

exit 0;

sub detect_distro() {
    if ( -f "/etc/debian_version" ) {
	return("DEBIAN");
    } elsif ( -f "/etc/issue" ) {
	open(FH, "/etc/issue");
	while(<FH>) {
	    chomp;
	    my $line = $_;
	    if ($line =~ /CentOS/) {
		close(FH);
		return("GENERIC");
	    }
	}
	close(FH);
    }
    return("GENERIC");
}

sub untaint() {
    my $str = shift;
    if ($str =~ /^([ &:#-\@\w.]+)$/) {
	$str = $1; #data is now untainted                                       
    } else {
        $str = "";
    }
    return($str);
}
