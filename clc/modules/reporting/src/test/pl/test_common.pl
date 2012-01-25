

#
# test_common.pl contains constants and functions which are used by multiple
#   testing perl scripts.
#
# author: tom.werges
#
# (c)2011, Eucalyptus Systems, Inc. All Rights Reserved.
#

#
# Constants defined across files. These are infrequently-used parameters which
#   you can modify if you wish.
#
sub storage_usage_mb() { 2 }
sub snap_usage_mb() { 4 }
sub startup_sleep_duration() { 120 }
sub upload_file() { "random.dat" }
sub image_file() { "fedora.img" }
sub vol_device() { "/dev/sda5" }
sub injected_image_file() { "injected_image.img" }


#
# Common functions which are used across perl scripts
#
sub rand_str($) {
	return sprintf("%x",rand(2<<$_[0]));
}

sub runcmd($) {
	print STDERR "Running cmd:$_[0]\n";
	my $ret = system("$_[0] 1>&2");
	return $ret;
}


sub execute_query($) {
	print "Executing query:$_[0]\n";
	my $output = `./db.sh --execute="$_[0]" -D eucalyptus_reporting --skip-column-names`;
	print "Output:$output\n";
	return split("\n",$output);
}

# TEST_RANGE
#  var_name, expected, value, error
sub test_range($$$$) {
	my ($name,$expected,$val,$error) = @_;
	print "test:$name, expected:$expected +/- $error, val:$val\n";
	if ($val < $expected-$error || $val > $expected+$error) {
		print " FAILED: test $name\n";
		return 127;
	}
	return 0;
}

# TEST_EQ
#  var_name, expected, value
sub test_eq($$$) {
	my ($name,$expected,$val) = @_;
	print "test:$name, expected:$expected val:$val\n";
	if ($val != $expected) {
		print " FAILED: test $name\n";
		return 127;
	}
	return 0;
}

1;

