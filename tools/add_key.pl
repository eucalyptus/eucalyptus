#!/usr/bin/perl

delete @ENV{qw(IFS CDPATH ENV BASH_ENV)};
$ENV{'PATH'}='/bin:/usr/bin:/sbin:/usr/sbin/';

$MOUNT=untaint(`which mount`);
$UMOUNT=untaint(`which umount`);
$MKDIR=untaint(`which mkdir`);
$RMDIR=untaint(`which rmdir`);
$CHOWN=untaint(`which chown`);
$CHMOD=untaint(`which chmod`);
$MKTEMP=untaint(`which mktemp`);
$TUNE2FS=untaint(`which tune2fs`);

# check binaries
if (!-x $MOUNT || !-x $UMOUNT || !-x $MKDIR || !-x $RMDIR || !-x $CHOWN || !-x $CHMOD || !-x $MKTEMP) {
    print STDERR "add_key cannot find all required binaries\n";
    do_exit(1);
}

# check input params
$img = untaint(shift @ARGV);
$key = untaint(shift @ARGV);
$tmpfile = "";
if (!-f "$key" || !-f "$img") {
    print STDERR "add_key cannot verify inputs: key=$key img=$img\n";
    do_exit(1);
}

if (system("$TUNE2FS -c 0 -i 0 $img >/dev/null 2>&1")) {
    print STDERR "cmd: $TUNE2FS -c 0 -i 0 $img\n";
#    do_exit(1);
}

chomp($tmpfile = untaint(`$MKTEMP -d`));
if (! -d "$tmpfile") {
    print STDERR "no dir: $tmpfile";
    do_exit(1);
}

$attached = 0;
for ($i=0; $i<10 && !$attached; $i++) {
    if (!system("$MOUNT -o loop $img $tmpfile")) {
	$attached = 1;
    }
}
if (!$attached) {
    print STDERR "cannot mount: $MOUNT -o loop $img $tmpfile\n";
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

    if (-d "$tmpfile") {
	system("$UMOUNT $tmpfile");
	system("$RMDIR $tmpfile");
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
