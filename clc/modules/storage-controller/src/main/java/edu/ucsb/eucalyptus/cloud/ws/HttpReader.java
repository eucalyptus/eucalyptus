package edu.ucsb.eucalyptus.cloud.ws;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.WalrusDataMessage;



public class HttpReader extends HttpTransfer {

	private static Logger LOG = Logger.getLogger(HttpReader.class);

	private LinkedBlockingQueue<WalrusDataMessage> getQueue;
	private HttpClient httpClient;
	private HttpMethodBase method;
	private File file;
	private String tempPath;
	private boolean compressed;

	public HttpReader(String path, LinkedBlockingQueue<WalrusDataMessage> getQueue, File file, String eucaOperation, String eucaHeader) {
		this.getQueue = getQueue;
		this.file = file;
		httpClient = new HttpClient();

		String httpVerb = "GET";
		String addr = StorageProperties.WALRUS_URL + "/" + path;

		method = constructHttpMethod(httpVerb, addr, eucaOperation, eucaHeader);
	}

	public HttpReader(String path, LinkedBlockingQueue<WalrusDataMessage> getQueue, File file, String eucaOperation, String eucaHeader, boolean compressed, String tempPath) {
		this(path, getQueue, file, eucaOperation, eucaHeader);
		this.compressed = compressed;
		this.tempPath = tempPath;
	}

	public String getResponseAsString() {
		try {
			httpClient.executeMethod(method);
			InputStream inputStream;
			if(compressed) {
				inputStream = new GZIPInputStream(method.getResponseBodyAsStream());
			} else {
				inputStream = method.getResponseBodyAsStream();
			}

			String responseString = "";
			byte[] bytes = new byte[StorageProperties.TRANSFER_CHUNK_SIZE];
			int bytesRead;
			while((bytesRead = inputStream.read(bytes)) > 0) {
				responseString += new String(bytes, 0 , bytesRead);
			}
			method.releaseConnection();
			return responseString;
		} catch(Exception ex) {
			LOG.error(ex, ex);
		}
		return null;
	}

	private void getResponseToFile() {
		byte[] bytes = new byte[StorageProperties.TRANSFER_CHUNK_SIZE];
		FileOutputStream fileOutputStream = null;
		BufferedOutputStream bufferedOut = null;
		try {
			File outFile;
			if(compressed)
				outFile = new File(tempPath + File.separator + file.getName() + Hashes.getRandom(16) + ".gz");		
			else
				outFile = file;
			httpClient.executeMethod(method);
			InputStream httpIn;
			httpIn = method.getResponseBodyAsStream();
			int bytesRead;
			fileOutputStream = new FileOutputStream(outFile);
			bufferedOut = new BufferedOutputStream(fileOutputStream);
			while((bytesRead = httpIn.read(bytes)) > 0) {
				bufferedOut.write(bytes, 0, bytesRead);
			}
			bufferedOut.close();
			if(compressed) {
				try
				{
					Runtime rt = Runtime.getRuntime();
					Process proc = rt.exec(new String[]{ "/bin/gunzip", "-c", outFile.getAbsolutePath()});
					StreamConsumer error = new StreamConsumer(proc.getErrorStream());
					StreamConsumer output = new StreamConsumer(proc.getInputStream(), file);
					error.start();
					output.start();
					output.join();
					error.join();
				} catch (Throwable t) {
					LOG.error(t);
				}
				if(!outFile.delete()) {
					LOG.error("Unable to delete temporary file: " + outFile.getAbsolutePath());
				}
			}
		} catch (Exception ex) {
			LOG.error(ex, ex);
		} finally {
			method.releaseConnection();
			if(bufferedOut != null) {
				try {
					bufferedOut.close();
				} catch (IOException e) {
					LOG.error(e);	
				}
			}
			if(fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					LOG.error(e);	
				}
			}
		}
	}

	private void getResponseToQueue() {
		byte[] bytes = new byte[StorageProperties.TRANSFER_CHUNK_SIZE];
		try {
			httpClient.executeMethod(method);
			InputStream httpIn = method.getResponseBodyAsStream();
			int bytesRead;
			getQueue.add(WalrusDataMessage.StartOfData(0));
			while((bytesRead = httpIn.read(bytes)) > 0) {
				getQueue.add(WalrusDataMessage.DataMessage(bytes, bytesRead));
			}
			getQueue.add(WalrusDataMessage.EOF());
		} catch (Exception ex) {
			LOG.error(ex, ex);
		} finally {
			method.releaseConnection();
		}
	}

	public void run() {
		if(getQueue != null) {
			getResponseToQueue();
		} else if(file != null) {
			getResponseToFile();
		}
	}

	public String getResponseHeader(String headerName) {
		try {
			httpClient.executeMethod(method);
			Header value = method.getResponseHeader(headerName);
			method.releaseConnection();
			if(value != null)
			    return value.getValue();
		} catch(Exception ex) {
			LOG.error(ex, ex);
		}
		return null;
	}
}
