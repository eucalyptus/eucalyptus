#!/usr/bin/perl

#
# inject.pl injects an executable or perl script into an image, then modifies the startup
#  of that image such that the executable or perl script will start automatically when
#  the image starts.
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
# author: tom.werges
#

use strict;
use warnings;

if ($#ARGV < 1) {
	die "Usage: inject.pl image_file executable_file";
}

my $image_file = shift;
my $executable_file = shift;

sub cmd($);

mkdir("/tmp/image");
cmd("losetup /dev/loop6 $image_file");
cmd("mount /dev/loop6 /tmp/image");
cmd("cp $executable_file /tmp/image/usr/bin");
symlink("/tmp/image/usr/bin","/etc/init.d/"); #Fix this
cmd("umount /dev/loop6");
cmd("losetup -d /dev/loop6");

exit(0);

sub cmd($) {
	print "Running command:$_[0]:";
	my $return_code = (system($_[0])/256);
	if ($return_code==0) {
		print "passed\n";
		return;
	} else {
		print "FAILED\n";
		exit(127);
	}
}

