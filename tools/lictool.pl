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
  --out      <output file>   : output to a file
  --passonly                 : print encrypted password only, no LIC template
END

sub print_usage {
  print $USAGE."\n";
  exit(1);
}

my $password = "";
my $outfile = "";
my $passonly = 0;

my $result = GetOptions("password=s" => \$password,
                        "out=s"      => \$outfile,
                        "passonly"   => \$passonly);

if ($password eq "" or not $result) {
  print_usage;
}

my $EUCALYPTUS = $ENV{'EUCALYPTUS'};
if ($EUCALYPTUS eq "") {
  $EUCALYPTUS = "/";
}

my $CERT = "$EUCALYPTUS/var/lib/eucalyptus/keys/cloud-cert.pem";
my $encrypted = `echo -n $password | openssl rsautl -encrypt -inkey $CERT -certin | openssl base64 | tr -d '\n'`;

if ($passonly) {
  print $encrypted;
  exit(0);
}

my $TEMPLATE = "$EUCALYPTUS/usr/share/eucalyptus/lic_template";

open TEMP, "<$TEMPLATE" or die "Can not open $TEMPLATE: $!";
my $lic = do { local $/; <TEMP> };
close TEMP;

$lic =~ s/ENCRYPTED_PASSWORD/$encrypted/g;

if ($outfile eq "") {
  print $lic;
} else {
  open OUT, ">$outfile" or die "Can not open $outfile to write: $!";
  print OUT $lic;
  close OUT;
}
