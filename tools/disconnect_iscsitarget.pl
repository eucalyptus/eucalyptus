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

($euca_home, $ip, $store, $encrypted_password) = parse_devstring($dev_string);

if(length($euca_home) <= 0) {
    print STDERR "EUCALYPTUS path is not defined.\n";
    do_exit(1);
}

logout_target($ip, $store, $passwd);


sub parse_devstring {
    my ($dev_string) = @_;
    return split($DELIMITER, $dev_string);
}

sub logout_target {
    my ($ip, $store, $passwd) = @_;

    $rc = system("$ISCSIADM -m node -T $store -u");

    if ($rc < 0) { 
	print STDERR "could not logout from session to $store on $ip\n";
 	do_exit(1);
    }
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
