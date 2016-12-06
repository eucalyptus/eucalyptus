package com.eucalyptus.objectstorage.pipeline.handlers;

import com.eucalyptus.http.MappingHttpRequest;
import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.CompositeChannelBuffer;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunk;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.partitioningBy;
import static org.bouncycastle.asn1.x500.style.RFC4519Style.c;

/**
 * Represents a stream of AWS chunks possibly consisting of a stream of HTTP chunks. Only the current chunk is cached internally.
 */
public class AwsChunkStream {
  private static final int MAX_BUFFER_COMPONENTS = 1024;
  private static final byte EQUALS = "=".getBytes(Charsets.UTF_8)[0];
  private static final byte SEMICOLON = ";".getBytes(Charsets.UTF_8)[0];

  StreamingHttpRequest currentRequest = new StreamingHttpRequest();

  /**
   * A streaming Http request that encapsulates one or more Http chunks necessary to complete one or more Aws chunks. A single Http chunk
   * may complete multiple Aws chunks or multiple Http chunks may be needed to complete a single Aws chunk.
   */
  public static class StreamingHttpRequest {
    MappingHttpRequest initialRequest;
    List<HttpChunk> httpChunks = new ArrayList<>();
    List<AwsChunk> awsChunks;

    StreamingHttpRequest() {
      awsChunks = new ArrayList<>();
    }

    StreamingHttpRequest(HttpChunk httpChunk, List<AwsChunk> awsChunks) {
      httpChunks.add(httpChunk);
      this.awsChunks = awsChunks;
    }

    /**
     * Appends the httpChunk and returns true if the append causes an AwsChunk to be completed.
     */
    boolean append(HttpChunk httpChunk) {
      httpChunks.add(httpChunk);
      AwsChunk currentChunk = getOrCreateCurrentAwsChunk();
      currentChunk.appendContent(httpChunk.getContent().copy());

      if (!currentChunk.isParsed())
        currentChunk.parseChunkInfo();

      boolean chunkCompleted = false;
      while (currentChunk.isComplete()) {
        chunkCompleted = true;
        if (currentChunk.isFinal()) {
          break;
        } else {
          currentChunk = new AwsChunk(currentChunk);
          awsChunks.add(currentChunk);
          currentChunk.parseChunkInfo();
        }
      }

      return chunkCompleted;
    }

    private AwsChunk getOrCreateCurrentAwsChunk() {
      AwsChunk result;
      if (awsChunks.isEmpty()) {
        result = new AwsChunk();
        awsChunks.add(result);
      } else {
        result = awsChunks.get(awsChunks.size() - 1);
      }

      return result;
    }

    void setInitialRequest(MappingHttpRequest initialRequest) {
      this.initialRequest = initialRequest;
    }
  }

  public static class AwsChunk {
    public String previousSignature;
    public int chunkSize = -1;
    public String chunkSizeHex;
    public String chunkSignature;
    public int payloadStartIndex;
    private ChannelBuffer contents;

    AwsChunk() {
    }

    AwsChunk(AwsChunk previousChunk) {
      previousSignature = previousChunk.chunkSignature;
      // Next chunk starts at payload end + CRLF
      int nextChunkStartIndex = previousChunk.payloadEndIndex() + 2;

      // Create contents as a slice from previous chunk
      int readableBytes = previousChunk.contents.readableBytes();
      int sliceLen = readableBytes - nextChunkStartIndex;
      contents = readableBytes > nextChunkStartIndex ? previousChunk.contents.slice(nextChunkStartIndex, sliceLen) : null;
    }

    boolean isParsed() {
      return chunkSignature != null;
    }

    boolean isComplete() {
      return chunkSignature != null && contents.readableBytes() >= payloadEndIndex();
    }

    boolean isFinal() {
      return chunkSize == 0;
    }

    int payloadEndIndex() {
      return payloadStartIndex + chunkSize;
    }

    public ByteBuffer getPayload() {
      return isComplete() ? contents.slice(payloadStartIndex, chunkSize).toByteBuffer() : null;
    }

    public HttpChunk toHttpChunk() {
      ChannelBuffer httpChunkContent = isComplete() ? contents.slice(payloadStartIndex, chunkSize) : null;
      return new DefaultHttpChunk(httpChunkContent);
    }

    public ChannelBuffer getContents() {
      return isComplete() ? contents.slice(0, payloadStartIndex + chunkSize) : null;
    }

    private void appendContent(ChannelBuffer newContent) {
      if (contents == null) {
        contents = newContent;
      } else if (contents instanceof CompositeChannelBuffer) {
        CompositeChannelBuffer composite = (CompositeChannelBuffer) contents;
        if (composite.numComponents() >= MAX_BUFFER_COMPONENTS) {
          contents = ChannelBuffers.wrappedBuffer(composite.copy(), newContent);
        } else {
          List<ChannelBuffer> decomposed = composite.decompose(0, composite.readableBytes());
          ChannelBuffer[] buffers = decomposed.toArray(new ChannelBuffer[decomposed.size() + 1]);
          buffers[buffers.length - 1] = newContent;
          contents = ChannelBuffers.wrappedBuffer(buffers);
        }
      } else {
        contents = ChannelBuffers.wrappedBuffer(contents, newContent);
      }
    }

    /**
     * Parses chunk length and signature.
     */
    private void parseChunkInfo() {
      if (contents != null) {
        int sigStartIndex = 0;
        byte lastByte = 0;

        for (int i = 0; i < contents.readableBytes(); i++) {
          byte b = contents.getByte(i);

          if (chunkSizeHex == null) {
            if (b == SEMICOLON) {
              byte[] size = new byte[i];
              contents.getBytes(0, size, 0, size.length);
              chunkSizeHex = new String(size);
              chunkSize = (int) Long.parseLong(chunkSizeHex, 16);
            }
          } else {
            if (b == EQUALS) {
              sigStartIndex = i + 1;
            } else if (sigStartIndex != 0 && lastByte == '\r' && b == '\n') {
              byte[] signature = new byte[i - sigStartIndex - 1];
              contents.getBytes(sigStartIndex, signature, 0, signature.length);
              chunkSignature = new String(signature);
              payloadStartIndex = i + 1;
              break;
            }

            lastByte = b;
          }
        }
      }
    }
  }

  /**
   * Appends the HttpChunk to the AwsChunkStream and returns a StreamingHttpRequest if the append caused one or more AwsChunks to be
   * completed, else returns null.
   */
  public StreamingHttpRequest append(HttpChunk httpChunk) {
    if (currentRequest.append(httpChunk)) {
      HttpChunk incompleteHttpChunk = currentRequest.httpChunks.remove(currentRequest.httpChunks.size() - 1);
      Map<Boolean, List<AwsChunk>> awsChunkPartition = currentRequest.awsChunks.stream().collect(partitioningBy(AwsChunk::isComplete));
      List<AwsChunk> completeAwsChunks = awsChunkPartition.get(Boolean.TRUE);
      List<AwsChunk> incompleteAwsChunks = awsChunkPartition.get(Boolean.FALSE);

      StreamingHttpRequest result = currentRequest;
      result.awsChunks = completeAwsChunks;
      currentRequest = new StreamingHttpRequest(incompleteHttpChunk, incompleteAwsChunks);
      return result;
    } else {
      return null;
    }
  }
}