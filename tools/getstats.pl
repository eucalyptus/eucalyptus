#!/usr/bin/perl

$id = shift @ARGV;
$blkdevstr = shift @ARGV;
$ifacestr = shift @ARGV;

$blkbytes = 0;
$ifbytes = 0;

@blkdevs = split(",", $blkdevstr);
@ifaces = split(",", $ifacestr);

foreach $blkdev (@blkdevs) {
    open(RFH, "virsh domblkstat $id $blkdev|");
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
    open(RFH, "virsh domifstat $id $iface|");
    while(<RFH>) {
	chomp;
	my $line = $_;
	if ($line =~ /rx_bytes (.*)/ || $line =~ /tx_bytes (.*)/) {
	    $ifbytes += $1;
	}
    }
    close(RFH);
}

print "OUTPUT $blkbytes $ifbytes\n";
