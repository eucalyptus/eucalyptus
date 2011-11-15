#!/usr/bin/perl

sub check_cc_state{
   test_start("check cc state");
   run_on_cc("grep localState= /opt/eucalyptus/var/log/eucalyptus/cc.log | tail -n 3", "failed to grep cc.log");
   test_end();
}

&check_cc_state;
