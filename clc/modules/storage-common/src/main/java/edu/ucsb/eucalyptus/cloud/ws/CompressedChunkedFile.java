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
import java.io.RandomAccessFile;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.stream.ChunkedInput;

import edu.ucsb.eucalyptus.constants.IsData;

public class CompressedChunkedFile implements ChunkedInput, IsData {
	private Logger LOG = Logger.getLogger( CompressedChunkedFile.class );
	private RandomAccessFile file;
	private long offset;
	private int CHUNK_SIZE = 8192;
	private long fileLength;

	public CompressedChunkedFile(RandomAccessFile file, long fileLength) {
		this.file = file;
		this.offset = 0;
		this.fileLength = fileLength;
	}

	public CompressedChunkedFile(RandomAccessFile file, long start, long end, int chunkSize) {
		this.file = file;
		this.offset = start;
		this.fileLength = end;
		CHUNK_SIZE = chunkSize;
	}
	
	@Override
	public void close() throws Exception {
		file.close();
	}

	@Override
	public boolean hasNextChunk() throws Exception {
		return offset < fileLength && file.getChannel().isOpen();
	}

	@Override
	public Object nextChunk() throws Exception {
		long offset = this.offset;
		if (offset >= fileLength) {
			return null;
		}
		int chunkSize = (int) Math.min(CHUNK_SIZE, fileLength - offset);
		byte[] chunk = new byte[chunkSize];
		file.readFully(chunk);
		this.offset = offset + chunkSize;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream zip = new GZIPOutputStream(out);
		zip.write(chunk);
		zip.close();
		return ChannelBuffers.wrappedBuffer(out.toByteArray());
	}	
}

