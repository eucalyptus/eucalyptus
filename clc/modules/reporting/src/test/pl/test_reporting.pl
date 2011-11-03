#!/usr/bin/perl

use strict;

# Params: num_users num_users_per_account interval duration image+ 

my $user_num = shift;
my $num_users_per_account = shift;

my @usernames = ();
my @accountnames = ();

sub rand_str($) {
	return sprintf("%x",rand(2<<$_[0]));
}


# Create users and accounts, download credentials, and establish dir for each user
my $account_num = "";
for (my $i=0; $i<($user_num*$num_users_per_account); $i++) {
	if ($i % $num_users_per_account == 0) {
		$account_num = rand_str(16);
	}
	my $account_name = "account-$account_num";
	push(@accountnames, $account_name);
	my $user_name = "user-$account_num-" . rand_str(32);
	push(@usernames, $user_name);
	print "account:$account_name user:$user_name\n";
	`euare-accountcreate -a $account_name` and die("Couldn't create account:$account_name");
	`euare-usercreate --delegate $account_name -p / -u $user_name` and die("Couldn't create user:$user_name");
	`euca-get-credentials -a $account_name -u $user_name creds-$account_name-$user_name.zip` and die("Couldn't get credentials:$user_name");
	`(mkdir credsdir-$account_name-$user_name; cd credsdir-$account_name-$user_name; unzip ../creds-$account_name-$user_name.zip)` and die("Couldn't unzip credentials:$user_name");
}


# Run simulate_usage with values

# Run check_db with values

# Generate CSV reports
#   Verify instance CSV
#   Verify S3 CSV
#   Verify Storage CSV
#   How to get correct values?

# Run simulate_negative_usage with value

# Run negative_check_db with values

