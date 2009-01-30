#!/usr/bin/perl

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

#$MOUNT=untaint(`which mount`);
#$UMOUNT=untaint(`which umount`);
#$MOUNT=untaint("/tmp/euca_mountwrap");
#$UMOUNT=untaint("/tmp/euca_mountwrap");
$MKDIR=untaint(`which mkdir`);
$RMDIR=untaint(`which rmdir`);
$CHOWN=untaint(`which chown`);
$CHMOD=untaint(`which chmod`);
$MKTEMP=untaint(`which mktemp`);
$TUNE2FS=untaint(`which tune2fs`);
$LOSETUP=untaint(`which losetup`);

# check binaries
if (!-x $MKDIR || !-x $RMDIR || !-x $CHOWN || !-x $CHMOD || !-x $MKTEMP || !-x $LOSETUP) {
    print STDERR "add_key cannot find all required binaries\n";
    do_exit(1);
}

# check input params
$mounter = untaint(shift @ARGV);
$offset = untaint(shift @ARGV);
$img = untaint(shift @ARGV);
$key = shift @ARGV; # untaint later

$tmpfile = "";
$loopdev = "";
$mounted = 0;
$attached = 0;

if (!-f "$img" || !-x "$mounter") {
    print STDERR "add_key cannot verify inputs: mounter=$mounter img=$img\n";
    do_exit(1);
}
if ($offset eq "") {
    $offset = 0;
}

chomp($tmpfile = untaint(`$MKTEMP -d`));
if (! -d "$tmpfile") {
    print STDERR "no dir: $tmpfile";
    do_exit(1);
}

# find loop dev and attach image to it
for ($i=0; $i<10 && !$attached; $i++) {
    $loopdev=untaint(`$LOSETUP -f`);
    if ($loopdev ne "") {
	if ($offset == 0) {
	    $rc = system("$LOSETUP $loopdev $img");
	} else {
	    $rc = system("$LOSETUP -o $offset $loopdev $img");
	}
	if (!$rc) {
	    $attached = 1;
	} else {
	    system("$LOSETUP -d $loopdev");
	}
    }
}
if (!$attached) {
    print STDERR "cannot attach a loop device\n";
    do_exit(1);
}

if (system("$TUNE2FS -c 0 -i 0 $loopdev >/dev/null 2>&1")) {
    print STDERR "cmd: $TUNE2FS -c 0 -i 0 $loopdev\n";
#    do_exit(1);
}

# without a key, add_key.pl just runs tune2fs
if (not defined($key)) {
    do_exit(0);
}

$key = untaint($key);
if (!-f "$key") {
    print STDERR "add_key cannot verify inputs: key=$key\n";
    do_exit(1);
}

if (system("$mounter mount $loopdev $tmpfile")) {
    print STDERR "cannot mount: $mounter mount $loopdev $tmpfile\n";
    do_exit(1);
}
$mounted = 1;

if ( !-d "$tmpfile/root/.ssh" ) {
    if (system("$MKDIR $tmpfile/root/.ssh")) {
	print STDERR "cmd: $MKDIR $tmpfile/root/.ssh\n"; 
	do_exit(1);
    }
    system("$CHOWN root $tmpfile/root/.ssh");
    system("$CHMOD 0700 $tmpfile/root/.ssh");
}

if (!open(OFH, ">>$tmpfile/root/.ssh/authorized_keys")) {
    print STDERR "cannot write to: $tmpfile/root/.ssh/authorized_keys\n"; 
    do_exit(1);
}
print OFH "\n";
if (!open(FH, "$key")) {
    print STDERR "cannot read from: $key\n"; 
    do_exit(1);
}
while(<FH>) {
    chomp;
    print OFH "$_\n";
}
close(FH);
close(OFH);

system("$CHOWN root $tmpfile/root/.ssh/authorized_keys");
system("$CHMOD 0600 $tmpfile/root/.ssh/authorized_keys");
do_exit(0);

sub do_exit() {
    $e = shift;

    if ($mounted && ($tmpfile ne "")) {
	system("$mounter umount $tmpfile");
    }
    if ($attached && ($loopdev ne "")) {
	system("$LOSETUP -d $loopdev");
    }
    if ($tmpfile ne "") {
	system("$RMDIR $tmpfile");
    }
    exit($e);
}

sub untaint() {
    $str = shift;
    if ($str =~ /^([ &:#-\@\w.]+)$/) {
	$str = $1; #data is now untainted
    } else {
	$str = "";
    }
    return($str);
}
