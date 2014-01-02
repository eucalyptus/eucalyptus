/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.util;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.EucalyptusCloudException;

@ConfigurableClass( root = "objectstorage", description = "Streaming upload channel configuration.")
public class ChannelBufferStreamingInputStream extends ChannelBufferInputStream {
	private ChannelBuffer b;
	private LinkedBlockingQueue<ChannelBuffer> buffers;
	private int bytesRead;

	//This field controls the size of the queue of channel buffers used for uploads.
	//A large queue will require more memory and may possibly cause an OOM condition if
	//enough heap space is not provided to the JVM.
	//A smaller queue will limit the number of concurrent requests that can be handled
	//but will be more suitable when memory is constrained.
	@ConfigurableField( description = "Channel buffer queue size for uploads", displayName = "objectstorage.uploadqueuesize")
	public static int QUEUE_SIZE = 100;
	
	@Override
	public boolean markSupported() {
		return super.markSupported();
	}

	private static final Logger LOG = Logger.getLogger(ChannelBufferStreamingInputStream.class); 

	@Override
	public int available() throws IOException {
		if (b == null) {
			return 0;
		}
		int currentlyAvailable = b.readableBytes();
		if (currentlyAvailable <= 0) {
			//maybe this buffer is exhausted
			//get the next buffer and report available
			//we only scan 1 buffer ahead
			int retries = 0;
			do {
				try {
					b = buffers.poll(1, TimeUnit.SECONDS);
					currentlyAvailable += b.readableBytes();
				} catch (InterruptedException e) {
					LOG.error(e, e);
					return currentlyAvailable;
				}
			} while((b == null) && retries++ < 60);
		}
		return currentlyAvailable;
	}

	@Override
	public void mark(int readlimit) {
		super.mark(readlimit);
	}

	@Override
	public synchronized int read(byte[] bytes, int off, int len) throws IOException {
		if (len > 0) {
			if (b == null) {
				return -1;
			}
			if (off < 0) {
				throw new IOException("Invalid offset: " + off);
			}
			if(off + len > bytes.length) {
				throw new IOException("Byte buffer is too small. Should be at least: " + (off + len));
			}
			int readSoFar = 0;
			int readable = 0;
			int toReadFromThisBuffer = 0;
			while (len > 0) {					
				readable = b.readableBytes();
				toReadFromThisBuffer = 0;
				if(readable > 0) {
					if (len > readable) {
						toReadFromThisBuffer = readable;
					} else {
						toReadFromThisBuffer = len; 
					}
					b.readBytes(bytes, off, toReadFromThisBuffer);
					len = len - toReadFromThisBuffer;
					readSoFar += toReadFromThisBuffer;
					off += readSoFar;
				} else {
					try {
						int retries = 0;
						do {
							b = buffers.poll(1, TimeUnit.SECONDS);
						} while ((b == null) && retries++ < 60);
						if (b == null) {
							LOG.error("No more data in this stream");
							bytesRead += readSoFar;
							return readSoFar;
						}
					} catch (InterruptedException e) {
						LOG.error(e, e);
						bytesRead += readSoFar;
						return readSoFar;
					}

				}
			}
			bytesRead += readSoFar;
			return readSoFar;
		} else {
			return 0;
		}
	}

	@Override
	public int read() throws IOException {
		return super.read();
	}

	@Override
	public int readBytes() {
		return bytesRead;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.netty.buffer.ChannelBufferInputStream#skip(long)
	 * 
	 * This will effectively discard anything in the stream up to n. 
	 * In this implementation, you cannot go back once you have skipped past.
	 */
	@Override
	public long skip(long n) throws IOException {
		return super.skip(n);
	}

	public ChannelBufferStreamingInputStream(ChannelBuffer buffer) {
		super(buffer);
		buffers = new LinkedBlockingQueue<ChannelBuffer>(QUEUE_SIZE);
		b = buffer;
		bytesRead = 0;
		try {
			boolean success = false;
			int retries = 0;
			while ((!success) && (retries++ < 20)) {
				success = buffers.offer(buffer, 1, TimeUnit.SECONDS);
			}
			if (!success) {
				LOG.error("Timed out writing data to stream.");
			}
		} catch (InterruptedException e) {
			LOG.error(e, e);			
		}
	}

	public void putChunk(ChannelBuffer input) throws InterruptedException, EucalyptusCloudException {
		boolean success = false;
		int retries = 0;
		while ((!success) && (retries++ < 20)) {
			success = buffers.offer(input, 1, TimeUnit.SECONDS);
		}
		if (!success) {
			LOG.error("Timed out writing data to stream.");
			throw new EucalyptusCloudException("Aborting upload, could not process data in time. Either increase the upload queue size or retry the upload later.");
		}
	}

	@Override
	public void close() throws IOException {
		LOG.trace("Closing Channel Stream: " + buffers.remainingCapacity() + " " + buffers.size());
		super.close();
	}
}
