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

BEGIN {
  use File::Spec::Functions qw(rel2abs);
  use File::Basename qw(dirname);

  my $script_abs_path = rel2abs($0);
  our $script_dir = dirname($script_abs_path);
}

use lib $script_dir;

require "iscsitarget_common.pl";

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

$NOT_REQUIRED = "not_required";

$DEFAULT = "default";

$ISCSIADM = untaint(`which iscsiadm`);
$MULTIPATH = untaint(`which multipath`);
$DMSETUP = untaint(`which dmsetup`);

$CONF_IFACES_KEY = "STORAGE_INTERFACES";

# check binaries
if (!-x $ISCSIADM) {
  print STDERR "Unable to find iscsiadm\n";
  do_exit(1);
}

# check input params
$dev_string = untaint(shift @ARGV);
($euca_home, $user, $auth_mode, $lun, $encrypted_password, @paths) = parse_devstring($dev_string);

if (is_null_or_empty($euca_home)) {
  print STDERR "EUCALYPTUS path is not defined:$dev_string\n";
  do_exit(1);
}

$EUCALYPTUS_CONF = $euca_home."/etc/eucalyptus/eucalyptus.conf";
%conf_iface_map = get_conf_iface_map();

# prepare target paths:
# <netdev0>,<ip0>,<store0>,<netdev1>,<ip1>,<store1>,...
if ((@paths < 1) || ((@paths % 3) != 0)) {
  print STDERR "Target paths are not complete:$dev_string\n";
  do_exit(1);
}

$multipath = 0;
$multipath = 1 if @paths > 3;
if (($multipath == 1) && (!-x $MULTIPATH)) {
  print STDERR "Unable to find multipath\n";
  do_exit(1);
}
sanitize_path(\@paths);

if (is_null_or_empty($lun)) {
  $lun = -1;
}

#Rescan
rescan_all_sessions();

@devices = ();
# iterate through each path, login/refresh for the new lun
while (@paths > 0) {
  $conf_iface = shift(@paths);
  $ip = shift(@paths);
  $store = shift(@paths);
  # get netdev from iface name using eucalyptus.conf
  $netdev = get_netdev_by_conf($conf_iface);
  # get dev from lun
  push @devices, retry_until_exists(\&get_iscsi_device, [$netdev, $ip, $store, $lun], 5);
}
# get the actual device
# Non-multipathing: the iSCSI device
# Multipathing: the mpath device
if ($multipath == 0) {
  $localdev = $devices[0];
} else {
  $localdev = retry_until_exists(\&get_mpath_device, \@devices, 5);
}
if (is_null_or_empty($localdev)) {
  print STDERR "Unable to get attached target device.\n";
  do_exit(1);
}
# get the /dev/disk/by-id path
if ($multipath == 0) {
  $localdev = get_disk_by_id_path("/dev/$localdev");
} else {
  # TODO(wenye): temporary measure to allow safe volume detaching due to "queue_if_no_path"
  run_cmd(1, 0, "$DMSETUP message $localdev 0 'fail_if_no_path'");
  sleep(2);
  $localdev = get_disk_by_id_path("/dev/mapper/$localdev");
}

print "$localdev";

##################################################################
sub retry_until_exists {
  my ($func, $args, $retries) = @_;
  for ($i = 0; $i < $retries; $i++) {
    $ret = $func->(@$args);
    if (!is_null_or_empty($ret)) {
      return $ret;
    }
  }
}
