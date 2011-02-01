#!/usr/bin/perl

use Crypt::OpenSSL::Random ;
use Crypt::OpenSSL::RSA ;
use MIME::Base64;


delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';
$KEY_PATH="";

$DELIMITER = ",";
$ISCSIADM = untaint(`which iscsiadm`);
$ISCSI_USER = "eucalyptus";

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

$KEY_PATH = $euca_home."/var/lib/eucalyptus/keys/node-pk.pem";

$password = decrypt_password($encrypted_password);

login_target($ip, $store, $password);

#wait for device to be ready

sleep 1;

print get_device_name($store);

sub parse_devstring {
    my ($dev_string) = @_;
    return split($DELIMITER, $dev_string);
}

sub login_target {
    my ($ip, $store, $passwd) = @_;
    if(!open DISCOVERY, "iscsiadm -m discovery -t sendtargets -p $ip |") {
	print "Could not discover targets";
	do_exit(1)
    }

    while(<DISCOVERY>) {};

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

    if(!open LOGIN, "iscsiadm -m node -T $store -l |") {
	print "Could not login to target";
	do_exit(1)
    }

    while(<LOGIN>) {};
}

sub decrypt_password {
    my ($encrypted_passwd) = @_;

    $private_key = "" ;
    open (KEYFILE, $KEY_PATH) ;
    while (<KEYFILE>) {
    $private_key .= $_ ;
    }
    close(KEYFILE);

    $rsa_priv = Crypt::OpenSSL::RSA->new_private_key($private_key);

    $msg = decode_base64($encrypted_passwd);
    $rsa_priv->use_pkcs1_padding();
    $rsa_priv->use_sha1_hash() ;

    my $passwd = $rsa_priv->decrypt($msg);

    return $passwd;
}

sub get_device_name {
    my ($store) = @_;
    $num_retries = 5;

    for ($i = 0; $i < $num_retries; ++$i) {
      if(!open GETSESSION, "iscsiadm -m session -P 3 |") {
	  print STDERR "Could not get iscsi session information";
	  do_exit(1)
      }
    
      $found_target = 0;
      $attach_seen = 1;    
      while (<GETSESSION>) {
          if($_ =~ /Target: (.*)\n/) {
	      last if $attach_seen == 0; 
	      $found_target = 1 if $1 eq $store;
	      $attach_seen = 0;
	  } elsif($_ =~ /.*Attached scsi disk ([a-zA-Z0-9]+).*\n/) {
	      if($found_target == 1) {
		return "/dev/", $1;
	      }
	      $attach_seen = 1;
	  }
      } 
      close GETSESSION; 
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
