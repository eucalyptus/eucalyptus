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

sub fill_in_state_map {
     my %state_map = (NOTREADY=>"NOTREADY",STOPPED=>"STOPPED",INITIALIZED=>"INITIALIZED",LOADED=>"LOADED",PRIMORDIAL=>"PRIMORDIAL", ENABLED=>"ENABLED",DISABLED=>"DISABLED",BROKEN=>"BROKEN");
     %state_map;
}

sub is_auto_transit {
    my ($from,$to) = ($_[0], $_[1]);
    
    if($from eq "NOTREADY" and $to eq "DISABLED") {
        1;}
    else{ 
        0;}
}

sub read_output {
    open FH, "$out_file"; # or {print "can't open the file $out_file\n"; return 1};
    my @lines = <FH>;
    close FH;
    @lines = "@lines"."\n";
}

sub describe_services {
   $comp = $_[0];
   $cmd = "$sbin_dir"."/euca-describe-services | grep "."$comp > $out_file 2>&1";
   $rc = system("$cmd");
  
   if($rc){
       $err_str = "euca-describe-services failed for component $comp\n";
       return;
   }
    
   my @output = &read_output;
   return @output; 
}

sub get_svc_state {
    my $comp = $_[0];
    my $IDX_STATE = 4;

    my @output = describe_services $comp;
    my $state = "invalid";
    if ($#output>=0) { 
       my @words = split(" ",$output[0]);
       return $words[$IDX_STATE];
    }else{
       $err_str = "[ERROR] Can't determine the service's state\n";
       return undef;
    }
    return $state; 
}


sub get_svc_name {
     my $comp = $_[0];
     my $IDX_NAME = 3;
     my @output = describe_services $comp;
  
     if ($#output >=0){
        my @words = split(" ", $output[0]);
        return $words[$IDX_NAME];
     }else{
        $err_str = "[ERROR] Can't determine the service's name\n";
        return undef;
     } 
}   


sub modify_svc_state { 
    if (not defined $_[0] or not defined $_[1]){
       $err_str="modify service state requires target state and service name\n";
       return 1; } 
      
     my $state = $_[0]; 
     my $svc = $_[1];
  #  print "modifying $svc\'s state via $state\n";
    system("rm -f $outfile");
    $cmd = "$sbin_dir"."/euca-modify-service -s $state $svc > $out_file 2>&1";
    #print "$cmd\n";
    my $rc = system($cmd);
    if ($rc) { 
       $err_str="Couldn't modify service\n";
       if (-e $out_file){ 
          my @output = &read_output; 
          $err_str .= "@output";
       }       
       return 1;
    }
    
    my @output = &read_output;
    foreach $line (@output){
        if ($line =~/RESPONSE true/){
              return 0;
        }
    } 
    return 1;
}

sub print_usage {
   print "Usage: ./ha_transit service_name state_transition\n";
   print "Example) ./ha_transit vb-192.168.23.73 \"INITIALIZED->LOADED->NOTREADY\"\n";
   print "Make sure EUCALYPTUS variable is set \n";
}

sub setup {
   if (not defined $ENV{"EUCALYPTUS"})
   {
      print "EUCALYPTUS not set\n";
      &print_usage; exit 1;
   }
   if (not defined $ENV{"EC2_URL"})
   {  print "EC2_URL not defined; did you source eucarc?\n";
      exit 1;  
   }

   if ($#ARGV < 1){
     &print_usage;
     exit 1;
   }

   foreach (@ARGV){
      if (m/help/i){  &print_usage; exit 1; }
   }

   $euca_home = $ENV{"EUCALYPTUS"};;
   $sbin_dir = "$euca_home"."/usr/sbin";
   $out_file ="/tmp/cmd.out";
   $pause_sec = 10;
   $err_str ="";
   system("rm -f $out_file");
   return;
}

############# MAIN #################
&setup;
%state_map = &fill_in_state_map;
$component = shift(@ARGV);
@states = split(/[^a-zA-Z]+/,join("",@ARGV));

print "Testing \'$component\'"." for transition \'@states\'\n";
#### test if the hash has the key defined for the given state
foreach $st (@states){
   if (not exists $state_map{$st})
   {   
       print "the state \'$st\' isn't a valid state\n";
       exit 1;
   } 
} 

$state = get_svc_state "$component";
$svc_name = get_svc_name "$component";

print "$svc_name\'s STARTING STATE: $state\n";

##### do state transition ###### 
my $state_action = undef;
my $prev_state=$state;
my $i=0;
foreach $to (@states){
    $state_action = $state_map{$to}; 
    print "\'$prev_state\' --> \'$to($state_action)\' ";
    $rc = modify_svc_state "$state_action","$svc_name";
    if($rc){
       print " [FAILED]\n";
       print "Reason: ".$err_str;
       exit 1;
    }
    $i=0;
    while ($i++ < $pause_sec){ 
        system ("sleep 1");
        print ".";
    }
    $now = get_svc_state $component;

    if ($now eq $to or is_auto_transit($to, $now)){
        print " [DONE]\n";
    }else
    {
        print " [FAILED: reported state = $now]\n"; 
        print "Reason: ".$err_str;
        exit 1;
    }
    $prev_state=$now;
}

print "Service transition completed!\n";
exit 0;
