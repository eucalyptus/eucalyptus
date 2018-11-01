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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpChunk;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;

@ConfigurableClass(root = "objectstorage", description = "Streaming upload channel configuration.")
public class ChannelBufferStreamingInputStream extends InputStream {
  private static final Logger LOG = Logger.getLogger(ChannelBufferStreamingInputStream.class);

  //This field controls the size of the queue of channel buffers used for uploads.
  //A large queue will require more memory and may possibly cause an OOM condition if
  //enough heap space is not provided to the JVM.
  //A smaller queue will limit the number of concurrent requests that can be handled
  //but will be more suitable when memory is constrained.
  @ConfigurableField(description = "Channel buffer queue size for uploads",
      displayName = "objectstorage.uploadqueuesize",
      initialInt = 20 )
  public static int QUEUE_SIZE = 20;

  @ConfigurableField(description = "Channel buffer queue timeout (in seconds)",
      displayName = "objectstorage.uploadqueuetimeout",
      initialInt = 1 )
  public static int QUEUE_TIMEOUT = 1;

  private final Channel channel;
  private final LinkedBlockingQueue<ChannelBuffer> buffers = new LinkedBlockingQueue<>(QUEUE_SIZE);
  private final AtomicBoolean closed = new AtomicBoolean( false );
  private final Long contentLength;
  private long bytesRead = 0;
  private ChannelBuffer buffer; // current buffer

  public ChannelBufferStreamingInputStream(
      @Nullable Channel channel,
      @Nonnull ChannelBuffer buffer,
      @Nullable Long contentLength ) {
    this.channel = channel;
    this.buffer = buffer;
    this.contentLength = contentLength;
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public int available( ) {
    int currentlyAvailable = buffer.readableBytes();
    if (currentlyAvailable <= 0 && ( ( contentLength == null || contentLength > bytesRead ) )) {
      //maybe this buffer is exhausted
      //get the next buffer and report available
      //we only scan 1 buffer ahead
      int retries = 0;
      do {
        try {
          final ChannelBuffer nextBuffer = buffers.poll(QUEUE_TIMEOUT, TimeUnit.SECONDS);
          if ( nextBuffer != null ) {
            buffer = nextBuffer;
            currentlyAvailable = buffer.readableBytes();
            if ( readComplete( 0 ) ) {
              break;
            }
          }
        } catch (InterruptedException e) {
          LOG.error(e, e);
          return currentlyAvailable;
        }
      } while (currentlyAvailable <= 0 && retries++ < 60);
    }
    return currentlyAvailable;
  }

  @Override
  public void mark(int readlimit) {
  }

  @Override
  public synchronized int read( @Nonnull byte[] bytes, int off, int len ) throws IOException {
    if (len > 0) {
      if (off < 0) {
        throw new IOException("Invalid offset: " + off);
      }
      if (off + len > bytes.length) {
        throw new IOException("Byte buffer is too small. Should be at least: " + (off + len));
      }
      if (closed.get()) {
        throw new IOException("Stream closed");
      }
      try {
        int readSoFar = 0;
        int readable;
        int toReadFromThisBuffer;
        while ( len > 0 ) {
          readable = buffer.readableBytes( );
          if ( readable > 0 ) {
            if ( len > readable ) {
              toReadFromThisBuffer = readable;
            } else {
              toReadFromThisBuffer = len;
            }
            buffer.readBytes( bytes, off, toReadFromThisBuffer );
            len = len - toReadFromThisBuffer;
            readSoFar += toReadFromThisBuffer;
            off += toReadFromThisBuffer;
          } else if ( readSoFar == 0 && readComplete( 0 ) ) {
            return -1;
          } else if ( readComplete( readSoFar ) ) {
            bytesRead += readSoFar;
            return readSoFar;
          } else {
            try {
              int retries = 0;
              do {
                final ChannelBuffer nextBuffer = buffers.poll( QUEUE_TIMEOUT, TimeUnit.SECONDS );
                if ( nextBuffer != null ) {
                  buffer = nextBuffer;
                  checkResume( );
                }
              } while ( buffer.readableBytes( ) <= 0 && !closed.get( ) && retries++ < 60 );
              if ( readComplete( 0 ) ) {
                bytesRead += readSoFar;
                return readSoFar;
              } else if ( buffer.readableBytes( ) <= 0 ) {
                if ( closed.get( ) ) {
                  throw new IOException( "Stream closed" );
                }
                LOG.error( "No more data in this stream" );
                bytesRead += readSoFar;
                return readSoFar;
              }
            } catch ( InterruptedException e ) {
              LOG.error( e, e );
              bytesRead += readSoFar;
              return readSoFar;
            }
          }
        }
        bytesRead += readSoFar;
        return readSoFar;
      } finally {
        checkResume( );
      }
    } else {
      return 0;
    }
  }

  @Override
  public int read( ) throws IOException {
    byte[] buffer = new byte[1];
    int read = read( buffer, 0, 1 );
    if ( read < 0 ) {
      return -1;
    } else {
      return Byte.toUnsignedInt( buffer[0] );
    }
  }

  @Override
  public void reset( ) throws IOException {
    throw new IOException("mark/reset not supported");
  }

  @Override
  public long skip(long n) throws IOException {
    return super.skip(n); // calls read which does bytes read accounting
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
    } else {
      checkSuspend( );
    }
  }

  @Override
  public void close( ) {
    LOG.trace("Closing Channel Stream: " + buffers.remainingCapacity() + " " + buffers.size());
    closed.set( true );
  }

  private boolean readComplete( int additionalByteCount ) {
    return
        ( contentLength != null && contentLength <= ( bytesRead + additionalByteCount ) ) ||
        ( contentLength == null && buffer == HttpChunk.LAST_CHUNK.getContent( ) && buffers.isEmpty( ) );
  }

  private boolean canPutChunks( ) {
    return canPutChunks( Math.max( 1, QUEUE_SIZE / 10 ) );
  }

  private boolean canPutChunks( int count ) {
    return buffers.remainingCapacity( ) >= count;
  }

  private void checkSuspend( ) {
    if ( channel != null && !canPutChunks( ) ) {
      channel.setReadable( false );
    }
  }

  private void checkResume( ) {
    if ( channel != null && canPutChunks( ) ) {
      channel.setReadable( true );
    }
  }
}
