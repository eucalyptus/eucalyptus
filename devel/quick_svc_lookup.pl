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

my $COL_SVCTYPE = 2;
my $COL_PART = 3;
my $COL_COMPNAME=4;
my $COL_SVCSTATE = 5;
my $COL_SVCADDR=7;
my $COL_SVCARN=8;

%svc_state_map = undef;   # key=> ip, value = hash of servicetype=>state; the value is the local view of the state

sub read_svc_state {
    open (OLDOUT, ">&STDOUT");
    open (STDOUT, "> /tmp/quick_svc_compare.out");

    &run_on_clc("$euca_dir/usr/sbin/euca-describe-services", "euca-describe-services failed");
    close(STDOUT);
    open(STDOUT, ">&OLDOUT");
    close(OLDOUT);

    my @lines = &read_file("/tmp/quick_svc_compare.out");

    my $ip = undef;
    my @fields=undef;
    foreach $line (@lines){
        chomp($line);
        if($line =~ /\[[0-9.]+\]/){
           @fields = split /[\[\]]/, $line;
           $ip = $fields[1];
        }else{
           @fields = split /\s+/, $line;
           my $key = $fields[$COL_SVCTYPE].":".$fields[$COL_COMPNAME];
           my $value = $fields[$COL_SVCSTATE];
           $svc_state_map{$ip}->{$key} = $value;
        }
    }
}

@svc_to_print=qw(eucalyptus walrus storage cluster vmwarebroker);

sub include{
    foreach $relavant (@svc_to_print){
         if ($_[0] =~ /$relavant/){
              return 1;
         }
    }
   return 0;
}
sub print_state {
   test_start("Service-State Comparison");
    foreach $ip (keys %svc_state_map){  ## column --> ip
      foreach $ip2 (keys %svc_state_map){  ## column --> ip
        %map = %{$svc_state_map{$ip2}};
        foreach $key (keys %map){  # key=SVC_TYPE:COMP_NAME, val=state;
          $svc_state_map{$ip}->{$key} = "------" if ! defined $svc_state_map{$ip}->{$key} && exists $svc_state_map{$ip2}->{$key};
        }
      }
    }

    my %line_map = undef; # key==>svctype:comp_id, val==>state separated by IP
    $header = sprintf("%-18.18s","COMPONENTS");
    foreach $ip (keys %svc_state_map){  ## column --> ip
        $header.=sprintf("%-15.15s",$ip);
        %map = %{$svc_state_map{$ip}};
       
        foreach $key (keys %map){  # key=SVC_TYPE:COMP_NAME, val=state;
             $state = $map{$key};
             if(&include($key) && $ip !~ /^$/ ){
                  $line_map{$key} .= sprintf("%-15.15s",$state); 
             }  
        }  
    }
    print $header."\n";
    foreach $key (sort (keys %line_map)){
         print sprintf("%-32.30s %s\n", $key, $line_map{$key}); 
    }

    test_end();
}

&read_svc_state;
&print_state;
