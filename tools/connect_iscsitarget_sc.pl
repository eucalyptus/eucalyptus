#!/usr/bin/perl

use Crypt::OpenSSL::Random ;
use Crypt::OpenSSL::RSA ;
use MIME::Base64;


delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';
$P12_PATH="";

$DELIMITER = ",";
$ISCSIADM = untaint(`which iscsiadm`);
$OPENSSL = untaint(`which openssl`);
$ISCSI_USER = "eucalyptus";

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


if ((length($lun) > 0) && ($lun > -1)) {
  # check if a session corresponding to the store exists
  if (get_session($ARGV[0]) == 1) {
    # rescan session
    rescan_target();  
  } else {
    # else login to session
    if(length($auth_mode) > 0) {
      $password = "not_required";
    } else {
      $password = decrypt_password($encrypted_password, $sc_pk);
    }
    if(length($password) <= 0) {
      print STDERR "Unable to decrypt target password. Aborting.\n";
    }
    login_target($ip, $store, $password, $auth_mode);
  }
  # get dev from lun
  sleep 1;
  print get_device_name_from_lun($store, $lun);
} else {
  $password = decrypt_password($encrypted_password, $sc_pk);

  if(length($password) <= 0) {
    print STDERR "Unable to decrypt target password. Aborting.\n";
  }
  login_target($ip, $store, $password);
  #wait for device to be ready
  sleep 1;
  print get_device_name($store);
}

sub parse_devstring {
    my ($dev_string) = @_;
    return split($DELIMITER, $dev_string);
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

sub login_target {
    my ($ip, $store, $passwd) = @_;
    if(!open DISCOVERY, "iscsiadm -m discovery -t sendtargets -p $ip |") {
	print "Could not discover targets";
	do_exit(1)
    }

    while(<DISCOVERY>) {};

    if($password ne "not_required") {
      if(!open USERNAME, "iscsiadm -m node -T $store --op=update --name node.session.auth.username --value=$ISCSI_USER |") {
        print "Could not update target username";
        do_exit(1)
      }

      while(<USERNAME>) {};

      if(!open PASSWD, "iscsiadm -m node -T $store --op=update --name node.session.auth.password --value=$passwd |") {
        print "Could not update target password";
        do_exit(1)
      }

      while(<PASSWD>) {};

    }

    if(!open LOGIN, "iscsiadm -m node -T $store -l |") {
	print "Could not login to target";
	do_exit(1)
    }

    my $login = "";
    while(<LOGIN>) {$login = $login.$_;};
    if(length($login) <= 0) {
	print STDERR "Unable to login to target. Aborting.\n";
 	do_exit(1);
    }
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

sub get_session {
    my ($store) = @_;
    $num_retries = 5;

    for ($i = 0; $i < $num_retries; ++$i) {
      if(!open GETSESSION, "iscsiadm -m session |") {
          print STDERR "Could not get iscsi session information";
          do_exit(1)
      }

      while (<GETSESSION>) {
        if ($_ =~ /.*$store\n/) {
          close GETSESSION;
	  return 1;
	}
      }
      close GETSESSION;
    }
    return 0;
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
	    $found_target = 1 if $1 eq $store;
	} elsif($_ =~ /.*Attached scsi disk ([a-zA-Z0-9]+).*\n/) {
	    if($found_target == 1) {
		return "/dev/".$1;
	    }
	}
    } 
    close GETSESSION; 
}

sub get_device_name_from_lun {
    my ($store, $lun) = @_;
    $num_retries = 5;

    for ($i = 0; $i < $num_retries; ++$i) {
      if(!open GETSESSION, "iscsiadm -m session -P 3 |") {
          print STDERR "Could not get iscsi session information";
          do_exit(1)
      }

      $found_target = 0;
      $found_lun = 0;
      $attach_seen = 1;
      while (<GETSESSION>) {
          if ($_ =~ /Target: (.*)\n/) {
              last if $attach_seen == 0;
              $found_target = 1 if $1 eq $store;
              $attach_seen = 0;
              $found_lun = 0;
          } elsif ($_ =~ /.*Attached scsi disk ([a-zA-Z0-9]+).*\n/) {
              if ($found_target == 1 && $found_lun == 1) {
                return "/dev/", $1;
              }
              $attach_seen = 1;
          } elsif ($_ =~ /.*Lun: (.*)\n/) {
              $found_lun = 1 if $1 eq $lun;
          }
      }
      close GETSESSION;
    }
}

sub rescan_target {

  if(!open GETSESSION, "iscsiadm -m session -R |") {
    print STDERR "Could not get iscsi session information";
    do_exit(1)
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
