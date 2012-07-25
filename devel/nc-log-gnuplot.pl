#!/usr/bin/env perl

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

# nc-log-gnuplot.pl = produces a bash script that drives gnuplot
# to produce a PDF illustrating a timeline of instance events
#
# TODO: this may not work for many instances and long runs

use diagnostics;
use warnings; 
use sigtrap;
use strict;
use English; # for descriptive predefined var names
use Data::Dumper; # for debugging: print Dumper($var)
use Getopt::Long;
use Pod::Usage;
use POSIX;

# globals
our $verbose = 0;
$OUTPUT_AUTOFLUSH = 1; # no output buffering

# deal with command-line parameters
GetOptions('verbose|v' => sub { $verbose = 1; }) or die "unknown option";
if (@ARGV == 0) {
    printf STDERR "Error: at least one input file is required. E.g.,\n\n"
        . "\tgv `perl nc-log-gnuplot.pl nc.log | bash`\n\n";
    exit (1);
}

# main vars
my $ninstances = 0;
my $nfiles = 0;
my $nlines = 0;
my %instances;
my @indexes;
my $min_ts = 0;
my $max_ts = 0;
my $labels = "";

 FILE: foreach my $infile (@ARGV) {
     
     if (!open(IN, $infile)) {
         die "could not open $infile\n";
     }
     
     # read in the lines of the file
     my $ilines = 0;
     my $tlines = 0;
   LINE: while (<IN>) {
       chop; # newline
       my %months = (
           'Jan' => 1,
           'Feb' => 2,
           'Mar' => 3,
           'Apr' => 4,
           'May' => 5,
           'Jun' => 6,
           'Jul' => 7,
           'Aug' => 8,
           'Sep' => 9,
           'Oct' => 10,
           'Nov' => 11,
           'Dec' => 12
           );
       
       # e.g. [Thu Aug 18 15:39:27 2011][029052][EUCADEBUG ] [i-3A440720]
       if (/^\[([^\]]+)\]\s*\[([^\]]+)\]\s*\[([^\]]+)\]\s*\[([^\]]+)\]\s+(.*)$/) {
	   my $line = $_;
	   my $date = $1;
	   my $pid = $2;
	   my $level = $3;
	   my $id = $4;
	   my $msg = $5;
	   
	   # convert the timestamp
	   my $ts;
	   if ($date =~ /(...) (...) (\d+) (\d\d):(\d\d):(\d\d) (\d\d\d\d)/) {
	       my $mon = $months{$2};
	       defined $mon or die "failed to parse month ($2)";
	       $ts = strftime "%s", $6, $5, $4, $3, $mon, $7;
	   } else {
	       printf STDERR "could not parse timestamp: $line\n" if $verbose;
	       next LINE;
	   }

	   # find or pick an index for the instance
	   if ( not $id =~ /^i-/ ) {
	       printf STDERR "could not parse instance: $line\n" if $verbose;
	       next LINE;
	   }
	   my $index;
	   if (not defined $instances{$id}) {
	       $index = $ninstances++;
               $instances{$id}{index} = $index;
	       $instances{$id}{data} = "";
               $instances{$id}{lines} = 0;
               $indexes[$index] = $id;
	   } else {
	       $index = $instances{$id}{index};
	   }
	   
	   # process the message to pick out interesting events
	   my $event;
	   if ($msg =~ /(booting)/ ||
	       $msg =~ /(constructing)/ ||
	       $msg =~ /(copying)/ ||
	       $msg =~ /(injecting)/ ||
               $msg =~ /(destroying)/ ||
	       $msg =~ /(forgetting)/) {
	       $event = $1;
           } elsif ($msg =~ /(doRunInstance)/) {
               $event = "running";
	   } else {
	       next LINE;
	   }

           # keep track of earliest and latest timestamp
           if ($min_ts == 0 or $min_ts > $ts) {
               $min_ts = $ts;
           }
           if ($max_ts == 0 or $max_ts < $ts) {
               $max_ts = $ts;
           }

           # use seconds relative to the first seen event
           my $plot_ts = $ts - $min_ts;
           $instances{$id}{data} .= "$index\t$plot_ts\t$event\n";
           $instances{$id}{lines}++;
	   $ilines++;

       # e.g. [Thu Aug 18 15:39:27 2011][029052][EUCADEBUG ] {22341234234}
       } elsif (/^\[([^\]]+)\]\s*\[([^\]]+)\]\s*\[([^\]]+)\]\s*{([^}]+)}\s+(.*)$/) {
	   my $date = $1;
	   my $pid = $2;
	   my $level = $3;
	   my $tid = $4;
	   my $msg = $5;

	   # low-level messages tagged by thread and not instance ID

	   $tlines++;
       } else {
	   # either stack trace lines or DescribeResources, DescribeInstances, etc
       }

       $nlines++;
   }
     my $total = $ilines + $tlines;
     printf STDERR "found in $infile $nlines lines with $ilines instance lines and $tlines process lines\n" if $verbose;
     $nfiles++;
}

# produce a bash script that will invoke gnuplot 
# and feed it data via a temporary file

my $height_in = floor((($max_ts-$min_ts)*9)/1000.0);
my $width_in = floor($ninstances);
my $label_y = -30;
my $range_x = $ninstances;
my $pindex = 0; # index to use in the plot, since not every set may be selected
my $pcommands = ""; # gnuplot plot commands
my $plabels = ""; # gnuplot label commands
print <<_EOF;
#!/bin/bash

# write plot data into a temporary file
dfile=`tempfile`
echo \"
_EOF

INST: for (my $i=0; $i<$ninstances; $i++) {
    my $id = $indexes[$i];
    if ($instances{$id}{lines} < 1) {
        next INST;
    }
    print "# instance $id index $pindex\n";
    print "$instances{$id}{data}";
    if ($i!=($ninstances-1)) {
        print "\n\n";
    }
    if ($pindex > 0) {
        $pcommands .= ", \\\n";
    }
    my $lindex = $pindex+1;
    $pcommands .= "\t'\$dfile' index $pindex using 1:2 with lines, "
        . "'\$dfile' index $pindex using 1:2:3 with labels";
    $plabels .= "set label $lindex \"$id\" at $pindex,$label_y center\n";
    $pindex++;
}

print <<_EOF;
\" >\$dfile

# launch gnuplot
pfile=`tempfile`.pdf
gnuplot <<EOF
set output '\$pfile'
set term pdf size $width_in,$height_in
unset border
unset key
set yrange [$label_y:] reverse
set xrange [-1:$range_x]
set noxtics
$plabels
plot \\
$pcommands
EOF
echo \$pfile

# cleanup
rm \$dfile
_EOF
