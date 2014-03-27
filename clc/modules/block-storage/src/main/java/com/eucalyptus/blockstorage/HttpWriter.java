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

package com.eucalyptus.blockstorage;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.storage.common.CallBack;
import com.eucalyptus.util.EucalyptusCloudException;


@Deprecated
public class HttpWriter extends HttpTransfer {
	private static Logger LOG = Logger.getLogger(HttpWriter.class);

	public HttpWriter(String httpVerb, String bucket, String key, String eucaOperation, String eucaHeader) {
		httpClient = new HttpClient();
		String walrusAddr = StorageProperties.WALRUS_URL;
		if(walrusAddr != null) {
			String addr = walrusAddr + "/" + bucket + "/" + key;
			method = constructHttpMethod(httpVerb, addr, eucaOperation, eucaHeader, true);
		}
	}

	public HttpWriter(String httpVerb, File file, String size, CallBack callback, String bucket, String key, String eucaOperation, String eucaHeader, Map<String, String> httpParameters) {

		httpClient = new HttpClient();
		String walrusAddr = StorageProperties.WALRUS_URL;
		if(walrusAddr != null) {
			String addr = walrusAddr + "/" + bucket + "/" + key;
			Set<String> paramKeySet = httpParameters.keySet();
			boolean first = true;
			for(String paramKey : paramKeySet) {
				if(!first) {
					addr += "&";
				} else {
					addr += "?";
				}
				first = false;
				addr += paramKey;
				String value = httpParameters.get(paramKey);
				if(value != null)
					addr += "=" + value;
			}
			method = constructHttpMethod(httpVerb, addr, eucaOperation, eucaHeader, false);
			if(method != null) {				
				method.addRequestHeader(StorageProperties.StorageParameters.EucaSnapSize.toString(), size);				
				method.setRequestHeader("Transfer-Encoding", "chunked");
				
				//Sign the request
				signEucaInternal(method);
				
				((PutMethodWithProgress)method).setOutFile(file);
				((PutMethodWithProgress)method).setCallBack(callback);
			}	
		}
	}

	private String print() {
		StringBuilder requestString = new StringBuilder();
		for(Header h : method.getRequestHeaders()) {
			requestString.append("Header name: " + h.getName() + " = " + h.getValue() + "\n");
		}		
		
		return requestString.toString();
	}
	
	public void run() throws EucalyptusCloudException {
		try {
			httpClient.executeMethod(method);
			method.releaseConnection();
		} catch (Exception ex) {
			throw new EucalyptusCloudException("error transferring", ex);
		}
	}
}
