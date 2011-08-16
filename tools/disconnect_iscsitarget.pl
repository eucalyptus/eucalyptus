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

($euca_home, $ip, $store, $encrypted_password, $lun, $auth_mode) = parse_devstring($dev_string);
$store =~ s/\.$//g;

if(length($euca_home) <= 0) {
    print STDERR "EUCALYPTUS path is not defined.\n";
    do_exit(1);
}

if ((length($lun) > 0) && ($lun > -1)) {
    delete_lun($store, $lun);
    rescan_target();
    if(only_device($store, $lun)) {
        logout_target($ip, $store, $passwd);
    }
} else {
    logout_target($ip, $store, $passwd);
}


sub parse_devstring {
    my ($dev_string) = @_;
    return split($DELIMITER, $dev_string);
}

sub delete_lun {
    my ($store, $lun) = @_;
    $num_retries = 1;

    for ($i = 0; $i < $num_retries; ++$i) {
      if(!open GETSESSION, "iscsiadm -m session -P 3 |") {
          print STDERR "Could not get iscsi session information";
          do_exit(1)
      }

      $found_target = 0;
      $sid = -1;
      $host_number = -1;
      while (<GETSESSION>) {
          if ($_ =~ /Target: (.*)\n/) {
              $found_target = 1 if $1 eq $store;
          } elsif ($_ =~ /.*SID: (.*)\n/) {
              if ($found_target == 1) {
                $sid = $1;
              }
          } elsif ($_ =~ /.*Host Number:\s(.*)\sState.*\n/) {
              if ($found_target == 1) {
                $host_number = $1;
                last;
              }
          }
      }
      close GETSESSION;
    }
    return if $sid < 0 && $host_number < 0;
    #this path is kernel specific. FIXME.
    $delete_path = "/sys/class/iscsi_session/session$sid/device/target$host_number:0:0/$host_number:0:0:$lun/delete";
    if (!open DELETELUN, ">$delete_path") {
        print STDERR "Unable to write to $delete_path\n";
        do_exit(1);
    }
    print DELETELUN "1";
    close DELETELUN;
}

sub only_device {
    my ($store, $lun) = @_;
    $num_retries = 5;
    for ($i = 0; $i < $num_retries; ++$i) {
      $only = 1;
      if(!open GETSESSION, "iscsiadm -m session -P 3 |") {
          print STDERR "Could not get iscsi session information";
          do_exit(1)
      }

      $found_target = 0;
      while (<GETSESSION>) {
          if ($_ =~ /Target: (.*)\n/) {
              last if $found_target == 1;
              if ($1 eq $store) {
                  $found_target = 1;
                  $only = 1;
              }
          } elsif ($_ =~ /.*Attached scsi disk ([a-zA-Z0-9]+).*\n/) {
              if ($found_target == 1) {
                  $only = 0;
                  last;
              }
          }
      }
      close GETSESSION;
    }
    return $only;
}

sub rescan_target {
  if(!open GETSESSION, "iscsiadm -m session -R |") {
    print STDERR "Could not get iscsi session information";
    do_exit(1)
  }
  close GETSESSION;
  print "Done rescanning.";
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
