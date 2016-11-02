#!/usr/bin/perl
#
# Script to authorize/deauthorize remote libvirtd clients for TLS migration of instances between NC nodes.
#
# (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
# Run on NC. Usage:
#
# authorize-migration-keys.pl [-v] [-r] [-c config-file] [-a client secret | -D]
#
# Where:
#
# authorize-migration-keys.pl -a client secret
#   ...will authorize the specified migration client (IP address) using
#   'secret' as the shared secret and overwriting any previously saved
#   shared secret for this client.
#
# authorize-migration-keys.pl -D
#   ...will deauthorize all migration clients.
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

@DN_order_1 = ('C', 'O', 'L', 'CN');
@DN_order_2 = ('CN', 'O', 'L', 'C');

# printf for debug (verbose) output.
sub dprint
{
  print @_ if defined $opt_v;
}

sub usage
{
  print STDERR "$0 [-v] [-r] [-c config-file] [-a client secret | -D]\n";
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

  foreach (@DN_order_1) {
    $DN_1 .= "$_=$DN{$_},";
  }
  $DN_1 =~ s/,$//;
  foreach (@DN_order_2) {
    $DN_2 .= "$_=$DN{$_},";
  }
  $DN_2 =~ s/,$//;

  return ($DN{CN}, $DN_1, $DN_2);

}

sub process_dn_list
{
  chomp @_;

  my $dn_list = join (' ', @_);

  $dn_list =~ /\[(.*)\]/;
  my $dn_entries = $1;

  my @dn_list = split ('", ', $dn_entries);

  foreach (@dn_list) {
    s/^\s*"//;
    s/",*\s*$//;
  }

  return @dn_list;
}

sub save_new_dn_list
{
  my $header_printed = 0;
  my $hash_line = 0;
  my @existing_dn = @{$_[0]};
  my @new_dn;

  if ($opt_D) {
    dprint "Removing ALL entries from tls_allowed_dn_list\n";
    print CONFIG_OUT "tls_allowed_dn_list = []\n";
    return;
  }

  if ($opt_a) {
    my ($dn_key, $dn1, $dn2) = generate_new_dn($opt_a, $ARGV[0]);

    foreach my $element (@existing_dn) {
      dprint "Processing $element\n";
      if ($element =~ /CN=([\d\.]*)/) {
        if ($1 ne $dn_key) {
          push(@new_dn, $element);
        } else {
          dprint "Existing entry for '$dn_key' -- replacing it\n";
        }
      }
    }

    dprint "New entry '$dn1' added\n";
    push(@new_dn, $dn1);
    dprint "New entry '$dn2' added\n";
    push(@new_dn, $dn2);
  }

  $new_size = scalar @new_dn;
  if ($new_size == 0) {
    print CONFIG_OUT "tls_allowed_dn_list = []\n";
    return;
  }

  foreach (@new_dn) {
    if (!$header_printed) {
      print CONFIG_OUT "tls_allowed_dn_list = [\"$_\",";
      ++$header_printed;
      if (++$hash_line == $new_size) {
	print CONFIG_OUT "]\n";
      } else {
	print CONFIG_OUT "\n";
      }
    } else {
      print CONFIG_OUT "                       \"$_\",";
      if (++$hash_line == $new_size) {
	print CONFIG_OUT "]\n";
      } else {
	print CONFIG_OUT "\n";
      }
    }
  }
}

# Main program begins here.

getopts ('a:vDc:r');

if ( ($opt_D != 1) and ($ARGV[0] and ($opt_a eq '')) ) {
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
      my (@DNs) = process_dn_list(@dn_list);
      save_new_dn_list(\@DNs);
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
      my (@DNs) = process_dn_list(@dn_list);
      save_new_dn_list(\@DNs);
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

  # Did the files actually change?
  `diff -q $config_file $backup_config`;
  if (($?>>8) == 0)
  {
     dprint "No configuration file change detected, exiting without libvirtd restart\n";
     exit 0;
  } 

  # Using backticks rather than system() so as to not clutter output if this script is called from an init script.
  `/sbin/service libvirtd restart`;

  # Make sure that libvirtd comes back up, sometimes it takes a while to recover
  $connected = 0;
  for my $i (0..20)
  {
     `virsh connect`;
     if (($?>>8) == 0)
     {
        dprint "Connected to hypervisor\n";
         $connected = 1;
         last;
     }
     else 
     {
       dprint "Can't connect to hypervisor attempt: $i\n";
       sleep 1;
     }
  }
  if ($connected == 0) 
  { 
     print STDERR "Could not connect to the hypervisor\n";
     exit 1; 
  }
}

exit 0;
