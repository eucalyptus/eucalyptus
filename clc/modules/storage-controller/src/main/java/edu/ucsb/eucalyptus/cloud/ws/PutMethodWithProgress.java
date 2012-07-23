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
			if(totalBytesProcessed > 0) {
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
