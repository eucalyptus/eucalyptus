#!/usr/bin/perl
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
