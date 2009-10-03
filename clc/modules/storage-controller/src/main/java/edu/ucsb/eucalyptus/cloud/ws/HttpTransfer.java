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

import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.bootstrap.Component;
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
			PrivateKey ccPrivateKey = SystemCredentialProvider.getCredentialProvider(Component.storage).getPrivateKey();
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