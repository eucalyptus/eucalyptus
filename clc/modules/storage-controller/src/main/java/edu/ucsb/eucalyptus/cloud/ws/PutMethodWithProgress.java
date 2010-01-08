package edu.ucsb.eucalyptus.cloud.ws;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.httpclient.ChunkedOutputStream;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PutMethod;

import com.eucalyptus.util.StorageProperties;


public class PutMethodWithProgress extends PutMethod {
	private File outFile;
	private CallBack callback;

	public PutMethodWithProgress(String path) {
		super(path);
	}

	public void setOutFile(File outFile) {
		this.outFile = outFile;
	}

	public void setCallBack(CallBack callback) {
		this.callback = callback;
	}

	@Override
	protected boolean writeRequestBody(HttpState state, HttpConnection conn) throws IOException {
		InputStream inputStream;
		if (outFile != null) {
			inputStream = new FileInputStream(outFile);

			ChunkedOutputStream chunkedOut = new ChunkedOutputStream(conn.getRequestOutputStream());
			byte[] buffer = new byte[StorageProperties.TRANSFER_CHUNK_SIZE];
			int bytesRead;
			int numberProcessed = 0;
			long totalBytesProcessed = 0;
			while ((bytesRead = inputStream.read(buffer)) > 0) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				GZIPOutputStream zip = new GZIPOutputStream(out);
				zip.write(buffer, 0, bytesRead);
				zip.close();
				chunkedOut.write(out.toByteArray());
				totalBytesProcessed += bytesRead;
				if(++numberProcessed >= callback.getUpdateThreshold()) {
					callback.run();
					numberProcessed = 0;
				}
			}
			if(totalBytesProcessed == outFile.length()) {
				callback.finish();
			} else {
				callback.failed();
			}
			chunkedOut.finish();				
			inputStream.close();
		} else {
			return false;
		}
		return true;
	}
}