#!/usr/bin/perl

use strict;
use Getopt::Long;

my $USAGE = <<END;

This is a tool to help create the LDAP integration configuration.

Usage: lictool.pl <options>

Options:
  --password <password>      : set the password for result LIC
  --out      <output path>   : output to a file
  --passonly                 : print encrypted password only, no LIC template
  --custom <custom lic path> : use a custom LIC file instead of installed LIC template
  --nocomment                : remove comments from the LIC template

Examples:

# create a LIC template with encrypted password, print to STDOUT
\$ lictool.pl --password secret > example.lic

# create a LIC template with encrypted password, print to a file
\$ lictool.pl --password secret --out example.lic

# only print the encrypted password
\$ lictool.pl --password secret --passonly

# use an existing LIC and replace the credential field with encrypted password
\$ lictool.pl --password secret --custom my.lic --out mynew.lic

# create a LIC template with encrypted password and with comments removed
\$ lictool.pl --password secret --out example.lic --nocomment
END

sub print_usage {
  print $USAGE."\n";
  exit(1);
}

my $password = "";
my $outfile = "";
my $passonly = 0;
my $custom_template = "";
my $remove_comment = 0;

my $result = GetOptions("password=s" => \$password,
                        "out=s"      => \$outfile,
                        "passonly"   => \$passonly,
                        "custom=s"   => \$custom_template,
                        "nocomment"  => \$remove_comment);

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
my $PASS_LINE = qr/"auth-credentials"\s*:\s*".*"/;

my $input_file;
if ($custom_template eq "") {
  $input_file = $TEMPLATE;
} else {
  $input_file = $custom_template;
}
my @lic = ();
open TEMP, "<$input_file" or die "Can not open $input_file: $!";
while (<TEMP>) {
  if (/$PASS_LINE/) {
    s/$PASS_LINE/"auth-credentials":"$encrypted"/g;
  } elsif (/\"_comment\":/) {
    next if ($remove_comment);
  }
  push(@lic, $_);
}
close TEMP;

my $lic_out = join("", @lic);
if ($outfile eq "") {
  print $lic_out;
} else {
  open OUT, ">$outfile" or die "Can not open $outfile to write: $!";
  print OUT $lic_out;
  close OUT;
}
