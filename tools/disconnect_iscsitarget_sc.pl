#!/usr/bin/perl

use Crypt::OpenSSL::Random;
use Crypt::OpenSSL::RSA;
use MIME::Base64;

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

$P12_PATH="";
$ISCSI_USER="eucalyptus";

$DELIMITER = ",";
$ISCSIADM = untaint(`which iscsiadm`);
$OPENSSL = untaint(`which openssl`);

# check binaries

if (!-x $ISCSIADM || !-x $OPENSSL) {
    print STDERR "Unable to find required dependencies\n";
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

$P12_PATH = $euca_home."/var/lib/eucalyptus/keys/euca.p12";

$sc_pk = get_storage_pk();

if((length($ip) <= 0) || (length($store) <= 0) || length($encrypted_password) <= 0) {
    print STDERR "Invalid input. Need to specify IP,STORE,ENCRYPTED_PASS\n";
    do_exit(1);
}


$passwd = "not_required";

if ((length($lun) > 0) && ($lun > -1)) {
    delete_lun($store, $lun);
    rescan_target();
    if(only_device($store, $lun)) {
        logout_target($ip, $store, $passwd);
    }
} else {
    logout_target($ip, $store, $passwd);
}

sub get_storage_pk {
    my $pk = "";

    if(!open GET_KEY, "openssl pkcs12 -in $P12_PATH -name eucalyptus -name 'eucalyptus' -password pass:eucalyptus  -passin pass:eucalyptus -passout pass:eucalyptus -nodes | grep -A30 'friendlyName: storage' | grep -A26 'BEGIN RSA' |") {
	print "Could not get storage key";
	do_exit(1)
    }

    $pk = "";
    while(<GET_KEY>) {
	$pk = $pk.$_;
    };
    close(GET_KEY);

    if (!$pk || $pk eq "") {
	if(!open GET_KEY, "openssl pkcs12 -in $P12_PATH -name eucalyptus -name 'eucalyptus' -password pass:eucalyptus  -passin pass:eucalyptus -passout pass:eucalyptus -nodes | grep -A30 'friendlyName: storage' | grep -A27 'BEGIN PRIVATE KEY' |") {
	    print "Could not get storage key";
	    do_exit(1)
	}
	
	$pk = "";
	while(<GET_KEY>) {
	    $pk = $pk.$_;
	};
	close(GET_KEY);
    }

    return $pk;
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


    if(!open DISCONNECT, "$ISCSIADM -m node -T $store -u |") {
        print "Could not logout from target";
        do_exit(1);
    }

    my $logout = "";
    while(<DISCONNECT>) {$logout = $logout.$_;};
    if(length($logout) <= 0) {
	print STDERR "Unable to log out of target. Aborting.\n";
 	do_exit(1);
    }
    print $logout;
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

sub decrypt_password {
    my ($encrypted_passwd, $private_key) = @_;

    $rsa_priv = Crypt::OpenSSL::RSA->new_private_key($private_key);

    $msg = decode_base64($encrypted_passwd);
    $rsa_priv->use_pkcs1_padding();
    $rsa_priv->use_sha1_hash() ;

    my $passwd = $rsa_priv->decrypt($msg);

    return $passwd;
}
