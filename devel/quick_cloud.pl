#!/usr/bin/perl
sub describe_services {
   test_start("euca-describe-services");
   run_on_clc("$euca_dir/usr/sbin/euca-describe-services", "euca-describe-services failed");
   test_end();
}

sub enabled_clusters {
   test_start("list enabled clusters");
   run_on_clc("$euca_dir/usr/sbin/euca-modify-property -p euca=\'com.eucalyptus.cluster.Clusters.getInstance().listValues().collect{ it.stateMachine }\'",
             "listing failed");
   test_end();
}

sub disabled_clusters {
   test_start("list disabled clusters");
   run_on_clc("$euca_dir/usr/sbin/euca-modify-property -p euca=\'com.eucalyptus.cluster.Clusters.getInstance().listDisabledValues().collect{ it.stateMachine }\'",
             "listing failed");
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

sub coordinator_local {
   test_start("is coordinator?");
   run_on_clc("$euca_dir/usr/sbin/euca-modify-property -p euca=\'com.eucalyptus.bootstrap.Hosts.Coordinator.INSTANCE.isLocalhost( )\'");
   test_end();
}

sub drbd_role {
   test_start("drbdadm role all");
   run_on_clc("drbdadm role all", "rbdadm role all failed");
   test_end(); 
}

&describe_services;
&enabled_clusters;
&disabled_clusters;
&drbd_dstate;
&drbd_cstate;
&check_mysqld;
&coordinator_local;
&drbd_role;
