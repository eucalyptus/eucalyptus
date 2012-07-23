/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.auth.login;

import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.component.id.Storage;

public class WalrusComponentLoginModule extends BaseLoginModule<WalrusWrappedComponentCredentials> {
	private static Logger LOG = Logger.getLogger( WalrusComponentLoginModule.class );
	public WalrusComponentLoginModule() {}

	@Override
	public boolean accepts( ) {
		return super.getCallbackHandler( ) instanceof WalrusWrappedComponentCredentials;
	}

	@Override
	public boolean authenticate( WalrusWrappedComponentCredentials credentials ) throws Exception {
		Signature sig;
		boolean valid = false;
		String data = credentials.getLoginData();
		String signature = credentials.getSignature();
		try {
			try {
				PublicKey publicKey = SystemCredentials.lookup(Storage.class).getCertificate().getPublicKey();
				sig = Signature.getInstance("SHA1withRSA");
				sig.initVerify(publicKey);
				sig.update(data.getBytes());
				valid = sig.verify(Base64.decode(signature));
			} catch ( Exception e ) {
				LOG.warn ("Authentication: certificate not found in keystore");
			} finally {
				if( !valid && credentials.getCertString() != null ) {
					try {
						boolean found = false;
						X509Certificate nodeCert = Hashes.getPemCert( Base64.decode( credentials.getCertString() ) );
						for (Partition part : Partitions.list()) {
							if (nodeCert.equals(part.getNodeCertificate())) {
								found = true;
								break;
							}
						}
						if (!found) {
							throw new AuthenticationException("Invalid certificate");
						}
						if(nodeCert != null) {
							PublicKey publicKey = nodeCert.getPublicKey( );
							sig = Signature.getInstance( "SHA1withRSA" );
							sig.initVerify( publicKey );
							sig.update( data.getBytes( ) );
							valid = sig.verify( Base64.decode( signature ) );
						}
					} catch ( Exception e2 ) {
						LOG.error ("Authentication error: " + e2.getMessage());
						return false;
					}            
				}
			}
		} catch (Exception ex) {
			LOG.error ("Authentication error: " + ex.getMessage());
			return false;
		}

		if(valid) {					
			try {
				User user;
				String queryId = credentials.getQueryId();
				if(queryId != null) {
					user = Accounts.lookupUserByAccessKeyId(queryId);  
				} else {
					user = Accounts.lookupSystemAdmin( );	
				}
				super.setCredential(queryId);
				super.setPrincipal(user);
				return true;	
			} catch (AuthException e) {
				LOG.error(e);
				return false;
			}
		}
		return false;	
	}

	@Override
	public void reset( ) {}
}
