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

# inject.pl injects an executable or perl script into an image, then modifies the startup
#  of that image such that the executable or perl script will start automatically when
#  the image starts.

use strict;
use warnings;

if ($#ARGV < 1) {
	die "Usage: inject.pl image_file executable_file";
}

my $image_file = shift;
my $executable_file = shift;

sub cmd($);

mkdir("/mnt/image");
cmd("losetup /dev/loop6 $image_file");
cmd("mount /dev/loop6 /mnt/image");
cmd("cp $executable_file /mnt/image/etc/init.d");
chmod(755, "/mnt/image/etc/init.d/$executable_file");
cmd("chroot /mnt/image ln -s /usr/bin/$executable_file /etc/rc3.d/S99$executable_file");
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
