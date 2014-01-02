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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.pipeline.auth;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.blockstorage.Storage;

public class ObjectStorageComponentLoginModule extends BaseLoginModule<ObjectStorageWrappedComponentCredentials> {
	private static Logger LOG = Logger.getLogger( ObjectStorageComponentLoginModule.class );
	public ObjectStorageComponentLoginModule() {}

	@Override
	public boolean accepts( ) {
		return super.getCallbackHandler( ) instanceof ObjectStorageWrappedComponentCredentials;
	}

	@Override
	public boolean authenticate( ObjectStorageWrappedComponentCredentials credentials ) throws Exception {
		Signature sig;
		boolean valid = false;
		String data = credentials.getLoginData();
		String signature = credentials.getSignature();
		boolean found = false;
		X509Certificate signingCert = null;
		
		try {
			//Find which cert to use based on the fingerprint, which is in the certstring.
			String scFingerprint = SystemCredentials.lookup(Storage.class).getCertFingerprint();
			
			//Check the SC first
			if(scFingerprint.equals(credentials.getCertMD5Fingerprint())) {
				found = true;
				signingCert = SystemCredentials.lookup(Storage.class).getCertificate();
			}
			else {
				//Check the NCs and CCs credentials for a match
				for(Partition part : Partitions.list()) {
					if(X509CertHelper.calcFingerprint(part.getCertificate()).equals(credentials.getCertMD5Fingerprint())) {
						signingCert = part.getCertificate();
						found = true;
						break;
					}
					else if(X509CertHelper.calcFingerprint(part.getNodeCertificate()).equals(credentials.getCertMD5Fingerprint())) {
						signingCert = part.getNodeCertificate();
						found = true;
						break;
					}				
				}
			}
			
			if (!found) {
				throw new AuthenticationException("Invalid certificate");
			}

			if(signingCert != null) {
				PublicKey publicKey = signingCert.getPublicKey( );
				sig = Signature.getInstance( "SHA256withRSA" );
				sig.initVerify( publicKey );
				sig.update(data.getBytes());				
				valid = sig.verify( Base64.decode( signature ) );
			}
		} catch ( Exception e2 ) {
			LOG.error ("Authentication error: " + e2.getMessage());
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
