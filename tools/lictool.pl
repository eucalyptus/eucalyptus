#!/usr/bin/perl

use strict;
use Getopt::Long;

my $USAGE = <<END;

This is a tool to help create the LDAP integration configuration.
Examples:
\$ lictool.pl --password secret > example.lic      # create a LIC template with encrypted password
\$ lictool.pl --password secret --out example.lic  # create a LIC template with encrypted password
\$ lictool.pl --password secret --passonly         # only print the encrypted password

Usage: lictool.pl <options>

Options:
  --password <password>      : set the password for result LIC
  --out      <output path>   : output to a file
  --passonly                 : print encrypted password only, no LIC template
  --custom <custom lic path> : use a custom LIC file. Note: the tool looks for
                               'ENCRYPTED_PASSWORD' as the placeholder for 'auth-credentials'
                               value.
END

sub print_usage {
  print $USAGE."\n";
  exit(1);
}

my $password = "";
my $outfile = "";
my $passonly = 0;
my $custom_template = "";

my $result = GetOptions("password=s" => \$password,
                        "out=s"      => \$outfile,
                        "passonly"   => \$passonly,
                        "custom=s"   => \$custom_template);

if ($password eq "" or not $result) {
  print_usage;
}

my $EUCALYPTUS = $ENV{'EUCALYPTUS'};
if ($EUCALYPTUS eq "") {
  $EUCALYPTUS = "/";
}

my $FORMAT = "RSA/ECB/PKCS1Padding";
my $CERT = "$EUCALYPTUS/var/lib/eucalyptus/keys/cloud-cert.pem";
my $encrypted = `echo -n $password | openssl rsautl -encrypt -inkey $CERT -certin | openssl base64 | tr -d '\n'`;
$encrypted = "{$FORMAT}".$encrypted;

if ($passonly) {
  print $encrypted;
  exit(0);
}

my $TEMPLATE = "$EUCALYPTUS/usr/share/eucalyptus/lic_template";
my $PASSWORD_PLACEHOLDER = "ENCRYPTED_PASSWORD";

my $input_file;
if ($custom_template eq "") {
  $input_file = $TEMPLATE;
} else {
  $input_file = $custom_template;
}
open TEMP, "<$input_file" or die "Can not open $input_file: $!";
my $lic = do { local $/; <TEMP> };
close TEMP;

$lic =~ s/$PASSWORD_PLACEHOLDER/$encrypted/g;

if ($outfile eq "") {
  print $lic;
} else {
  open OUT, ">$outfile" or die "Can not open $outfile to write: $!";
  print OUT $lic;
  close OUT;
}
