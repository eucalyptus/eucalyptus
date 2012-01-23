#!/usr/bin/perl

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
