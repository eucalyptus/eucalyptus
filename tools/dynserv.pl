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
#
# This file may incorporate work covered under the following copyright
# and permission notice:
#
#   Software License Agreement (BSD License)
#
#   Copyright (c) 2008, Regents of the University of California
#   All rights reserved.
#
#   Redistribution and use of this software in source and binary forms,
#   with or without modification, are permitted provided that the
#   following conditions are met:
#
#     Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#
#     Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer
#     in the documentation and/or other materials provided with the
#     distribution.
#
#   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
#   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
#   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
#   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
#   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
#   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
#   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
#   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
#   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
#   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
#   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
#   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
#   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
#   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
#   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
#   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
#   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
#   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
#   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.

use File::Path;

my $eucalyptus = $ENV{'EUCALYPTUS'};
my $dynpath = "$eucalyptus/var/lib/eucalyptus/dynserv";
my $rc;
my $dynpath = shift @ARGV;
my @allowhosts = @ARGV;

$rc = setup_dynpath($dynpath);
if ($rc) {
    print "ERROR: could not create directory structure\n";
    exit (1);
}

$rc = open(OFH, ">$dynpath/dynserv-httpd.conf");
if ($rc <= 0) {
    print "ERROR: could not create config file '$dynpath/dynserv-httpd.conf'\n";
    exit (1);
}

$authz = find_authz();
$apache = find_apache2();
if ($authz eq "none" || $apache eq "none") {
    print "ERROR: cannot find authz module ($authz) or apache2 ($apache)\n";
    exit (1);
}

$rc = prepare_configfile($eucalyptus, $dynpath, "eucalyptus", "eucalyptus", $authz, $apache, @allowhosts);
if ($rc) {
    print "ERROR: could not set up configfile\n";
    exit(1);
}

$rc = restart_apache($dynpath, $apache);
if ($rc) {
    print "ERROR: could not restart apache2\n";
    exit(1);
}

exit(0);

sub restart_apache() {
    my $dynpath = shift @_;
    my $apache = shift @_;

    my $cmd = "$apache -f $dynpath/dynserv-httpd.conf -k graceful";
    my $rc = system("$cmd");
    if ($rc) {
	print "ERROR: could not run cmd '$cmd'\n";
    }
    return($rc);
}

sub prepare_configfile() {
    my $eucalyptus = shift @_;
    my $dynpath = shift @_;
    my $user = shift @_;
    my $group = shift @_;
    my $authz = shift @_;
    my $apache = shift @_;
    my @allowhosts = @_;

    if (!-d "$eucalyptus/var/run/eucalyptus" || !-d "$eucalyptus/var/log/eucalyptus") {
	print "ERROR: eucalyptus root '$eucalyptus' not found\n";
	return(1);
    }
    if (!-d "$eucalyptus" || !-d "$dynpath" || !-e "$authz" || !-x "$apache") {
	print "ERROR: eucalyptus=$eucalyptus dynpath=$dynpath user=$user group=$group authz=$authz apache=$apache\n";
	return(1);
    }

    $allows = "127.0.0.0/8";
    foreach $host (@allowhosts) {
	$allows = $allows . " $host";
    }

    print OFH <<EOF;
ServerTokens OS
ServerRoot "$dynpath"
ServerName 127.0.0.1
Listen 8776
KeepAliveTimeout 30
PidFile $eucalyptus/var/run/eucalyptus/httpd-dynserv.pid
User $user
group $group
ErrorLog $eucalyptus/var/log/eucalyptus/httpd-dynserv-err.log
LogLevel warn
LoadModule authz_host_module $authz
DocumentRoot "$dynpath/data/"
<Directory "$dynpath/data/">
   Order deny,allow
#  Allow from 127.0.0.1
#  Allow from none
   Allow from $allows
   Deny from all
</Directory>
EOF
close(OFH);
return(0);
}

sub find_authz() {
    my @known_locations = ('/usr/lib64/httpd/modules/mod_authz_host.so',
			   '/usr/lib/httpd/modules/mod_authz_host.so',
			   '/usr/lib64/apache2/mod_authz_host.so',
			   '/usr/lib/apache2/mod_authz_host.so',
			   '/usr/lib/apache2/modules/mod_authz_host.so');

    $foundfile = "none";
    foreach $file (@known_locations) {
	if ( -f "$file" ) {
	    $foundfile = $file;
	}
    }
    return($foundfile);
}

sub find_apache2() {
    my @known_locations = ('/usr/sbin/apache2',
			   '/usr/sbin/httpd2',
			   '/usr/sbin/httpd');
    $foundfile = "none";
    foreach $file (@known_locations) {
	if ( -x "$file" ) {
	    $foundfile = $file;
	}
    }
    return($foundfile);
}

sub setup_dynpath() {
    my $root=shift @_;

    mkpath("$root/data/", {error => \my $err});
    if (@$err) {
	print "ERROR: could not create directory '$root/data'\n";
	return(1);
    }

    return(0);
}
