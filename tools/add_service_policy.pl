#!/usr/bin/perl

$policypath = shift @ARGV || die "USAGE: cat <service.xml> | add_server_policy.pl <policy_path>\n";

$found=0;
while(<STDIN>) {
    my $orig = $_;
    my $line = $orig;
    chomp $line;
    $added = "";
    if ($found == 0) {
	if ($line =~ /^\s*<\/service>/) {
	    $added = "<module ref=\"rampart\"/>\n";
	    $added .= `cat $policypath | sed "s:EUCALYPTUS_HOME:\$EUCALYPTUS:g" | sed "s:AXIS2C_HOME:\$AXIS2C_HOME:g"`;
	}
    }
    
    print ("$added");
    print ("$orig");


}
exit 0;
