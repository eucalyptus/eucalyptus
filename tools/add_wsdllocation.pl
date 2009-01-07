#!/usr/bin/perl

#$fname = shift @ARGV || die "USAGE: add_wsdllocation.pl <service.xml> <wsdl_path>\n";
$wsdlpath = shift @ARGV || die "USAGE: cat <service.xml> | add_wsdllocation.pl <wsdl_path>\n";

$found=0;
while(<STDIN>) {
    my $orig = $_;
    my $line = $orig;
    chomp $line;
    $added = "";
    if ($found == 0) {
	if ($line =~ /^<service name=/) {
	    $added = "<parameter name=\"wsdl_path\" locked=\"xsd:false\">$wsdlpath</parameter>\n";
	}
    }
    
    print ("$orig");
    print ("$added");

}
exit 0;
