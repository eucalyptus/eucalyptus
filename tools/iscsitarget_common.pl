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

$SK_TARGET = "target";
$SK_PORTAL = "portal"; # current portal
$SK_PPORTAL = "pportal"; # persistent portal
$SK_TPGT = "tpgt";
$SK_IFACE = "iface";
$SK_NETDEV = "netdev";
$SK_LUN = "lun";
$SK_SID = "sid";
$SK_HOSTNUMBER = "hostnumber";

$DELIMITER = ",";

$ERROR_CAUSE = "iscsiadm";
$ERROR_MESSAGE = "iSCSI driver not found";
$UDEVADM = "udevadm";

sub parse_devstring {
  my ($dev_string) = @_;
  return split($DELIMITER, $dev_string);
}

sub sanitize_path {
  # my (@paths) = @{$_[0]};
  for ($i = 2; $i < @{$_[0]}; $i += 3) {
    ${$_[0]}[$i] =~ s/\.$//g;
  }
}

sub run_cmd {
  my ($print_on_error, $exit_on_error, $command) = @_;
  my @outlines = qx($command 2>&1);
  if ($? != 0) {
    check_and_log_fault($command, @outlines);
    $print_on_error and print STDERR "Failed to run '$command': @outlines";
    $exit_on_error and do_exit(1);
  }
  return @outlines;
}

sub check_and_log_fault {
  my ($command, @output) = @_;

  if (!is_null_or_empty($euca_home) && !is_null_or_empty($euca_component) && ($command =~ /\Q$ERROR_CAUSE\E/i)) {
    my $FAULT_GEN_CMD = $euca_home."/usr/sbin/euca-generate-fault -c ".lc($euca_component)." 2001 component ".$euca_component;
    
    foreach (@output) {
      chomp;
      if(/\Q$ERROR_MESSAGE\E/i) {
        qx($FAULT_GEN_CMD 2>&1);
        last;
      }
    }
  } 
}

sub do_exit {
  my $e = shift;
  exit($e);
}

sub untaint {
  my $str = shift;
  if ($str =~ /^([ &:#-\@\w.]+)$/) {
    $str = $1; #data is now untainted
  } else {
    $str = "";
  }
  return($str);
}

sub lookup_mpath {
  my @output = run_cmd(0, 0, "$MULTIPATH -ll");
  my %mpath = ();
  my $dev;
  foreach (@output) {
    chomp;
    if (/^(mpath\S+) .*/) {
      $dev = $1;
    } elsif (/.*-\s+\S+:\S+\s+(\S+) .*/) {
      $mpath{$1} = $dev;
    }
  }
  return %mpath;
}

sub lookup_iface {
  my @output = run_cmd(0, 0, "$ISCSIADM -m iface");
  my %iface = ();
  foreach (@output) {
    chomp;
    if (/^(\S+)\s+[^,]+,[^,]+,[^,]+,([^,]+),[^,]+/) {
      if ($2 ne "<empty>") {
        $iface{$2} = $1;
      }
    }
  }
  return %iface;
}

sub retry_until_true {
  my ($func, $args, $retries) = @_;
  for ($i = 0; $i < $retries; $i++) {
    if ($func->(@$args) == 1) {
      return 1;
    }
  }
  return 0;
}

sub match_iscsi_session {
  my ($session, $netdev, $ip, $store) = @_;
  if (($session->{$SK_TARGET} eq $store) &&
      (($session->{$SK_PORTAL} eq $ip) || ($session->{$SK_PPORTAL} eq $ip)) &&
      (is_null_or_empty($netdev) || ($session->{$SK_NETDEV} eq $netdev))) {
    return 1;
  }
  return 0;
}

sub get_iscsi_device {
  my ($netdev, $ip, $store, $lun) = @_;
  #Trigger udev explicitly
  run_cmd(0,0,"$UDEVADM trigger");
  for $session (lookup_session()) {
    if (match_iscsi_session($session, $netdev, $ip, $store)) {
      if ($lun > -1) {
        return $session->{"$SK_LUN-$lun"};
      } else {
        return $session->{get_first_lun(sort(keys %$session))};
      }
    }
  }
}

sub get_first_lun {
  foreach (@_) {
    if (/$SK_LUN-\d+/) {
      return $_;
    }
  }
}

sub get_mpath_device {
  %mpaths = lookup_mpath();
  foreach (@_) {
    my $mpathdev = $mpaths{$_};
    return $mpathdev if !is_null_or_empty($mpathdev);
  }
}

sub is_null_or_empty {
  my ($value) = @_;
  return 1 if ((!defined $value) || (length($value) < 1));
  return 0;
}

sub get_conf_iface_map {
  my %iface_map = ();
  if (!open CONF, $EUCALYPTUS_CONF) {
    print STDERR "Could not open eucalyptus.conf";
    exit(1);
  }
  while (<CONF>) {
    chomp;
    if (/^$CONF_IFACES_KEY\s*=\s*\"(.*)\"/) {
      foreach my $pair (split(",", $1)) {
        my ($iface, $netdev) = split("=", $pair);
        $iface_map{$iface} = $netdev;
      }
      last;
    }
  }
  close CONF;
  return %iface_map;
}

sub get_netdev_by_conf {
  my ($conf_iface) = @_;
  my $dev;
  if (!is_null_or_empty($conf_iface)) {
    $dev = $conf_iface_map{$conf_iface};
    if (is_null_or_empty($dev)) {
      print STDERR "Can't find valid interface mapping in eucalyptus.conf.\n";
      exit(1);
    }
  }
  return $dev;
}

sub get_iface {
  my ($netdev) = @_;
  my %ifaces = lookup_iface();
  return $ifaces{$netdev};
}

sub lookup_session {
  my @output = run_cmd(0, 0, "$ISCSIADM -m session -P 3");
  my $session = {};
  my @sessions = ();
  my $lun;
  foreach (@output) {
    chomp;
    if (/^Target:\s+(\S+)/) {
      $session = {};
      push @sessions, $session;
      $session->{$SK_TARGET} = $1;
    } elsif (/^\s+Current Portal:\s+([\d\.]+):\d+,(\d+)/) {
      $session->{$SK_PORTAL} = $1;
      $session->{$SK_TPGT} = $2;
    } elsif (/^\s+Persistent Portal:\s+([\d\.]+):\d+,(\d+)/) {
      $session->{$SK_PPORTAL} = $1;
    } elsif (/^\s+Iface Name:\s+(\S+)/) {
      $session->{$SK_IFACE} = $1;
    } elsif (/^\s+Iface Netdev:\s+(\S+)/) {
      $session->{$SK_NETDEV} = $1;
    } elsif (/.*\s+Lun:\s+(\d+)/) {
      $lun = $1;
    } elsif (/^\s+Attached scsi disk\s+(\S+).*/) {
      $session->{"$SK_LUN-$lun"} = $1;
    } elsif (/^\s+SID:\s+(\d+)/) {
      $session->{$SK_SID} = $1;
    } elsif (/^\s+Host Number:\s+(\d+)\s+State.*/) {
      $session->{$SK_HOSTNUMBER} = $1;
    }
  }
  return @sessions;
}

sub get_wrap_name {
    my ($store) = @_;
    
    my $wrapped_name = "euca-" . $store;
    $wrapped_name =~ s/[\.:]/-/g;

    return $wrapped_name;
}

sub get_wrap_dev_name {
    my ($localdev, $store) = @_;

    my $wrapped_dev_name = "/dev/mapper/" . get_wrap_name($store);
    my $size_local = `blockdev --getsz $localdev`;
    my $size_wrapped = `blockdev --getsz $wrapped_dev_name`;
    if ($size_local eq $size_wrapped) {
	return $wrapped_dev_name;
    } else {
	return $localdev;
    }
}

sub unwrap_device {
    my ($store) = @_;

    my $wrapped_name = get_wrap_name($store);
    my $cmd = "dmsetup remove $wrapped_name";
    if (system($cmd) != 0) {
	print STDERR "'$cmd' failed with $?\n";
    }
}

sub wrap_device {
    my ($localdev, $store) = @_;

    my $size_sectors = `blockdev --getsz $localdev`;
    chomp($size_sectors); # remove newline
    if ($size_sectors > 0) {
	my $wrapped_name = get_wrap_name($store);
	my $cmd = "echo \"0 $size_sectors linear $localdev 0\" | dmsetup create $wrapped_name";
	if (system($cmd) != 0) {
	    print STDERR "'$cmd' failed with $?\n";
	}
    }
    return get_wrap_dev_name($localdev, $store); # return the wrapped dev name if exists or localdev
}
