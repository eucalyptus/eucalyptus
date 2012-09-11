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
use Cwd 'abs_path';
use Sys::Virt;
use XML::Simple;

open DISKSTATS, "cat /proc/diskstats |";
my @diskstats = <DISKSTATS>; # slurp in all the disk stats into an array
my $disk_ts = get_ts(); # record the timestamp of when 'cat' returned
close DISKSTATS;

# populate a dictionary with statistics about all disk devices on the system
my %diskstats;
foreach my $disk_stat (@diskstats) {
    $disk_stat =~ s/^\s*(.*)\s*$/$1/; # trim whitespace before and after a line
    my %d; # struct to contain disk stats
    ($d{dev_maj}, 
     $d{dev_min},
     $d{dev_name},     # Explanations from http://www.kernel.org/doc/Documentation/iostats.txt
     $d{reads_comp},   #  1) This is the total number of reads completed successfully
     $d{reads_merg},   #  2) Number of reads merged (Reads and writes which are adjacent to each other may be merged for efficiency)
     $d{sect_read},    #  3) This is the total number of sectors read successfully
     $d{mill_read},    #  4) This is the total number of milliseconds spent by all reads
     $d{writes_comp},  #  5) This is the total number of writes completed successfully
     $d{writes_merg},  #  6) Number of writes merged
     $d{sect_written}, #  7) This is the total number of sectors written successfully
     $d{mill_written}, #  8) This is the total number of milliseconds spent by all writes
     $d{ios_progress}, #  9) Number of I/Os currently in progress. The only field that should go to zero.
     $d{mill_io},      # 10) Number of milliseconds spent doing I/Os
     $d{mill_io_w}     # 11) Weighted # of milliseconds spent doing I/Os
    ) = split (/\s+/, $disk_stat);
    $diskstats{$d{dev_name}} = \%d;
}

my $BYTES_PER_SECTOR = 512;
my $MILLS_PER_SECOND = 1000;
my $vmm = Sys::Virt->new();
my @domains = $vmm->list_domains();
foreach my $dom (@domains) {

    # according to http://people.redhat.com/~rjones/virt-top/faq.html
    # this is nanoseconds of CPU time used by the domain since the domain booted
    # multiple samples can be used by the caller of this script to calculate a
    # running average using the technique at the bottom of the URL
    #   %CPU = 100 x cpu_time_diff_nano / (time_interval_seconds x nr_cores x 10^9)
    print_stat ($dom, get_ts(), "default", "CPUUtilization", $dom->get_info()->{cpuTime}); 

    my $xml_text = $dom->get_xml_description($flags=0);
    my $xml_struct = XMLin($xml_text, ForceArray => [ 'disk', 'interface' ]); # parse domain's XML dump to find disks and NICs

    # iterate over NICs and add up rx and tx byte counters
    my $rx_bytes = 0;
    my $tx_bytes = 0;
    foreach my $nic (@{$xml_struct->{devices}->{interface}}) {
	my $guest_nic = $nic->{target}->{dev};
	$rx_bytes += $dom->interface_stats($guest_nic)->{rx_bytes};
	$tx_bytes += $dom->interface_stats($guest_nic)->{tx_bytes};
    }
    my $ts = get_ts();
    print_stat ($dom, $ts, "total", "NetworkIn",  $rx_bytes);
    print_stat ($dom, $ts, "total", "NetworkOut", $tx_bytes);

    # iterate over disks of the instance and pull out relevant stats from the %diskstats dictionary
    foreach my $disk (@{$xml_struct->{devices}->{disk}}) {
	my $dev_path = abs_path($disk->{source}->{dev});
	if (index ($dev_path, "/dev/") != 0) { next }
	my $dev_name = substr ($dev_path, 5);
	my $stats = $diskstats{$dev_name};
	my $dim = $disk->{target}->{dev};
	if (defined $stats) {
	    print_stat ($dom, $disk_ts, $dim, "DiskReadOps",          $stats->{reads_comp});
	    print_stat ($dom, $disk_ts, $dim, "DiskWriteOps",         $stats->{writes_comp});
	    print_stat ($dom, $disk_ts, $dim, "DiskReadBytes",        $stats->{sect_read} * $BYTES_PER_SECTOR);
	    print_stat ($dom, $disk_ts, $dim, "DiskWriteBytes",       $stats->{sect_written} * $BYTES_PER_SECTOR);
	    print_stat ($dom, $disk_ts, $dim, "VolumeTotalReadTime",  $stats->{mill_read} / $MILLS_PER_SECOND);
	    print_stat ($dom, $disk_ts, $dim, "VolumeTotalWriteTime", $stats->{mill_written} / $MILLS_PER_SECOND);
	}
    }
}

sub get_ts {
    my ($s, $usec) = gettimeofday();
    return ($s * 1000) + $usec;
}

sub print_stat {
    my ($dom, $ts, $dimension, $counter, $value) = @_;
    my $s = "\t"; # separator
    print $dom->get_name() . $s
	. $ts              . $s
	. $counter         . $s 
	. "summation"      . $s 
	. $dimension       . $s
	. $value           . "\n";
}
