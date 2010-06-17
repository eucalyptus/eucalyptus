#!/usr/bin/perl

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

$DELIMITER = ",";
$ISCSIADM = untaint(`which iscsiadm`);

# check binaries
if (!-x $ISCSIADM) {
    print STDERR "Unable to find iscsiadm\n";
    do_exit(1);
}

# check input params
$dev_string = untaint(shift @ARGV);

($euca_home, $ip, $store, $passwd) = parse_devstring($dev_string);

print get_device_name($store);

sub parse_devstring {
    my ($dev_string) = @_;
    return split($DELIMITER, $dev_string);
}

sub get_device_name {
    my ($store) = @_;

    if(!open GETSESSION, "iscsiadm -m session -P 3 |") {
	print "Could not get iscsi session information";
	do_exit(1)
    }
    
    $found_target = 0;    
    while (<GETSESSION>) {
        if($_ =~ /Target: (.*)\n/) {
	    $found_target = 1 if $1 == $store;
	} elsif($_ =~ /.*Attached scsi disk ([a-zA-Z0-9]+).*\n/) {
	    if($found_target == 1) {
		return "/dev/", $1;
	    }
	}
    } 
    close GETSESSION; 
}

sub do_exit() {
    $e = shift;

    if ($mounted && ($tmpfile ne "")) {
	system("$mounter umount $tmpfile");
    }
    if ($attached && ($loopdev ne "")) {
	system("$LOSETUP -d $loopdev");
    }
    if ($tmpfile ne "") {
	system("$RMDIR $tmpfile");
    }
    exit($e);
}

sub untaint() {
    $str = shift;
    if ($str =~ /^([ &:#-\@\w.]+)$/) {
	$str = $1; #data is now untainted
    } else {
	$str = "";
    }
    return($str);
}
