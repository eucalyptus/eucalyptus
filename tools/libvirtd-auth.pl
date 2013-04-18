#!/usr/bin/perl

# Run on NC. Usage:
#
# libvirtd-auth.pl [-v] [-c config-file] [-a client secret | -r client | -z]
#
# Where:
#
# libvirtd-auth.pl -a client secret
#   ...will authorized the specified migration client (IP address) using 'secret' as the shared secret.
#
# libvirtd-auth.pl -z
#   ...will revoke all migration clients.
#
# libvirtd-auth.pl -r client
#   ...will revoke only the specified migration client (IP address).
#
# All usage cases accept [-c config-file] to specify a fully-qualified path
# to the libvirtd.conf file. If this is not specified, the default path of
# /etc/libvirt/libvirtd.conf will be used.
#
# All usage cases also accept [-v] as a "verbose" flag.

use Getopt::Std;

$config_file = "/etc/libvirt/libvirtd.conf";
$keypath = "$ENV{'EUCALYPTUS'}/var/lib/eucalyptus/keys/cluster-cert.pem";
$certtool = "certtool -i --infile";

#%DNs = ();
@DN_order = ('C', 'O', 'L', 'CN');

# printf for debug (verbose) output.
sub dprint
{
  print @_ if defined $opt_v;
}

sub usage
{
  print STDERR "$0 [-v] [-c config-file] [-a client secret | -r client | -z]\n";
}

sub generate_new_dn
{
  my $DN;
  my %DN = ();
  $DN{CN} = $_[0];
  $DN{L} = $_[1];

  $cert = `$certtool $keypath | grep Subject:`;
  $cert =~ s/^\s+Subject:\s*//;

  my @cert = split(',', $cert);
  my $c_CN;
  my $c_C;

  foreach (@cert) {
    if (/^CN=(.*)/) {
      # CN in the signing cert becomes O in the DN.
      $DN{O} = $1;
    } elsif (/^C=(.*)/) {
      $DN{C} = $1;
    }
  }

  if ($DN{O} and $DN{C}) {
    dprint "Constructing a new DN entry for '$DN{CN}' : '$DN{L}' ...\n"
  } else {
    # This is reason to fail the migration!
    print STDERR "Cannot construct a new DN entry for '$DN{CN}' : '$DN{L} ... exiting'\n";
    exit 1;
  }

  foreach (@DN_order) {
    $DN .= "$_=$DN{$_},";
  }
  $DN =~ s/,$//;

  return ($DN{CN}, $DN);
}

sub process_dn_list
{
  chomp @_;

  my %DNs = ();
  my $dn_list = join (' ', @_);

  $dn_list =~ /\[(.*)\]/;
  my $dn_entries = $1;

  my @dn_list = split ('", ', $dn_entries);

  foreach (@dn_list) {
    s/^\s*"//;
    s/",*\s*$//;
  }

  foreach (@dn_list) {
    my $dn;
    my @dn_fields;
    my @dn = split (',');
    foreach (@dn) {
      if (/^CN=.*/) {
	$dn = $_;
	push @dn_fields, $_;
      } else {
	push @dn_fields, $_;
      }
    }
    if ($dn) {
      $DNs{$dn} = join (',', @dn_fields);
    }
  }

  return %DNs;
}

sub save_new_dn_list
{
  my $header_printed = 0;
  my $hash_line = 0;
  my (%DNs) = @_;

  if ($opt_z) {
    dprint "Removing ALL entries from tls_allowed_dn_list\n";
    print CONFIG_OUT "tls_allowed_dn_list = []\n";
    return;
  }

  if ($opt_r) {
    my $val = delete $DNs{"CN=" . $opt_r};
    if (!$val) {
      print STDERR "Did not find key '$opt_r' in tls_allowed_dn_list\n";
    } else {
      dprint "Removed '$opt_r' from tls_allowed_dn_list\n";
    }
  } elsif ($opt_a) {
    my ($dn_key, $dn) = generate_new_dn($opt_a, $ARGV[0]);

    if ($DNs{"CN=$dn_key"}) {
      print STDERR "Duplicate entry '$dn_key' -- not adding\n";
    } else {
      dprint "New entry '$dn' added\n";
      $DNs{"_"} = $dn;
    }
  }

  my $hash_size = keys %DNs;

  if ($hash_size == 0) {
    print CONFIG_OUT "tls_allowed_dn_list = []\n";
    return;
  }

  foreach (keys %DNs) {
    if (!$header_printed) {
      print CONFIG_OUT "tls_allowed_dn_list = [\"$DNs{$_}\",";
      ++$header_printed;
      if (++$hash_line == $hash_size) {
	print CONFIG_OUT "]\n";
      } else {
	print CONFIG_OUT "\n";
      }
    } else {
      print CONFIG_OUT "                       \"$DNs{$_}\",";
      if (++$hash_line == $hash_size) {
	print CONFIG_OUT "]\n";
      } else {
	print CONFIG_OUT "\n";
      }
    }
  }
}

# Main program begins here.

getopts ('a:vzr:c:');

$opts = 0;

++$opts if $opt_a;
++$opts if $opt_z;
++$opts if $opt_r;

if (($opts != 1) or (($opt_a ne '') and ($ARGV[0] eq '')) or ($ARGV[0] and ($opt_a eq ''))) {
  usage();
  exit(1);
}

if ($opt_c) {
  $config_file = $opt_c;
}

dprint "Using config file: $config_file\n";

$new_config = $config_file . ".new";
$orig_config = $config_file . ".orig";

open (CONFIG_IN, "<$config_file") or
  die "Cannot open existing config file ($config_file): $!\n";

# Back up original (distribution) file; will silently fail if it has already been backed up.
my $linkret = link ($config_file, $orig_config);
if ($linkret) {
  dprint "Could not link to '$orig_config': $!\n";
}

# Eliminate any stragglers. Without remorse.
unlink ($new_config);
open (CONFIG_OUT, ">$new_config") or
  die "Cannot open new config file ($new_config): $!\n";

while (<CONFIG_IN>) {
  if (/^[#]*\s*tls_allowed_dn_list\s*=\s*/ and !$in_dn_list) {
    print STDERR "Multiple tls_allowed_dn_list entries found?" if $dn_list_found;

    # Check for terminating bracket.
    if (/.*\]/) {
      # Dealing with only a single line--easy.
      my @dn_list;
      push @dn_list, $_;
      my (%DNs) = process_dn_list(@dn_list);
      save_new_dn_list(%DNs);
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
      my (%DNs) = process_dn_list(@dn_list);
      save_new_dn_list(%DNs);
      undef @dn_list;
      ++$dn_list_found;
      $in_dn_list = 0;
    }
  } else {
    # Remainder of config file--before and after tls_allowed_dn_list entry.
    print CONFIG_OUT $_;
  }
}

# Move new file into place.
# FIXME: add error-checking or all file operations: link, unlink, rename..
unlink ($config_file);
rename ($new_config, $config_file);
