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
$img = untaint(shift @ARGV);
$key = shift @ARGV; # untaint later
$tmpfile = "";
$loopdev = "";

if (!-f "$img" || !-x "$mounter") {
    print STDERR "add_key cannot verify inputs: mounter=$mounter img=$img\n";
    do_exit(1);
}


if (system("$TUNE2FS -c 0 -i 0 $img >/dev/null 2>&1")) {
    print STDERR "cmd: $TUNE2FS -c 0 -i 0 $img\n";
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

chomp($tmpfile = untaint(`$MKTEMP -d`));
if (! -d "$tmpfile") {
    print STDERR "no dir: $tmpfile";
    do_exit(1);
}

$attached = 0;
for ($i=0; $i<10 && !$attached; $i++) {
    $loopdev=untaint(`$LOSETUP -f`);
    $rc = system("$LOSETUP $loopdev $img");
    if ($loopdev ne "" && !$rc) {
	if (!system("$mounter mount $loopdev $tmpfile")) {
	    $attached = 1;
	}
    }
}
if (!$attached) {
    print STDERR "cannot mount: $mounter -o loop $img $tmpfile\n";
    do_exit(1);
}

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

    if ($tmpfile ne "") {
	system("$mounter umount $tmpfile");
	if ($loopdev ne "") {
	    system("$LOSETUP -d $loopdev");
	    system("$RMDIR $tmpfile");
	}
    }
    exit($e);
}

sub untaint() {
    $str = shift;
    if ($str =~ /^([ &:#-\@\w.]+)$/) {
	$str = $1; #data is now untainted
    } else {
	print STDERR "add_key inputs are tainted\n";
	$str = "";
    }
    return($str);
}
