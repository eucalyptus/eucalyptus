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


use POSIX;

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

my $rootwrap = untaint(shift @ARGV);
my $virsh = untaint(shift @ARGV);
my $instanceId = untaint(shift @ARGV);
my $localdev = untaint(shift @ARGV);
my $virshxmlfile = untaint(shift @ARGV);

my $distro = "GENERIC";
my $user = getuid();

$inputfail = 1;
$detachfail = 2;
#system("cp $virshxmlfile /tmp/wtf");
$distro = detect_distro();
print STDERR "DISTRO: $distro\n";
if ( ! -x "$rootwrap" ) {
    print STDERR "ERROR: cannot find root wrapper '$rootwrap'\n";
    exit $inputfail;
}

$rc = check_devexists($rootwrap, $virsh, $instanceId, $localdev);
if ($rc == 1) {
    print STDERR "device to detach is not attached\n";
    exit(0);
}

if ($distro eq "GENERIC") {
    # try both workarounds
    if ( ! -f "$virshxmlfile" ) {
	print STDERR "ERROR: cannot locate virsh XML file\n";
	exit $inputfail;
    }

    $cmd = "$rootwrap $virsh detach-device $instanceId $virshxmlfile";
    $rc = system($cmd);
    if ($rc) {
	print STDERR "ERROR: cmd failed '$cmd'\n";
	$cmd = "sudo xm block-detach $instanceId $localdev";
	$rc = system($cmd);
	if ($rc) {
	    print STDERR "ERROR: cmd failed '$cmd'\n";
	}
    }
    
    if ($rc) {
	exit ($detachfail);
    }
    
} elsif ($distro eq "DEBIAN") {
    # do the debian magic
    $cmd = "sudo xm block-detach $instanceId $localdev";
    $rc = system($cmd);
    if ($rc) {
	print STDERR "ERROR: cmd failed '$cmd'\n";
	exit ($detachfail);
    }
} else {
    print STDERR "ERROR: unknown distribution\n";
    exit $inputfail;
}

for ($i=0; $i<5; $i++) {
    $rc = check_devexists($rootwrap, $virsh, $instanceId, $localdev);
    if ($rc == 1) {
	exit(0);
    }
    sleep(1);
}

exit $detachfail;

sub check_devexists() {
    my $rootwrap = untaint(shift @_);
    my $virsh = untaint(shift @_);
    my $instanceId = untaint(shift @_);
    my $localdev = untaint(shift @_);

    open(RFH, "$rootwrap $virsh dumpxml $instanceId|");
    while(<RFH>) {
	chomp;
	my $line = $_;
	if ($line =~ /target dev='$localdev' bus='scsi'/) {
	    close(RFH);
	    return(0);
	}
    }
    close(RFH);
    return(1);
}

sub detect_distro() {
    if ( -f "/etc/debian_version" ) {
	return("DEBIAN");
    } elsif ( -f "/etc/issue" ) {
	open(FH, "/etc/issue");
	while(<FH>) {
	    chomp;
	    my $line = $_;
	    if ($line =~ /CentOS/) {
		close(FH);
		return("GENERIC");
	    }
	}
	close(FH);
    }
    return("GENERIC");
}

sub untaint() {
    my $str = shift;
    if ($str =~ /^([ &:#-\@\w.]+)$/) {
	$str = $1; #data is now untainted                                       
    } else {
        $str = "";
    }
    return($str);
}
