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

sub describe_services {
   test_start("euca-describe-services");
   run_on_clc("$euca_dir/usr/sbin/euca-describe-services", "euca-describe-services failed");
   test_end();
}

sub cluster_proxy_state {
   test_start("list cluster proxy states");
   run_on_clc("$euca_dir/usr/sbin/euca-modify-property -p euca=\'[\\\"enabled\\\":com.eucalyptus.cluster.Clusters.getInstance().listValues().collect{ it.stateMachine }, \\\"disabled\\\":com.eucalyptus.cluster.Clusters.getInstance().listDisabledValues().collect{ it.stateMachine }]\'",
             "listing failed");
   test_end();
}

sub db_connections {
  test_start("check database connection states");
  run_on_clc("$euca_dir/usr/sbin/euca-modify-property -p euca=\'com.eucalyptus.entities.PersistenceContexts.list().collect{ \\\"\\\\n\\\" + it + \\\"=> ENABLED \\\" + com.eucalyptus.bootstrap.Databases.lookup(it).getActiveDatabases() + \\\" DISABLED \\\" + com.eucalyptus.bootstrap.Databases.lookup(it).getInactiveDatabases()  }\'","failed to get database connection pool info.");
  test_end();
}

sub drbd_dstate {
   test_start("drbd dstate");
   run_on_clc("drbdadm dstate all", "drbdadm dstate failed");
   test_end();
}

sub drbd_cstate {
   test_start("drbd cstate");
   run_on_clc("drbdadm state all", "drbdadm cstate failed");
   test_end();
}

sub check_mysqld {
   test_start("if mysqld is running on clc");
   check_on_clc("ps afx | grep mysqld | grep -v grep");
   test_end();
}

sub host_membership {
   test_start("host membership map");
   run_on_clc("$euca_dir/usr/sbin/euca-modify-property -p euca=\'com.eucalyptus.bootstrap.Hosts.hostMap.values().collect{ it }\'");
   test_end();
}
sub gms_members {
   test_start("GMS membership list");
   run_on_clc("$euca_dir/usr/sbin/euca-modify-property -p euca=\'com.eucalyptus.bootstrap.Hosts.hostMap.getChannel( ).getView( ).getMembers( )\'");
   test_end();
}
sub coordinator_local {
   test_start("is coordinator?");
   run_on_clc("$euca_dir/usr/sbin/euca-modify-property -p euca=\'\\\"Coordinator: \\\" + com.eucalyptus.bootstrap.Hosts.isCoordinator() + \\\" => \\\" + com.eucalyptus.bootstrap.Hosts.getCoordinator()\'");
   run_on_clc("$euca_dir/usr/sbin/euca-modify-property -p euca=\'\\\"Localhost:         => \\\" + com.eucalyptus.bootstrap.Hosts.localHost()\'");
   test_end();
}

sub drbd_role {
   test_start("drbdadm role all");
   run_on_clc("drbdadm role all", "rbdadm role all failed");
   test_end(); 
}
&host_membership;
&gms_members;
&coordinator_local;
&describe_services;
&cluster_proxy_state;
&drbd_dstate;
&drbd_cstate;
&drbd_role;
&check_mysqld;
&db_connections;
