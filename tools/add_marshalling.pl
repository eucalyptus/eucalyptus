#!/usr/bin/env perl                                                                                                                                      

 FILE: for $file ( @ARGV ) {
     open (IN, "<$file") or next FILE;
     undef $/;
     $_ = <IN>;
     $/ = "\n";
     close IN;
     print "scanning $file\n";

     $done=0;
     if (s/     #include "([^"]+)"/#include "$1"\n#include <server-marshal.h>\n/s) {$done++;}
     if (s/adb_(\w+)_t\* axis2_skel_(\w+)_(\w+) \(([^\)]+) adb_(\w+)_t\* (\w+) [^}]+TODO[^}]+}/adb_$1_t* axis2_skel_$2_$3 ($4 adb_$5_t* $6 )\n        { return (${3}Marshal($6, env)); }/sg) {$done++;}
     if (s/adb_(\w+)_t\* axis2_skel_(\w+)_(\w+) \(const axutil_env_t \*env [^}]+TODO[^}]+}/adb_$1_t* axis2_skel_$2_$3 (const axutil_env_t \*env)\n        { return (${3}Marshal(env)); }/sg) {$done++;}
     
     if ($done)
     {

         print "   modifying $file\n";
         system "cp $file $file~";
         open (OUT, ">$file") or die "could not overwrite $file";
         print OUT;
         close OUT;
     }
}
