#!/usr/bin/perl
#
# Script to authorize/deauthorize remote libvirtd clients for TLS migration of instances between NC nodes.
#
# Copyright 2013 Eucalyptus Systems, Inc.
#
# This Program Is Free Software: You Can Redistribute It And/Or Modify
# It Under The Terms Of The Gnu General Public License As Published By
# The Free Software Foundation; Version 3 Of The License.
#
# This Program Is Distributed In The Hope That It Will Be Useful,
# But Without Any Warranty; Without Even The Implied Warranty Of
# Merchantability Or Fitness For A Particular Purpose.  See The
# Gnu General Public License For More Details.
#
# You Should Have Received A Copy Of The Gnu General Public License
# Along With This Program.  If Not, See Http://Www.Gnu.Org/Licenses/.
#
# Please Contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
# Ca 93117, Usa Or Visit Http://Www.Eucalyptus.Com/Licenses/ If You Need
# Additional Information Or Have Any Questions.
#
#
# Run on NC. Usage:
#
# authorize-migration-keys.pl [-v] [-r] [-c config-file] [-a client secret | -A client secret | -d client | -D]
#
# Where:
#
# authorize-migration-keys.pl -a client secret
#   ...will authorize the specified migration client (IP address) using 'secret' as the shared secret and overwriting any previously saved shared secret for this client.
# authorize-migration-keys.pl -A client secret
#   ...will authorize the specified migration client (IP address) using 'secret' as the shared secret. If a shared secret was previously saved for this client, it will left in place and NOT overwritten.
#
# authorize-migration-keys.pl -D
#   ...will deauthorize all migration clients.
#
# authorize-migration-keys.pl -d client
#   ...will deauthorize only the specified migration client (IP address).
#
# All usage cases accept [-c config-file] to specify a fully-qualified path
# to the libvirtd.conf file. If this is not specified, the default path of
# /etc/libvirt/libvirtd.conf will be used.
#
# All usage cases also accept [-v] as a "verbose" flag and [-r] as a flag to force a restart of libvirtd
# after modification of the libvirtd.conf file.

use Getopt::Std;
use File::Copy;

$config_file = "/etc/libvirt/libvirtd.conf";
$keypath = "$ENV{'EUCALYPTUS'}/var/lib/eucalyptus/keys/node-cert.pem";
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
  print STDERR "$0 [-v] [-r] [-c config-file] [-a client secret | -A client secret | -d client | -D]\n";
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
    print STDERR "Cannot construct a new DN entry for '$DN{CN}' : '$DN{L}' ... exiting'\n";
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

  if ($opt_D) {
    dprint "Removing ALL entries from tls_allowed_dn_list\n";
    print CONFIG_OUT "tls_allowed_dn_list = []\n";
    return;
  }

  if ($opt_d) {
    my $val = delete $DNs{"CN=" . $opt_d};
    if (!$val) {
      print STDERR "Did not find key '$opt_d' in tls_allowed_dn_list\n";
    } else {
      dprint "Removed '$opt_d' from tls_allowed_dn_list\n";
    }
  } elsif ($opt_a) {
    my ($dn_key, $dn) = generate_new_dn($opt_a, $ARGV[0]);

    if ($DNs{"CN=$dn_key"}) {
      dprint "Existing entry for '$dn_key' -- replacing it with '$dn'\n";
      $DNs{"CN=$dn_key"} = $dn;
    } else {
      dprint "New entry '$dn' added\n";
      $DNs{"_"} = $dn;
    }
  } elsif ($opt_A) {
    my ($dn_key, $dn) = generate_new_dn($opt_A, $ARGV[0]);

    if ($DNs{"CN=$dn_key"}) {
      print STDERR "Existing entry for '$dn_key' -- not adding\n";
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

getopts ('a:A:vDd:c:r');

$opts = 0;

++$opts if $opt_a;
++$opts if $opt_A;
++$opts if $opt_D;
++$opts if $opt_d;

if (($opts != 1) or ((($opt_a ne '') or ($opt_A ne '')) and ($ARGV[0] eq '')) or ($ARGV[0] and (($opt_a eq '') and ($opt_A eq '')))) {
  usage();
  exit 1;
}

if ($opt_c) {
  $config_file = $opt_c;
}

dprint "Using config file: $config_file\n";

$new_config = $config_file . ".new";
$orig_config = $config_file . ".orig";
$backup_config = $config_file . ".bak";

open (CONFIG_IN, "<$config_file") or
  die "Cannot open existing config file ($config_file): $!\n";

# Back up original (distribution) file; will silently fail if it has already been backed up.
my $linkret = link ($config_file, $orig_config);
if ($linkret) {
  dprint "Could not link to '$orig_config': $!\n";
}

# Eliminate any straggler backup files.
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
move ($config_file, $backup_config);
rename ($new_config, $config_file);

# Now restart libvirtd if called with -r.
if ($opt_r) {
  # Using backticks rather than system() so as to not clutter output if this script is called from an init script.
  `/sbin/service libvirtd restart`;
}

exit 0;
