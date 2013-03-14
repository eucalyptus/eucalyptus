#!/usr/bin/perl

# Run on NC. Usage:
#
# libvirtd-auth.pl [-v] [-c config-file] [-r] [DN]
#
# Where:
#
# libvirtd-auth.pl DN
#   ...will authorized the specified migration client (DN).
#
# libvirtd-auth.pl -r
#   ...will revoke all migration clients.
#
# libvirtd-auth.pl -r DN
#   ...will revoke only the specified migration client (DN).
#
# All usage cases accept [-c config-file] to specify a fully-qualified path
# to the libvirtd.conf file. If this is not specified, the default path of
# /etc/libvirt/libvirtd.conf will be used.
#
# All usage cases also accept [-v] as a "verbose" flag.

use Getopt::Std;

$config_file = "/etc/libvirt/libvirtd.conf";

%DNs = ();

# printf for debug (verbose) output.
sub dprint
{
  print @_ if defined $opt_v;
}

sub process_dn_list
{
#  print @_;
  chomp @_;

  my $dn_list = join (' ', @_);

#  print $dn_list;

  $dn_list =~ /\[(.*)\]/;
  my $dn_entries = $1;

#  print "\n\n$1\n\n";

  my @dn_list = split ('", ', $dn_entries);

  foreach (@dn_list) {
    s/^\s*"//;
    s/"\s*$//;
  }

  print join ("\n", @dn_list);
  print "\n";

  foreach (@dn_list) {
    my $dn;
    my @dn_fields;
    my @dn = split (',');
    foreach (@dn) {
      if (/^CN=.*/) {
	$dn = $_;
      } else {
	push @dn_fields, $_;
      }
    }
    if ($dn) {
      $DNs{$dn} = join (',', @dn_fields);
    }
  }

  foreach (keys %DNs) {
    print "$_,$DNs{$_}\n";
  }


  my $foo = <STDIN>
}

getopts ('vrc:');

if ($opt_c) {
  $config_file = $opt_c;
}

dprint "Using config file: $config_file\n";

$new_config = $config_file . ".new";

open (CONFIG_IN, "<$config_file") or
  die "Cannot open existing config file ($config_file): $!\n";

# Eliminate any stragglers. Without remorse.
unlink ($new_config);
open (CONFIG_OUT, ">$new_config") or
  die "Cannot open new config file ($new_config): $!\n";

while (<CONFIG_IN>) {
  if (/^[#]*\s*tls_allowed_dn_list\s*=\s*/ and !$in_dn_list) {
    dprint "Multiple tls_allowed_dn_list entries found?" if $dn_list_found;

    # Check for terminating bracket.
    if (/.*\]/) {
      # Dealing with only a single line--easy.
      my @dn_list;
      push @dn_list, $_;
      process_dn_list(@dn_list);
      ++$dn_list_found;
    } else {
      # Entries spread over multiple lines--not as easy.
      #
      # First line of entry.
      undef @dn_list;
      push @dn_list, $_;
      ++$in_dn_list;
    }
  } elsif ($in_dn_list) {
    # Continuation of multi-line entry.
    push @dn_list, $_;
    # Check for terminating bracket.
    if (/.*\]/) {
      # Found. Multi-line entry complete--finish & process.
      process_dn_list(@dn_list);
      undef @dn_list;
      ++$dn_list_found;
      $in_dn_list = 0;
    }
  } else {
    print $_;
  }
}
