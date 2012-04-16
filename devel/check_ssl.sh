#!/bin/bash 
(   # get openssl, gnutls, and bouncycastle versions 
    openssl version; 
    gnutls-cli-debug -v; 
    unzip -p /usr/share/eucalyptus/lib/bcprov.jar META-INF/MANIFEST.MF | grep Implementation-Version
    # check openssl {ssl2,ssl3,tls1} functionality against port 8774 and 8443 
    for p in 8773 8443; do 
        for f in serverpref ssl3 tls1 tls1_2 tls1_1; do  
    		( 
    		  echo -e "======= CHECKING $p WITH $f ======="; 
    		  echo -e 'GET /\n\n' | openssl s_client -connect 127.0.0.1:$p -quiet -$f 2>&1 >/dev/null  
    		) | xargs -i echo "127.0.0.1:$p openssl $f {}" 
    	done
    done  
    # check gnutls functionality against port 8774 and 8443
    for p in 8773 8443; do 
    	(
	       echo -e "======= CHECKING $p WITH gnutls ======="; 
            gnutls-cli-debug -p $p 127.0.0.1  
      ) | xargs -i echo "127.0.0.1:$p gnutls {}"; 
    done
)

