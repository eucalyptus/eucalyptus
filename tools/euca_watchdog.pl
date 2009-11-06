#!/usr/bin/perl
#Copyright (c) 2009  Eucalyptus Systems, Inc.	
#
#This program is free software: you can redistribute it and/or modify
#it under the terms of the GNU General Public License as published by 
#the Free Software Foundation, only version 3 of the License.  
# 
#This file is distributed in the hope that it will be useful, but WITHOUT
#ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
#FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
#for more details.  
#
#You should have received a copy of the GNU General Public License along
#with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
#Please contact Eucalyptus Systems, Inc., 130 Castilian
#Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
#if you need additional information or have any questions.
#
#This file may incorporate work covered under the following copyright and
#permission notice:
#
#  Software License Agreement (BSD License)
#
#  Copyright (c) 2008, Regents of the University of California
#  
#
#  Redistribution and use of this software in source and binary forms, with
#  or without modification, are permitted provided that the following
#  conditions are met:
#
#    Redistributions of source code must retain the above copyright notice,
#    this list of conditions and the following disclaimer.
#
#    Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in the
#    documentation and/or other materials provided with the distribution.
#
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
#  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
#  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
#  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
#  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
#  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
#  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
#  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
#  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
#  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
#  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
#  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
#  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
#  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
#  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
#  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
#  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
#  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
#  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
#  ANY SUCH LICENSES OR RIGHTS.
#

#
# euca_watchdog - daemon for monitoring a Eucalyptus cloud, sending
#                 alerts via email, and killing long-running instances 
# 
#   OPTIONS:
#
#   -l --limit:           in seconds, how long instances are allowed to run
#                         default value is set below as a global variable
#   -e --exempt-file:     name of file containing usernames exempt from
#                         termination, one per line; this file is consulted
#                         upon every iteration, so it can be edited as the
#                         script runs
#   -c --checkpoint-file: name of the file were the script saves the list
#                         of instances and their timestamps so that the 
#                         latter are not lost if script is re-started
#   -m --max-instances:   the maximum number of instances allowed to
#                         each (non-exempt) user
#   -s --status-file:     print node availability to this file, including 
#                         a prediction of how soon, in seconds, an instance
#                         will be evicted
#   -h --high-watermark   send a notification if there are this many or
#                         more instances running (can be max or close to it)
#   -n --notify-email     send all notifications to this e-mail address
#   -n --no-killing        don't kill any instances, just print out info
#   -d --daemon:          run in a loop
#   -q --quiet:           no messages on the console, only write to files

use diagnostics;
use warnings; 
use sigtrap;
use strict;
use Getopt::Long;
use English; # for descriptive predefined var names, such as:
use Fcntl ':flock'; 
$OUTPUT_AUTOFLUSH = 1; # no output buffering

# globals
our $limit = 3600; # default limit, in seconds
our $max_instances = 4; # default max
our %def_exempt = ("eucalyptus" => 1, # default exemptions
                   "admin" => 1 ); 
our $chkpt_file = "/tmp/euca_watchdog.checkpoint";
our $status_file; 
our $exempt_file;
our %instances;
our $verbose = 1; # set to 1 for debugging
our $quiet = 0; 
our $peace = 0; # no killing if set to 1
our $notify_email; # no notifications if unset
our $high_watermark; # no high watermark notifications if unset
our $daemon = 0; # run in a loop
our $sleep_time = 30; # how frequently, in sec, we query Eucalyptus

# process command-line parameters
GetOptions('l|limit=i'           => \$limit,
           'e|exempt-file=s'     => \$exempt_file,
           'c|checkpoint-file=s' => \$chkpt_file,
           'm|max-instances=i'   => \$max_instances,
           's|status-file=s'     => \$status_file,
           'h|high-watermark=i'  => \$high_watermark,
           'a|notify-email=s'    => \$notify_email,
           'n|no-killing'        => sub { $peace = 1 },
           'd|daemon'            => sub { $daemon = 1 },
           'q|quiet'             => sub { $quiet = 1; $verbose = 0 }
		   ) or die "Unknown parameter: $!\n";

# ensure that the environment variables necessary for EC2 tools are set
sub check_env { if ( not defined $ENV{$_[0]} ) { error ("environment variable \$$_[0] is not set!") } }
check_env ("EC2_HOME");
check_env ("EC2_PRIVATE_KEY");
check_env ("EC2_CERT");
check_env ("EC2_URL");

# if checkpoint file exists and non-empty, pick up that info
if ( -e "$chkpt_file" ) {
    if ( open ( CHKPT, "<$chkpt_file" ) ) {
        unless (flock CHKPT, LOCK_EX | LOCK_NB) {
            warning ("file $chkpt_file already locked; waiting...");
            alarm 10;
            flock CHKPT, LOCK_EX or error ("failed to obtain lock on $chkpt_file");
        }

        while ( <CHKPT> ) {
            # format: INSTANCEID USERNAME TIMESTAMP
            if ( /(^[\w\-]+) ([\w\-]+) (\d+)$/ ) {
                $instances{$1} = [$2, $3];
            }
        }
        close CHKPT; # unlocks safely, too
    }
}
if ( ( scalar keys %instances ) > 0 ) {
    print "loaded instances from checkpoint file $chkpt_file:\n" unless $quiet;
    foreach my $key ( keys %instances ) {
        print "\t$key by $instances{$key}[0] noticed on $instances{$key}[1]\n" unless $quiet;
    }
}

print "instance time limit: $limit seconds\n" unless $quiet;
print "maximum instances allowed: $max_instances\n" unless $quiet;

our $first = 1; # we'll print out some stuff only on first iteration
our $mode; # remembers whether previous iteration was "down", "low", or "high"

do {

    # determine who is exempt by re-reading the exempt file every time, in case it's changed
    my %exempt = %def_exempt;
    if ( defined $exempt_file ) { 
        if ( open ( EXEMPT, "<$exempt_file" ) ) {
            while ( <EXEMPT> ) {
                if ( /([\w\-]+)/ ) {
                    $exempt{$1} = 1;
                }
            }
            close EXEMPT;
        } else {
            if ( $first ) {
                warning ("exemptions file $exempt_file could not be opened");
            }
        }
    }
    if ( $first and not $quiet ) {
        print "exempt users:";
        print map { " $_" } keys %exempt;
        print "\n";
    }
    
    print "\n" if $verbose; # to separate output from each iteration
    my $now = time;
    my $now_str = localtime ($now);
    print "now=$now ($now_str)\n" if $verbose;

    # get list of running instances
    my %old_instances = %instances;
    %instances = (); # we rebuild a new list every time based on what is running
    print "querying instances...\n" if $verbose;
    if ( open (INSTANCES, "ec2-describe-instances |") ) {
        my $user = "unknown";
        while ( <INSTANCES> ) {
            if ( /RESERVATION\s+(r-\w+)\s+([\w\-]+)/i ) {
                my $r_id = $1;
                $user = $2;
            }
            if ( /INSTANCE\s+(i-\w+)\s+(emi-\w+)\s+([\d\.]+)\s+([\d\.]+)\s+(\w+)/i ) {
                my $i_id = $1;
                my $e_id = $2;
                my $public_ip = $3;
                my $private_ip = $4;
                my $state = $5;
                if ( not defined $old_instances {$i_id} ) {
                    $instances {$i_id} = [$user, $now]; # add new entry 
                } else {
                    $instances {$i_id} = $old_instances {$i_id}; # keep the timestamp of the old one
                }
                print "\t$i_id $e_id $instances{$i_id}[1] $user $public_ip $private_ip $state\n" if $verbose;
            }
        }
        close INSTANCES;
    } else {
        error ( "failed to run ec2-describe-instances", 1 );
    }

    # save the list of instances to a checkpoint
    if ( open ( CHKPT, "+<$chkpt_file" ) 
         or open ( CHKPT, ">$chkpt_file" ) ) {
        unless (flock CHKPT, LOCK_EX | LOCK_NB) {
            warning ("file $chkpt_file already locked; waiting...");
            alarm 10;
            flock CHKPT, LOCK_EX or error ("failed to obtain lock on $chkpt_file", 1);
        }
        truncate CHKPT, 0 or error ("failed to truncate file $chkpt_file", 1);
        foreach my $key ( keys %instances ) {
            print CHKPT "$key $instances{$key}[0] $instances{$key}[1]\n";
        }
        close CHKPT;
    } else {
        warning ("failed to save a checkpoint in $chkpt_file");
    }

    # check on the status
    my $available = 0;
    my $total = 0;
    print "querying availability zones...\n" if $verbose;
    if ( open (ZONES, "ec2-describe-availability-zones |") ) {
        while ( <ZONES> ) {
            if ( /AVAILABILITYZONE[\s\|]+([\w\-]+)[\s\|]+([\w\-]+)[\s\|]+(\d+)\/(\d+)\s+([\w\-]+)[\s\|]+/i ) {
                my $zone_name = $1;
                my $zone_status = $2;
                my $zone_available = $3;
                my $zone_total = $4;
                my $zone_instance_type = $5;
                print "\t$zone_name $zone_status $zone_available/$zone_total $zone_instance_type\n" if $verbose;
                if ( $zone_status =~ /up/i ) {
                    $available += $zone_available;
                    $total += $zone_total;
                }
            }
        }
        close ZONES;
    } else {
        error ( "failed to run ec2-describe-availability-zones", 1);
    }

    # try to estimate the ealiest expiration, if any
    my $remains = 0; # no expirations expected
    if ( ( scalar keys %instances ) > 0 ) {
        # find the earliest timestamp
        my $earliest = $now; # should be bigger than all start timestamps
        my $qualifying_instances = 0; # i.e. not exempt
        foreach my $id ( keys %instances ) {
            my $user = $instances{$id}[0];
            my $started = $instances{$id}[1];
            if ( not defined $exempt{$user} and $started < $earliest ) { 
                $qualifying_instances++;
                $earliest = $started;
            }
        }

        if ( $qualifying_instances ) { # else, keep remains==0
            # conservatively, add detection latency and some padding
            $remains = ($earliest + $limit) - $now + $sleep_time + 5; 
            # negative number means we're close, so return detection latency
            if ( $remains <= 0 ) { $remains = $sleep_time; } 
        }
    }

    # state transition logic with email notifications for entering "down" mode,
    # leaving "down" mode, and "high" mode (except on the first iteration, when 
    # we assume the admin knows what's going on)
    if ( $total > 0 ) { # cloud is up
        my $running = $total - $available; # total instances running
        my $next_mode = "low"; # assume low, if high_watermark isn't defined
        if ( defined $high_watermark and $running >= $high_watermark ) {
            $next_mode = "high";
        }
        if ( defined $mode ) { # do not notify on the first iteration, when $mode is undefined
            if ( $mode ne "high" and $next_mode eq "high" ) {
                notify ("heavy load", "Heavy load on the cloud: $running instances, $available available slots\n", 1);                
            } elsif ( $mode eq "down" ) {
                notify ("cloud went up", "Cloud Controller reports $available available out of $total total slots\n", 1);
            }
            $mode = $next_mode;
        }
    } else { # cloud is down
        if ( defined $mode and $mode ne "down" ) {
            notify ("cloud went down", "Cloud Controller could not be contacted or reported 000/000\n", 1);
        }
        $mode = "down";
    }
    
    # write the status to a file
    my $status = "total=$total available=$available expiration=$remains\n";
    print $status  unless $quiet;
    if ( defined $status_file ) {
        if ( open ( STATUS, ">$status_file" ) ) {
            print STATUS $status;
            close STATUS;
        } else {
            if ( $first ) {
                warning ("failed to write to status file $status_file");
            }
        }
    }
    
    # killin' time...
    my $to_kill = "";
    my %count = ();
    ID: foreach my $key ( keys %instances ) {
        my $user = $instances{$key}[0];
        my $started = $instances{$key}[1];
        if ( not defined $count{$user} ) {
            $count{$user} = 1;
        } else {
            $count{$user}++;
        }
        if ( defined $exempt{$user} ) { next ID }
        if ( ( $started + $limit ) >= $now 
             and $count{$user} <= $max_instances ) { next ID }
        $to_kill .= " $key";
    }
    foreach my $user ( keys %count ) {
        if ( $count{$user} > $max_instances ) {
            print "user $user has $count{$user} instances, exceeding max-instances=$max_instances\n" unless $quiet;
        }
    }
    if ( $to_kill ne "" ) {
        print "instances qualifying for termination:$to_kill...\n" unless $quiet;
        if ( not $peace ) {
            my $error = shell_command_status ("ec2-terminate-instances $to_kill");
            print "ec2-terminate-instances returned $error\n";
        }
    }

    $first = 0;
    if ( $daemon) { 
        print "sleeping for $sleep_time seconds...\n" if $verbose;
        sleep ($sleep_time);
    }
} while ( $daemon );

########################################################################

# run a shell command synchronously and return its STDOUT and STDERR
sub shell_command_output {
    my ( $cmd ) = @_;
    my $output = "";

    $verbose and print "executing: $cmd\n";
    if ( open(TMP, "$cmd |") ) {
        my $terminator = $/;
        undef $/;
        $output = <TMP>;
        $/ = $terminator;
        close(TMP);
    }
    return $output; # returns "" if fork failed or if cmd returns error
}

# run a shell command synchronously and return its error status
sub shell_command_status {
    my ( $cmd ) = @_;

    $verbose and print "executing: $cmd\n";
    if ( system ($cmd) ) {
        if ($? == -1) {
            print STDERR "failed to execute: $!\n";
        } elsif ($? & 127) {
            printf STDERR "child died with signal %d, %s coredump\n",
            ($? & 127),  ($? & 128) ? 'with' : 'without';
        } else {
            $verbose and printf STDERR "child exited with value %d\n", $? >> 8;
        }
    }
    return $?;
}

sub notify {
    my ( $subject, $body, $notify ) = @_;

    if ( defined $notify_email ) {

        $verbose and print "notifying $notify_email\n";
        my $full_subject = "Eucalyptus watchdog ALERT: $subject";

        if ( open (MAIL, "| mailx -s '$full_subject' $notify_email") ) {
            print MAIL "$body";
            close MAIL;
        } else {
            print STDERR "failed to run mailx for notification\n";
        }
    }
}

sub warning {
    my ( $str ) = @_;
    
    print STDERR "WARNING: $str\n";
}

sub error {
    my ( $str, $notify ) = @_;
	
    print STDERR "ERROR: $str\n";
    if ( defined $notify and $notify != 0 ) {
        notify ("euca_watchdog.pl died", "euca_watchdog.pl died due to error:\n$str\n");
    }
    exit 1;
}

