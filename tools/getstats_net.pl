#!/usr/bin/perl

# Copyright 2009-2012 Eucalyptus Systems, Inc.
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
# Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
# CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
# additional information or have any questions.
#
# This file may incorporate work covered under the following copyright
# and permission notice:
#
#   Software License Agreement (BSD License)
#
#   Copyright (c) 2008, Regents of the University of California
#   All rights reserved.
#
#   Redistribution and use of this software in source and binary forms,
#   with or without modification, are permitted provided that the
#   following conditions are met:
#
#     Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#
#     Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer
#     in the documentation and/or other materials provided with the
#     distribution.
#
#   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
#   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
#   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
#   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
#   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
#   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
#   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
#   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
#   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
#   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
#   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
#   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
#   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
#   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
#   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
#   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
#   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
#   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
#   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.

use Time::HiRes 'gettimeofday';
use Getopt::Std;

%chains = ( 'NetworkInExternal' => 'EUCA_COUNTERS_IN',
	    'NetworkOutExternal' => 'EUCA_COUNTERS_OUT');

# Two utility functions cribbed from getstats.pl
sub get_ts {
  my ($s, $usec) = gettimeofday();
  return ($s * 1000) + int ($usec / 1000);
}

sub print_stat {
  my ($dom, $ts, $dimension, $counter, $value) = @_;
  my $s = "\t";			# separator
  print $dom         . $s
    . $ts              . $s
      . $counter         . $s
	. "summation"      . $s
	  . $dimension       . $s
	    . $value           . "\n";
}

# Options:
# -a : Report aggregate (subnet) stats along with per-node stats.
# -m : Map IP addresses to instance IDs, if possible.
# -v : Be verbose--add some debug output.
getopts ('amv');

# Iterates through the desired iptables chains.
foreach my $counter (keys %chains) {
  my @counters;
  if (!open (IPT, "/sbin/iptables -L $chains{$counter} -v -x -n|")) {
    die "Can't run iptables: $!\n";
  }
  while (<IPT>) {
    # Pick off stats and source/destination addresses.
    next unless /^\s*\d+\s+(\d+)\s+all\s+--\s+\*\s+\*\s+((\d{1,3}\.){3}[0-9\/]{1,6})\s+((\d{1,3}\.){3}[0-9\/]{1,6})/;

    if ($2 == "0.0.0.0/0") {
      # Gathering stats on destination address.
      my $addr = $4;
      my $count = $1;

      if ($addr =~ /\/\d{1,2}/) {
	# Subnet stats, which are an aggregate stat.
	# (Note that this aggregate will likely contain stats for hosts
	# that are no longer present!)
	#
	# Don't care about them unless run with -a.
	if ($opt_a) {
	  ${$counter}{$addr} = $count;
        }
      } else {
	${$counter}{$addr} = $count;
      }
    } elsif ($4 == "0.0.0.0/0") {
      # Gathering stats on source address.
      my $addr = $2;
      my $count = $1;

      if ($addr =~ /\/\d{1,2}/) {
	# Subnet stats, which are an aggregate stat.
	# (Note that this aggregate will likely contain stats for hosts
	# that are no longer present!)
	#
	# Don't care about them unless run with -a.
	if ($opt_a) {
	  ${$counter}{$addr} = $count;
        }
      } else {
	${$counter}{$addr} = $count;
      }
    } else {
      # FIXME: send to a logfile?
      print STDERR "Unexpected addresses: $2\t$4\n";
    }
  }
  $time{$counter} = get_ts();
  close IPT;
}

if ($opt_m) {
  # Due to the && construct, this fails "politely"--without a die()
  # exit--if /root/eucarc is not found.
  if (!open (INST, "source /root/eucarc && euca-describe-instances |")) {
    die "Can't run euca-describe-instances: $!\n";
  }
  # Pick off addresses associated with each instance.
  while (<INST>) {
    next unless /^INSTANCE\s+(i-[^\s]+)\s+[^\s]+\s+(\d{1,3}\.){3}\d{1,3}\s+((\d{1,3}\.){3}\d{1,3})/;
    $instance_addr{$3} = $1;
  }
  # This is really only useful for debugging: prints IP-address to
  # instance map, one entry per line.
  if ($opt_v) {
    foreach $addr (keys %instance_addr) {
      print "IP address $addr maps to instance:\t$instance_addr{$addr}\n";
    }
    print "\n";
  }
}

foreach my $counter (keys %chains) {
  foreach my $block (keys %{$counter}) {
    if ($opt_m) {
      # List stats using mapped instances if run with -m. Requires
      # succesful run of euca-describe-instances.
      if ($instance_addr{$block}) {
	print_stat ($instance_addr{$block}, $time{$counter}, "default", $counter, ${$counter}{$block});
      } else {
	# Fallback for missing instance mapping is to output stats by IP
	# address.
	print_stat ($block, $time{$counter}, "default", $counter, ${$counter}{$block});
      }
    } else {
      print_stat ($block, $time{$counter}, "default", $counter, ${$counter}{$block});
    }
  }
}
