/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Neil Soman neil@eucalyptus.com
 */
package com.eucalyptus.auth.login;

import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;

import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.bootstrap.Component;

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
				PublicKey publicKey = SystemCredentialProvider.getCredentialProvider(Component.storage).getCertificate().getPublicKey();
				sig = Signature.getInstance("SHA1withRSA");
				sig.initVerify(publicKey);
				sig.update(data.getBytes());
				valid = sig.verify(Base64.decode(signature));
			} catch ( Exception e ) {
				LOG.warn ("Authentication: certificate not found in keystore");
			} finally {
				if( !valid && credentials.getCertString() != null ) {
					try {
						X509Certificate nodeCert = Hashes.getPemCert( Base64.decode( credentials.getCertString() ) );
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
					user = Users.lookupQueryId(queryId);  
				} else {
					user = Users.lookupUser( "admin" );			
					user.setAdministrator(true);
				}
				super.setCredential(queryId);
				super.setPrincipal(user);
				super.getGroups().addAll(Groups.lookupUserGroups( super.getPrincipal()));
				return true;	
			} catch (NoSuchUserException e) {
				LOG.error(e);
				return false;
			}
		}
		return false;	
	}

	@Override
	public void reset( ) {}
}
