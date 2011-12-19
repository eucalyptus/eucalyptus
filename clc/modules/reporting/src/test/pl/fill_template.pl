#!/usr/bin/perl

#
# fill_template.pl generates content based upon a template file. It scans through the
#  template file, substituting any variables, and then outputs the result.
#
# Variables within the template file take the format of [VARIABLE_NAME] and are
#  substituted according to the VAR_NAME=VALUE parameters passed to this command.
#
# For example, if you ran: "./fill_template.pl my_file.template FOO=bar", then this script
#  would read the file "my_file.template", replace every occurence of [FOO] with "bar",
#  and output the results.
#
# This scipt is used to _generate_ a perl script, which is then bundled within an image
#  and run automatically upon image startup. This allows the test scripts to control what
#  the image will do upon running it. See: use_resources.template
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
# author: tom.werges
#

use strict;
use warnings;

if ($#ARGV < 0) {
	die "Usage: fill_template.pl template_file (var=val)+\n";
}

my $template_file = shift;
my %template_vals = ();
while ($#ARGV+1>0) {
	my ($key, $val) = split("=",shift);
	$template_vals{$key}=$val;
}

open(INFILE, $template_file) or die("couldn't open $template_file");
while (<INFILE>) {
	if (/^# .*/) {
		next;
	}
	foreach my $k (keys(%template_vals)) {
		s/\[$k\]/$template_vals{$k}/;
	}
	print $_;
}
close(INFILE);
exit(0);

