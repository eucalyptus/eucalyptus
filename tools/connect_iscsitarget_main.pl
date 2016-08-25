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

use Crypt::OpenSSL::Random ;
use Crypt::OpenSSL::RSA ;
use MIME::Base64;

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

$NOT_REQUIRED = "not_required";

$DEFAULT = "default";

$CONF_IFACES_KEY = "STORAGE_INTERFACES";
$LOGIN_TIMEOUT = 12;
$LOGOUT_TIMEOUT = 5;
$LOGIN_RETRY_COUNT = 1;

# check input params
$dev_string = untaint(shift @ARGV);
($euca_home, $volume_id, $target_device, $target_serial, $target_bus, $ceph_user, $ceph_keyring, $ceph_conf, $protocol, $provider, $user, $auth_mode, $lun, $encrypted_password, @paths) = parse_devstring($dev_string);

if (is_null_or_empty($euca_home)) {
  print STDERR "EUCALYPTUS path is not defined:$dev_string\n";
  do_exit(1);
}

if (!is_null_or_empty($protocol) && $protocol eq $PROTOCOL_RBD) {
	
  print STDERR "Found rbd as protocol in connection string\n";

  if (is_null_or_empty($ceph_user)) {
    $ceph_user = "eucalyptus";
  }
  if (is_null_or_empty($ceph_keyring)) {
    $ceph_keyring = "/etc/ceph/ceph.client.eucalyptus.keyring";
  }
  if (is_null_or_empty($ceph_conf)) {
    $ceph_conf = "/etc/ceph/ceph.conf";
  }
	
  # Setup the virsh secret
  # Fetch the ceph key from keyring file
  if (open KEYRING_FH, "$ceph_keyring") {
    $founduser = "0";
	while (<KEYRING_FH>) {
      chomp;
      if (/^\[client\.(.*)\]$/) {
        $founduser = "1" if ($1 eq $ceph_user);  
      } elsif ($founduser and /^\s*key\s*=\s*(.*)$/) {
        $ceph_key = $1;
        last;
      }
    }
    close KEYRING_FH;
  } else {
    print STDERR "Unable to open ceph keyring file $ceph_keyring.\n";
    do_exit(1);
  }
  
  # Verify if the virsh secret is defined and set
  $command = "virsh secret-get-value $encrypted_password";
  @output = qx($command 2>&1);

  if ($? != 0) {
    # If the above command returns error, define a new secret and associate it with ceph key
    print STDERR "Failed to run $command: @output\n";
    setup_virsh_secret($encrypted_password, $ceph_key);
  } else { 
    # If the above command returns success, check whether the value in the virsh secret maps to the configured user
    $virsh_secret_value = shift(@output);
    chomp($virsh_secret_value);

    if ($virsh_secret_value ne $ceph_key) {
      print STDERR "Virsh secret is not associated with the configured user. Resetting the virsh secret\n";
      setup_virsh_secret($encrypted_password, $ceph_key);			
    } else {
      print STDERR "Virsh secret already configured. Moving on\n";
    }
  }

  # Get the monitoring hosts
  # All monitors may be listed on one line in ceph.conf: 
  # [mon]
  #       mon host = hostname1,hostname2,hostname3
  #       mon addr = a.b.c.d:xyz,a.b.c.d:xyz,a.b.c.d:xyz
  # 
  # ceph.conf may also have specific entries for each monitor:
  # [mon.a]
  #      host = hostname1
  #      mon addr = a.b.c.d:xyz
  # [mon.b]
  #      host = hostname2
  #      mon addr = a.b.c.d:xyz
  
  $ceph_host_line = "";       
  if (open CONF_FH, "$ceph_conf") {
    while (<CONF_FH>) {
      chomp;
      if (/^\s*mon addr\s*=\s*(.*)$/) {
        @addrs = split(/,/, $1);
        foreach (@addrs) {
          $monip = "";
          $monport = "";
          ($monip, $monport) = split(/:/, $_);
          if (!is_null_or_empty($monip) and !is_null_or_empty($monport)) {
            $ceph_host_line .= "    <host name='$monip' port='$monport'/>\n";
          } else {
            close CONF_FH;
            print STDERR "Either monitor address or port information not found. Looking for 'mon addr = a.b.c.d:xyz' like configuration in the $ceph_conf\n";
            do_exit(1);
          }
        }
      } 
    }
    close CONF_FH;
    
    if (is_null_or_empty($ceph_host_line)) {
      print STDERR "Monitor address and port information not found. Looking for 'mon addr = a.b.c.d:xyz' like configuration in the $ceph_conf\n";
      do_exit(1);            
    }
    
  } else {
    print STDERR "Unable to open $ceph_conf.\n";
    do_exit(1);
  }

  # Generate the libvirt.xml
  $libvirtxml = "<disk type='network' device='disk'>\n";
  $libvirtxml .= "  <source protocol='rbd' name='$lun'>\n";
  $libvirtxml .= $ceph_host_line;
  $libvirtxml .= "  </source>\n";
  $libvirtxml .= "  <auth username='$ceph_user'>\n";
  $libvirtxml .= "    <secret type='$SECRET_TYPE_CEPH' uuid='$encrypted_password'/>\n";
  $libvirtxml .= "  </auth>\n";
  $libvirtxml .= "  <target bus='$target_bus' dev='$target_device'/>\n";
  $libvirtxml .= "  <serial>$target_serial</serial>\n";
  $libvirtxml .= "</disk>\n";

  print "$libvirtxml";
} else {
  $ISCSIADM = untaint(`which iscsiadm`);
  $MULTIPATH = untaint(`which multipath`);

  # check binaries
  if (!-x $ISCSIADM) {
    print STDERR "Unable to find iscsiadm\n";
    do_exit(1);
  }
  
  $EUCALYPTUS_CONF = $euca_home."/etc/eucalyptus/eucalyptus.conf";
  %conf_iface_map = get_conf_iface_map();

  if (is_null_or_empty($user)) {
    $user = "eucalyptus";
  }

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

  if (!is_null_or_empty($auth_mode)) {
    $password = $NOT_REQUIRED;
  }

  # debugging
  use Data::Dumper;
  print STDERR "Before connecting:\n";
  for $session (lookup_session()) {
    print STDERR Dumper($session);
  }
	
  @devices = ();
  # iterate through each path, login/refresh for the new lun
  while (@paths > 0) {
    $conf_iface = shift(@paths);
    $ip = shift(@paths);
    $store = shift(@paths);
    # get netdev from iface name using eucalyptus.conf
    $netdev = get_netdev_by_conf($conf_iface);
    # lun based, check if session exists and refresh session
    if (($lun > -1) && (retry_until_true(\&check_session_exists, [$netdev, $ip, $store], 5) == 1)) {
      # rescan session
      run_cmd(1, 1, "$ISCSIADM -m session -R");
    } else {
      # prepare password for login target 
      if ($password ne $NOT_REQUIRED) {
        $password = decrypt_password($encrypted_password, get_private_key());
      }
      if (is_null_or_empty($password)) {
        print STDERR "Unable to decrypt target password.\n";
        do_exit(1);
      }
      # get/create iface for the path if network device is specified
      if (!is_null_or_empty($netdev)) {
        $iface = retry_until_exists(\&ensure_and_get_iface, [$netdev], 5);
        if (is_null_or_empty($iface)) {
          print STDERR "Failed to get iface.\n";
          do_exit(1);
        }
      }
      login_target($iface, $ip, $store, $user, $password);
      sleep(1);
    }
    
    # get dev from lun
    $blockdevice = retry_until_exists(\&get_iscsi_device, [$netdev, $ip, $store, $lun], 2);
    
    # rescan the session and try fetching the device. adding this check to cover 3PAR which might be slow in exporting luns
    if (is_null_or_empty($blockdevice)) {
      $blockdevice = retry_until_exists(\&refresh_session_and_get_iscsi_device, [$netdev, $ip, $store, $lun], 5);
    }
    
    # push the block device to the array only if its not empty
    if (!is_null_or_empty($blockdevice)) {
      push @devices, $blockdevice;
    }
  }
  # debugging
  use Data::Dumper;
  print STDERR "After connecting:\n";
  for $session (lookup_session()) {
    print STDERR Dumper($session);
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
  if ($multipath == 0) {
    $localdev = "/dev/$localdev";
  } else {
    $localdev = "/dev/mapper/$localdev";
  }
  # get the /dev/disk/by-id path
  $localdev = retry_until_exists(\&get_disk_by_id_path, [$localdev], 5);
  if (is_null_or_empty($localdev)) {
    print STDERR "Unable to get /dev/disk/by-id path for attached target device.\n";
    do_exit(1);
  }
  # make sure device exists on the filesystem
  for ($i = 0; $i < 12; $i++) { 
    last if (-e "$localdev");
    sleep(1);
  }
	
  $libvirtxml = "<disk type='block'>\n";
  $libvirtxml .= "  <driver cache='none' name='qemu'/>\n";
  $libvirtxml .= "  <source dev='$localdev'/>\n";
  $libvirtxml .= "  <target bus='$target_bus' dev='$target_device'/>\n";
  $libvirtxml .= "  <serial>$target_serial</serial>\n";
  $libvirtxml .= "</disk>\n";

  print "$libvirtxml";
}

##################################################################
sub login_target {
  my ($iface, $ip, $store, $user, $password) = @_;
  my $prefix;
  if (is_null_or_empty($iface)) {
    $prefix = "$ISCSIADM -m node -T $store -p $ip";
  } else {
    $prefix = "$ISCSIADM -m node -T $store -p $ip -I $iface";
  }
  # add paths
  run_cmd(1, 1, "$prefix -o new");
  
  # set login and logout timeouts and retries
  run_cmd(1, 1, "$prefix -o update -n node.conn[0].timeo.login_timeout -v $LOGIN_TIMEOUT");
  run_cmd(1, 1, "$prefix -o update -n node.conn[0].timeo.logout_timeout -v $LOGOUT_TIMEOUT");
  run_cmd(1, 1, "$prefix -o update -n node.session.initial_login_retry_max -v $LOGIN_RETRY_COUNT");
  
  # set password if necessary
  if (password ne $NOT_REQUIRED) {
    run_cmd(1, 1, "$prefix -o update -n node.session.auth.username -v $user");
    run_cmd(1, 1, "$prefix -o update -n node.session.auth.password -v $password");
  }
  
  # login. Dont error out on login failure. Setup the connection even if one path is up.
  run_cmd(1, 0, "$prefix -l");
}

sub ensure_and_get_iface {
  my ($netdev) = @_;
  %ifaces = lookup_iface();
  if (is_null_or_empty($ifaces{$netdev})) {
    $name = allocate_iface(values %ifaces);
    create_iface($name, $netdev);
    %ifaces = lookup_iface();
  }
  return $ifaces{$netdev};
}

sub allocate_iface {
  my %ifaceset = map {$_ => 1} @_;
  for ($i = 0; $i < 1024; $i++) {
    my $newname = "iface$i";
    return $newname if !$ifaceset{$newname};
  }
  print STDERR "Can not create iface anymore.\n";
  do_exit(1);
}

sub create_iface {
  my ($name, $netdev) = @_;
  run_cmd(1, 1, "$ISCSIADM -m iface -I $name --op=new");
  run_cmd(1, 1, "$ISCSIADM -m iface -I $name -o update -n iface.net_ifacename -v $netdev");
}

sub decrypt_password {
  my ($encrypted_passwd, $private_key) = @_;

  $rsa_priv = Crypt::OpenSSL::RSA->new_private_key($private_key);

  $msg = decode_base64($encrypted_passwd);
  $rsa_priv->use_pkcs1_padding();
  $rsa_priv->use_sha1_hash() ;

  return $rsa_priv->decrypt($msg);
}

sub retry_until_exists {
  my ($func, $args, $retries) = @_;
  for ($i = 0; $i < $retries; $i++) {
    $ret = $func->(@$args);
    if (!is_null_or_empty($ret)) {
      return $ret;
    }
    sleep(1);
  }
}

sub check_session_exists {
  my ($netdev, $ip, $store) = @_;
  for $session (lookup_session()) {
    if (match_iscsi_session($session, $netdev, $ip, $store)) {
      return 1;
    }
  }
  return 0;
}

sub setup_virsh_secret {
	my ($uuid, $ceph_key) = @_;
	
	# First undefine the secret if one exists
	run_cmd(1, 0, "virsh secret-undefine $uuid");
		  
	$secret = "<secret ephemeral='no' private='no'>\n";
	$secret .= "  <uuid>$uuid</uuid>\n"; 
 	$secret .= "  <usage type='$SECRET_TYPE_CEPH'>\n";
  $secret .= "    <name>$uuid</name>\n";
  $secret .= "  </usage>\n";
	$secret .= "</secret>\n";

	run_cmd(1, 1, "virsh secret-define --file /dev/stdin", $secret);
	run_cmd(1, 1, "virsh secret-set-value $uuid --base64 $ceph_key");
	run_cmd(1, 0, "rm -f $secret_path");
}

sub refresh_session_and_get_iscsi_device {
  my ($netdev, $ip, $store, $lun) = @_;
  print STDERR "Refreshing iscsiadm session before fetching attached device\n";
  run_cmd(1, 0, "$ISCSIADM -m session -R");
  return get_iscsi_device($netdev, $ip, $store, $lun);
}

##############################################
# needed by module
return 1;
