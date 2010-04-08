package edu.ucsb.eucalyptus.cloud.ws;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;

import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;


public class HttpWriter extends HttpTransfer {

	private HttpClient httpClient;
	private HttpMethodBase method;
	public HttpWriter(String httpVerb, String bucket, String key, String eucaOperation, String eucaHeader) {
		httpClient = new HttpClient();
		String walrusAddr = StorageProperties.WALRUS_URL;
		if(walrusAddr != null) {
			String addr = walrusAddr + "/" + bucket + "/" + key;
			method = constructHttpMethod(httpVerb, addr, eucaOperation, eucaHeader);
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
			method = constructHttpMethod(httpVerb, addr, eucaOperation, eucaHeader);
			if(method != null) {
				method.setRequestHeader("Transfer-Encoding", "chunked");
				method.addRequestHeader(StorageProperties.StorageParameters.EucaSnapSize.toString(), size);
				((PutMethodWithProgress)method).setOutFile(file);
				((PutMethodWithProgress)method).setCallBack(callback);
			}
		}
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