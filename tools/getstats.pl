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

use Getopt::Std;

my $blkbytes = 0;
my $ifbytes = 0;

getopts('i:b:n:', \%opts);
my $id = $opts{'i'};
my $blkdevstr = $opts{'b'};
my $ifacestr = $opts{'n'};

my @blkdevs = split(",", $blkdevstr);
my @ifaces = split(",", $ifacestr);

foreach $blkdev (@blkdevs) {
#    $blkdev =~ s/\d*$//;
    $blks{$blkdev} = 1;
}
foreach $blkdev (keys((%blks))) {
    open(RFH, "virsh domblkstat $id $blkdev 2>/dev/null|");
    while(<RFH>) {
	chomp;
	my $line = $_;
	if ($line =~ /rd_bytes (.*)/ || $line =~ /wr_bytes (.*)/) {
	    $blkbytes += $1;
	}
    }
    close(RFH);
}

foreach $iface (@ifaces) {
    open(RFH, "virsh domifstat $id $iface 2>/dev/null |");
    while(<RFH>) {
	chomp;
	my $line = $_;
	if ($line =~ /rx_bytes (.*)/ || $line =~ /tx_bytes (.*)/) {
	    $ifbytes += $1;
	}
    }
    close(RFH);
}

if ($blkbytes) {
    $blkmbytes = int($blkbytes / (1<<20));
} else {
    $blkmbytes = 0;
}
if ($ifbytes) {
    $ifmbytes = int($ifbytes / (1<<20));
} else {
    $ifmbytes = 0;
}
print "OUTPUT $blkmbytes $ifmbytes\n";
