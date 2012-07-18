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

package edu.ucsb.eucalyptus.cloud.ws;

import java.net.URL;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Date;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.util.StorageProperties;



//All HttpTransfer operations should be called asynchronously. The operations themselves are synchronous.
public class HttpTransfer {
	
	private static Logger LOG = Logger.getLogger(HttpTransfer.class);

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

	public HttpTransfer() {}
}
