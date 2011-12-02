#!/usr/bin/perl

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

