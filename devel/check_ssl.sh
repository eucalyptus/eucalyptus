#!/bin/bash

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
