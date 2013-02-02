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

package edu.ucsb.eucalyptus.cloud.ws;

import java.net.URL;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Date;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;



//All HttpTransfer operations should be called asynchronously. The operations themselves are synchronous.
public class HttpTransfer {
	
	private static Logger LOG = Logger.getLogger(HttpTransfer.class);
	protected HttpClient httpClient;
	protected HttpMethodBase method;
		
	public HttpMethodBase constructHttpMethod(String verb, String addr, String eucaOperation, String eucaHeader) {
		String date = new Date().toString();
		String httpVerb = verb;
		String addrPath;
		try {
			java.net.URI addrUri = new URL(addr).toURI();
			addrPath = addrUri.getPath().toString();
			String query = addrUri.getQuery();
			if(query != null) {
				addrPath += "?" + query;
			}
		} catch(Exception ex) {
			LOG.error(ex, ex);
			return null;
		}
		String data = httpVerb + "\n" + date + "\n" + addrPath + "\n";

		HttpMethodBase method = null;
		if(httpVerb.equals("PUT")) {
			method = new  PutMethodWithProgress(addr);
		} else if(httpVerb.equals("DELETE")) {
			method = new DeleteMethod(addr);
		} else  {
			method = new GetMethod(addr);
		} 
		method.setRequestHeader("Authorization", "Euca");
		method.setRequestHeader("Date", date);
		//method.setRequestHeader("Expect", "100-continue");
		method.setRequestHeader(StorageProperties.EUCALYPTUS_OPERATION, eucaOperation);
		if(eucaHeader != null) {
			method.setRequestHeader(StorageProperties.EUCALYPTUS_HEADER, eucaHeader);
		}
		try {
			PrivateKey ccPrivateKey = SystemCredentials.lookup(Storage.class).getPrivateKey();
			Signature sign = Signature.getInstance("SHA1withRSA");
			sign.initSign(ccPrivateKey);
			sign.update(data.getBytes());
			byte[] sig = sign.sign();

			method.setRequestHeader("EucaSignature", new String(Base64.encode(sig)));
		} catch(Exception ex) {
			LOG.error(ex, ex);
		}
		return method;
	}
	
	public void abortTransfer() throws EucalyptusCloudException {
		if(httpClient != null && method != null) {
			method.abort();
			method.releaseConnection();
		}
	}

	public HttpTransfer() {}
}
