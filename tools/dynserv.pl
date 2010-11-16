#!/usr/bin/perl

use File::Path;

my $eucalyptus = $ENV{'EUCALYPTUS'};
my $dynpath = "$eucalyptus/var/lib/eucalyptus/dynserv";
my $rc;
my $dynpath = shift @ARGV;
my @allowhosts = @ARGV;

$rc = setup_dynpath($dynpath);
if ($rc) {
    print "ERROR: could not create directory structure\n";
    exit (1);
}

$rc = open(OFH, ">$dynpath/dynserv-httpd.conf");
if ($rc <= 0) {
    print "ERROR: could not create config file '$dynpath/dynserv-httpd.conf'\n";
    exit (1);
}

$authz = find_authz();
$apache = find_apache2();
if ($authz eq "none" || $apache eq "none") {
    print "ERROR: cannot find authz module ($authz) or apache2 ($apache)\n";
    exit (1);
}

$rc = prepare_configfile($eucalyptus, $dynpath, "eucalyptus", "eucalyptus", $authz, $apache, @allowhosts);
if ($rc) {
    print "ERROR: could not set up configfile\n";
    exit(1);
}

$rc = restart_apache($dynpath, $apache);
if ($rc) {
    print "ERROR: could not restart apache2\n";
    exit(1);
}

exit(0);

sub restart_apache() {
    my $dynpath = shift @_;
    my $apache = shift @_;

    my $cmd = "$apache -f $dynpath/dynserv-httpd.conf -k graceful";
    my $rc = system("$cmd");
    if ($rc) {
	print "ERROR: could not run cmd '$cmd'\n";
    }
    return($rc);
}

sub prepare_configfile() {
    my $eucalyptus = shift @_;
    my $dynpath = shift @_;
    my $user = shift @_;
    my $group = shift @_;
    my $authz = shift @_;
    my $apache = shift @_;
    my @allowhosts = @_;

    if (!-d "$eucalyptus/var/run/eucalyptus" || !-d "$eucalyptus/var/log/eucalyptus") {
	print "ERROR: eucalyptus root '$eucalyptus' not found\n";
	return(1);
    }
    if (!-d "$eucalyptus" || !-d "$dynpath" || !-e "$authz" || !-x "$apache") {
	print "ERROR: eucalyptus=$eucalyptus dynpath=$dynpath user=$user group=$group authz=$authz apache=$apache\n";
	return(1);
    }

    $allows = "127.0.0.0/8";
    foreach $host (@allowhosts) {
	$allows = $allows . " $host";
    }

    print OFH <<EOF;
ServerTokens OS
ServerRoot "$dynpath"
ServerName 127.0.0.1
Listen 8776
KeepAliveTimeout 30
PidFile $eucalyptus/var/run/eucalyptus/httpd-dynserv.pid
User $user
group $group
ErrorLog $eucalyptus/var/log/eucalyptus/httpd-dynserv-err.log
LogLevel warn
LoadModule authz_host_module $authz
DocumentRoot "$dynpath/data/"
<Directory "$dynpath/data/">
   Order deny,allow
#  Allow from 127.0.0.1
#  Allow from none
   Allow from $allows
   Deny from all
</Directory>
EOF
close(OFH);
return(0);
}

sub find_authz() {
    my @known_locations = ('/usr/lib64/httpd/modules/mod_authz_host.so',
			   '/usr/lib/httpd/modules/mod_authz_host.so',
			   '/usr/lib64/apache2/mod_authz_host.so',
			   '/usr/lib/apache2/mod_authz_host.so',
			   '/usr/lib/apache2/modules/mod_authz_host.so');

    $foundfile = "none";
    foreach $file (@known_locations) {
	if ( -f "$file" ) {
	    $foundfile = $file;
	}
    }
    return($foundfile);
}

sub find_apache2() {
    my @known_locations = ('/usr/sbin/apache2',
			   '/usr/sbin/httpd2',
			   '/usr/sbin/httpd');
    $foundfile = "none";
    foreach $file (@known_locations) {
	if ( -x "$file" ) {
	    $foundfile = $file;
	}
    }
    return($foundfile);
}

sub setup_dynpath() {
    my $root=shift @_;

    mkpath("$root/data/", {error => \my $err});
    if (@$err) {
	print "ERROR: could not create directory '$root/data'\n";
	return(1);
    }

    return(0);
}
