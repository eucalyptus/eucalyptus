/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.util;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ConfigurableClass(root = "objectstorage", description = "Streaming upload channel configuration.")
public class ChannelBufferStreamingInputStream extends ChannelBufferInputStream {
  private static final Logger LOG = Logger.getLogger(ChannelBufferStreamingInputStream.class);

  //This field controls the size of the queue of channel buffers used for uploads.
  //A large queue will require more memory and may possibly cause an OOM condition if
  //enough heap space is not provided to the JVM.
  //A smaller queue will limit the number of concurrent requests that can be handled
  //but will be more suitable when memory is constrained.
  @ConfigurableField(description = "Channel buffer queue size for uploads",
      displayName = "objectstorage.uploadqueuesize",
      initialInt = 128 )
  public static int QUEUE_SIZE = 128;

  @ConfigurableField(description = "Channel buffer queue timeout (in seconds)",
      displayName = "objectstorage.uploadqueuetimeout",
      initialInt = 1 )
  public static int QUEUE_TIMEOUT = 1;

  private ChannelBuffer b;
  private LinkedBlockingQueue<ChannelBuffer> buffers;
  private int bytesRead;
  private AtomicBoolean closed = new AtomicBoolean( false );

  @Override
  public boolean markSupported() {
    return super.markSupported();
  }

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
          b = buffers.poll(QUEUE_TIMEOUT, TimeUnit.SECONDS);
          currentlyAvailable += b.readableBytes();
        } catch (InterruptedException e) {
          LOG.error(e, e);
          return currentlyAvailable;
        }
      } while ((b == null) && retries++ < 60);
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
      if (off + len > bytes.length) {
        throw new IOException("Byte buffer is too small. Should be at least: " + (off + len));
      }
      if (closed.get()) {
        throw new IOException("Stream closed");
      }
      int readSoFar = 0;
      int readable = 0;
      int toReadFromThisBuffer = 0;
      while (len > 0) {
        readable = b.readableBytes();
        if (readable > 0) {
          if (len > readable) {
            toReadFromThisBuffer = readable;
          } else {
            toReadFromThisBuffer = len;
          }
          b.readBytes(bytes, off, toReadFromThisBuffer);
          len = len - toReadFromThisBuffer;
          readSoFar += toReadFromThisBuffer;
          off += toReadFromThisBuffer;
        } else {
          try {
            int retries = 0;
            do {
              b = buffers.poll(QUEUE_TIMEOUT, TimeUnit.SECONDS);
            } while ((b == null) && !closed.get( ) && retries++ < 60);
            if (b == null) {
              if (closed.get()) {
                throw new IOException("Stream closed");
              }
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
    buffers = new LinkedBlockingQueue<>(QUEUE_SIZE);
    b = buffer;
    bytesRead = 0;
    try {
      boolean success = false;
      int retries = 0;
      while ((!success) && (retries++ < QUEUE_TIMEOUT)) {
        success = buffers.offer(buffer, QUEUE_TIMEOUT, TimeUnit.SECONDS);
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
    while ((!success) && (retries++ < QUEUE_TIMEOUT)) {
      success = buffers.offer(input, QUEUE_TIMEOUT, TimeUnit.SECONDS);
    }
    if (!success) {
      LOG.error("Timed out writing data to stream, closing.");
      IO.close( this );
      throw new EucalyptusCloudException("Aborting upload, could not process data in time. Either increase the upload queue size or retry the upload later.");
    } 
  }

  @Override
  public void close() throws IOException {
    LOG.trace("Closing Channel Stream: " + buffers.remainingCapacity() + " " + buffers.size());
    super.close();
    closed.set( true );
  }
}
