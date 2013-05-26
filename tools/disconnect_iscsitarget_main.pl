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

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

$ISCSIADM = untaint(`which iscsiadm`);
$MULTIPATH = untaint(`which multipath`);
$DMSETUP = untaint(`which dmsetup`);
$YES_RESCAN = "rescan";

$CONF_IFACES_KEY = "STORAGE_INTERFACES";

# check binaries
if (!-x $ISCSIADM) {
  print STDERR "Unable to find iscsiadm\n";
  do_exit(1);
}

# check input params
$dev_string = untaint(shift @ARGV);

#2nd param is if a rescan should be done or not
$do_rescan = untaint(shift @ARGV);
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
if (($multipath == 1) && ((!-x $MULTIPATH) || (!-x $DMSETUP))) {
  print STDERR "Unable to find multipath or dmsetup\n";
}
sanitize_path(\@paths);

if (is_null_or_empty($lun)) {
  $lun = -1;
}

if ($multipath == 1) {
  $mpath = get_mpath_device_by_paths(@paths);
}

# Remove unused mpath device
if ($multipath == 1 && !is_null_or_empty($mpath)) {
  sleep(1);
  # flush device map
  run_cmd(1, 1, "$MULTIPATH -f $mpath") if (-x $MULTIPATH);
  sleep(1);
}

while (@paths > 0) {
  $conf_iface = shift(@paths);
  $ip = shift(@paths);
  $store = shift(@paths);
  # get netdev from iface name using eucalyptus.conf
  $netdev = get_netdev_by_conf($conf_iface);
  if ($lun > -1) {
    # keep trying lun deletion until it is really gone
    for ($i = 0; $i < 10; $i++) {
      delete_lun($netdev, $ip, $store, $lun);
      
      if($do_rescan eq $YES_RESCAN) {
      	# rescan target
      	run_cmd(1, 1, "$ISCSIADM -m session -R");
      	last if is_null_or_empty(get_iscsi_device($netdev, $ip, $store, $lun));
      	sleep(5);
      } else {
      	#Don't rescan, continue
      	last if is_null_or_empty(get_iscsi_device($netdev, $ip, $store, $lun));      	
      }
    }
    print STDERR "Tried deleting lun $lun $i times in iSCSI session IP=$ip, IQN=$store\n";
    next if retry_until_true(\&has_device_attached, [$netdev, $ip, $store], 5) == 1;
  }
  # logout
  if (!is_null_or_empty($netdev)) {
    $iface = retry_until_exists(\&get_iface, [$netdev], 5);
    if (is_null_or_empty($iface)) {
      print STDERR "Failed to get iface.\n";
      do_exit(1);
    }
  }
  if (is_null_or_empty($iface)) {
    run_cmd(1, 0, "$ISCSIADM -m node -p $ip -T $store -u");
  } else {
    run_cmd(1, 0, "$ISCSIADM -m node -p $ip -T $store -I $iface -u");
  }
 # Delete /var/lib/iscsi/node 
  run_cmd(1, 0, "$ISCSIADM -m node -p $ip -T $store -o delete");
}

#####################################################
sub get_mpath_device_by_paths {
  my @devices = ();
  while (@_ > 0) {
    my $conf_iface = shift(@_);
    my $ip = shift(@_);
    my $store = shift(@_);
    # get netdev from iface name using eucalyptus.conf
    my $netdev = get_netdev_by_conf($conf_iface);
    # get dev from lun
    push @devices, retry_until_exists(\&get_iscsi_device, [$netdev, $ip, $store, $lun], 5);
  }
  return retry_until_exists(\&get_mpath_device, \@devices, 5);
}

sub delete_lun {
  my ($netdev, $ip, $store, $lun) = @_;
  my $sid;
  my $host_number;
  for $session (lookup_session()) {
    if (match_iscsi_session($session, $netdev, $ip, $store)) {
      $sid = $session->{$SK_SID};
      $host_number = $session->{$SK_HOSTNUMBER};
      last;
    }
  }
  if (is_null_or_empty($sid) || is_null_or_empty($host_number)) {
    print STDERR "Warning: failed to get SID or Host Number for session IP=$ip, IQN=$store,lun=$lun.\n";
    return;
  }
  $delete_path = "/sys/class/iscsi_session/session$sid/device/target$host_number:0:0/$host_number:0:0:$lun/delete";
  if (!open DELETELUN, ">$delete_path") {
    print STDERR "Unable to write $delete_path.\n";
  } else {
    print DELETELUN "1";
    close DELETELUN;
  }
}

sub has_device_attached {
  my ($netdev, $ip, $store) = @_;
  for $session (lookup_session()) {
    if (match_iscsi_session($session, $netdev, $ip, $store)) {
      return has_lun(keys %$session);
    }
  }
  return 0;
}

sub has_lun {
  foreach (@_) {
    if (/$SK_LUN-\d+/) {
      return 1;
    }
  }
  return 0;
}

sub retry_until_exists {
  my ($func, $args, $retries) = @_;
  for ($i = 0; $i < $retries; $i++) {
    $ret = $func->(@$args);
    if (!is_null_or_empty($ret)) {
      return $ret;
    }
  }
}

##############################################
# needed by module
return 1;
