#!/usr/bin/perl
#Copyright (c) 2009  Eucalyptus Systems, Inc.	
#
#This program is free software: you can redistribute it and/or modify
#it under the terms of the GNU General Public License as published by 
#the Free Software Foundation, only version 3 of the License.  
# 
#This file is distributed in the hope that it will be useful, but WITHOUT
#ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
#FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
#for more details.  
#
#You should have received a copy of the GNU General Public License along
#with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
#Please contact Eucalyptus Systems, Inc., 130 Castilian
#Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
#if you need additional information or have any questions.
#
#This file may incorporate work covered under the following copyright and
#permission notice:
#
#  Software License Agreement (BSD License)
#
#  Copyright (c) 2008, Regents of the University of California
#  
#
#  Redistribution and use of this software in source and binary forms, with
#  or without modification, are permitted provided that the following
#  conditions are met:
#
#    Redistributions of source code must retain the above copyright notice,
#    this list of conditions and the following disclaimer.
#
#    Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in the
#    documentation and/or other materials provided with the distribution.
#
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
#  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
#  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
#  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
#  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
#  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
#  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
#  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
#  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
#  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
#  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
#  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
#  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
#  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
#  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
#  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
#  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
#  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
#  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
#  ANY SUCH LICENSES OR RIGHTS.
#

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

#$MOUNT=untaint(`which mount`);
#$UMOUNT=untaint(`which umount`);
#$MOUNT=untaint("/tmp/euca_mountwrap");
#$UMOUNT=untaint("/tmp/euca_mountwrap");
$MKDIR=untaint(`which mkdir`);
$RMDIR=untaint(`which rmdir`);
$CHOWN=untaint(`which chown`);
$CHMOD=untaint(`which chmod`);
$MKTEMP=untaint(`which mktemp`);
$TUNE2FS=untaint(`which tune2fs`);
$LOSETUP=untaint(`which losetup`);

# check binaries
if (!-x $MKDIR || !-x $RMDIR || !-x $CHOWN || !-x $CHMOD || !-x $MKTEMP || !-x $LOSETUP) {
    print STDERR "add_key cannot find all required binaries\n";
    do_exit(1);
}

# check input params
$mounter = untaint(shift @ARGV);
$offset = untaint(shift @ARGV);
$img = untaint(shift @ARGV);
$key = shift @ARGV; # untaint later

$tmpfile = "";
$loopdev = "";
$mounted = 0;
$attached = 0;

if (!-f "$img" || !-x "$mounter") {
    print STDERR "add_key cannot verify inputs: mounter=$mounter img=$img\n";
    do_exit(1);
}
if ($offset eq "") {
    $offset = 0;
}

chomp($tmpfile = untaint(`$MKTEMP -d`));
if (! -d "$tmpfile") {
    print STDERR "no dir: $tmpfile";
    do_exit(1);
}

# find loop dev and attach image to it
for ($i=0; $i<10 && !$attached; $i++) {
    $loopdev=untaint(`$LOSETUP -f`);
    if ($loopdev ne "") {
	if ($offset == 0) {
	    $rc = system("$LOSETUP $loopdev $img");
	} else {
	    $rc = system("$LOSETUP -o $offset $loopdev $img");
	}
	if (!$rc) {
	    $attached = 1;
	} else {
	    system("$LOSETUP -d $loopdev");
	}
    }
}
if (!$attached) {
    print STDERR "cannot attach a loop device\n";
    do_exit(1);
}

if (system("$TUNE2FS -c 0 -i 0 $loopdev >/dev/null 2>&1")) {
    print STDERR "cmd: $TUNE2FS -c 0 -i 0 $loopdev\n";
#    do_exit(1);
}

# without a key, add_key.pl just runs tune2fs
if (not defined($key)) {
    do_exit(0);
}

$key = untaint($key);
if (!-f "$key") {
    print STDERR "add_key cannot verify inputs: key=$key\n";
    do_exit(1);
}

if (system("$mounter mount $loopdev $tmpfile")) {
    print STDERR "cannot mount: $mounter mount $loopdev $tmpfile\n";
    do_exit(1);
}
$mounted = 1;

if ( !-d "$tmpfile/root/.ssh" ) {
    if (system("$MKDIR $tmpfile/root/.ssh")) {
	print STDERR "cmd: $MKDIR $tmpfile/root/.ssh\n"; 
	do_exit(1);
    }
    system("$CHOWN root $tmpfile/root/.ssh");
    system("$CHMOD 0700 $tmpfile/root/.ssh");
}

if (!open(OFH, ">>$tmpfile/root/.ssh/authorized_keys")) {
    print STDERR "cannot write to: $tmpfile/root/.ssh/authorized_keys\n"; 
    do_exit(1);
}
print OFH "\n";
if (!open(FH, "$key")) {
    print STDERR "cannot read from: $key\n"; 
    do_exit(1);
}
while(<FH>) {
    chomp;
    print OFH "$_\n";
}
close(FH);
close(OFH);

system("$CHOWN root $tmpfile/root/.ssh/authorized_keys");
system("$CHMOD 0600 $tmpfile/root/.ssh/authorized_keys");
do_exit(0);

sub do_exit() {
    $e = shift;

    if ($mounted && ($tmpfile ne "")) {
	system("$mounter umount $tmpfile");
    }
    if ($attached && ($loopdev ne "")) {
	system("$LOSETUP -d $loopdev");
    }
    if ($tmpfile ne "") {
	system("$RMDIR $tmpfile");
    }

    if (-f "$img") {
	# be conservative about deleting loopback devices
	open(RFH, "losetup -a|");
	while(<RFH>) {
	    chomp;
	    my $line = $_;
	    
	    if ($line =~ /$img/) {
		if ($line =~ /(\/dev\/loop\d+).*/) {
		    system("$LOSETUP -d $1");
		}
	    }
	}
	close(RFH);
    }

    exit($e);
}

sub untaint() {
    $str = shift;
    if ($str =~ /^([ &:#-\@\w.]+)$/) {
	$str = $1; #data is now untainted
    } else {
	$str = "";
    }
    return($str);
}
