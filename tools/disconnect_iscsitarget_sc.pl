#!/usr/bin/perl

use Crypt::OpenSSL::Random;
use Crypt::OpenSSL::RSA;
use MIME::Base64;

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

$P12_PATH="";
$ISCSI_USER="eucalyptus";

if($ENV{'EUCALYPTUS'}) {
    $P12_PATH = $ENV{'EUCALYPTUS'}."/var/lib/eucalyptus/keys/euca.p12";
} else {
    print "EUCALYPTUS must be defined.";
    do_exit(1);
}


$DELIMITER = ",";
$ISCSIADM = untaint(`which iscsiadm`);
$OPENSSL = untaint(`which openssl`);

# check binaries

if (!-x $ISCSIADM || !-x $OPENSSL) {
    print STDERR "Unable to find required dependencies\n";
    do_exit(1);
}

$sc_pk = get_storage_pk();

# check input params
$dev_string = untaint(shift @ARGV);

($ip, $store, $encrypted_password) = parse_devstring($dev_string);

$password = decrypt_password($encrypted_password, $sc_pk);

logout_target($ip, $store, $password);

sub get_storage_pk {
    if(!open GET_KEY, "openssl pkcs12 -in $P12_PATH -name eucalyptus -name 'eucalyptus' -password pass:eucalyptus  -passin pass:eucalyptus -passout pass:eucalyptus -nodes | grep -A30 'friendlyName: storage' | grep -A26 'BEGIN RSA' |") {
	print "Could not get storage key";
	do_exit(1)
    }

    my $pk = "";

    while(<GET_KEY>) {
	$pk = $pk.$_;
    };

    return $pk;
}


sub parse_devstring {
    my ($dev_string) = @_;
    return split($DELIMITER, $dev_string);
}

sub logout_target {
    my ($ip, $store, $passwd) = @_;
    if(!open DISCOVERY, "iscsiadm -m discovery -t sendtargets -p $ip |") {
	print "Could not discover targets";
	do_exit(1)
    }

    while(<DISCOVERY>) {};

    if(!open USERNAME, "$ISCSIADM -m node -T $store -p $ip --op=update --name node.session.auth.username --value=$ISCSI_USER |") {
        print "Could not update target username";
        do_exit(1);
    }

    while(<USERNAME>) {};

    if(!open PASSWD, "$ISCSIADM -m node -T $store -p $ip --op=update --name node.session.auth.password --value=$passwd |") {
        print "Could not update target password";
        do_exit(1);
    }

    while(<PASSWD>) {};

    if(!open DISCONNECT, "$ISCSIADM -m node -T $store -p $ip -u |") {
        print "Could not logout from target";
        do_exit(1);
    }

    while(<DISCONNECT>) {print $_;};
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
