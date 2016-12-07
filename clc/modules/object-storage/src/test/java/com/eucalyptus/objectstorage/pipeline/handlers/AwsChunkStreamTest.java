package com.eucalyptus.objectstorage.pipeline.handlers;

import com.eucalyptus.auth.login.Hmacv4LoginModule;
import com.eucalyptus.objectstorage.pipeline.handlers.AwsChunkStream.AwsChunk;
import com.eucalyptus.objectstorage.pipeline.handlers.AwsChunkStream.StreamingHttpRequest;
import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AwsChunkStreamTest {
  private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc tortor metus, sagittis eget "
      + "augue ut,\n" + "feugiat vehicula risus. Integer tortor mauris, vehicula nec mollis et, consectetur eget tortor. In ut\n" +
      "elit" + " " + "sagittis, ultrices est ut, iaculis turpis. In hac habitasse platea dictumst. Donec laoreet tellus\n" + "at auctor "
      + "tempus. " + "Praesent" + " nec diam sed urna sollicitudin vehicula eget id est. Vivamus sed laoreet\n" + "lectus. Aliquam " +
      "convallis condimentum" + " risus, vitae" + " porta justo venenatis vitae. Phasellus vitae nunc\n" + "varius, volutpat quam nec, "
      + "mollis urna. Donec tempus, " + "nisi vitae gravida " + "facilisis, sapien sem malesuada\n" + "purus, id semper libero ipsum " +
      "condimentum nulla. Suspendisse vel mi " + "leo. Morbi pellentesque " + "placerat congue.\n" + "Nunc sollicitudin nunc diam, nec "
      + "hendrerit dui commodo sed. Duis dapibus " + "commodo elit, id commodo erat\n" + "congue id. Aliquam erat volutpat.\n";
  private static final byte[] FINAL_CHUNK = new byte[0];
  private static final String CRLF = "\r\n";
  private static final String CHUNK_SIGNATURE_HEADER = ";chunk-signature=";
  private static final int END_CHUNK_OFFSET = 8370;
  private static final int MID_CHUNK_PAYLOAD_OFFSET = 9000;
  private static final int MID_CHUNK_LEN_DECLARATION_OFFSET = 8372;
  private static final int MID_CHUNK_SIG_DECLARATION_OFFSET = 8384;
  private static final int MID_CHUNK_SIG_OFFSET = 8411;
  private static final String TEST_STRING = buildString(4096 * 6 + 1000);
  private static final String CHUNKED_STRING = chunkString(TEST_STRING, 4096);

  AwsChunkStream stream;

  @Before
  public void beforeMethod() {
    stream = new AwsChunkStream();
  }

  @Test
  public void testAppendChunksCleanlySplit() throws Exception {
    assertPayloadSplitOn(END_CHUNK_OFFSET);
  }

  @Test
  public void testAppendChunksSplitOnPayload() throws Exception {
    assertPayloadSplitOn(MID_CHUNK_PAYLOAD_OFFSET);
  }

  @Test
  public void testAppendChunksSplitOnLenDeclaration() throws Exception {
    assertPayloadSplitOn(MID_CHUNK_LEN_DECLARATION_OFFSET);
  }

  @Test
  public void testAppendChunksSplitOnSigDeclaration() throws Exception {
    assertPayloadSplitOn(MID_CHUNK_SIG_DECLARATION_OFFSET);
  }

  @Test
  public void testAppendChunksSplitOnSig() throws Exception {
    assertPayloadSplitOn(MID_CHUNK_SIG_OFFSET);
  }

  private List<AwsChunk> assertPayloadSplitOn(int index) {
    // Given
    String str1 = CHUNKED_STRING.substring(0, index);
    String str2 = CHUNKED_STRING.substring(index);
    ChannelBuffer part1Content = ChannelBuffers.copiedBuffer(str1, Charsets.UTF_8);
    ChannelBuffer part2Content = ChannelBuffers.copiedBuffer(str2, Charsets.UTF_8);

    // When
    StreamingHttpRequest part1Chunks = stream.append(new DefaultHttpChunk(part1Content));
    StreamingHttpRequest part2Chunks = stream.append(new DefaultHttpChunk(part2Content));

    // Then
    assertEquals(2, part1Chunks.awsChunks.size());
    assertEquals(6, part2Chunks.awsChunks.size());

    List<AwsChunk> allChunks = new ArrayList<>();
    allChunks.addAll(part1Chunks.awsChunks);
    allChunks.addAll(part2Chunks.awsChunks);
    assertChunks(allChunks);
    return part2Chunks.awsChunks;
  }

  private void assertChunks(List<AwsChunk> chunks) {
    assertChunk(chunks.get(0), 4096, "9f63a819511c7ee3d0dd464715825e117b6556475e5b4217e203328b3c8523eb");
    assertChunk(chunks.get(1), 4096, "ddccac04688b78d3581fb4832043760709ae73f9bf58b825fd5bfcda0cd429f3");
    assertChunk(chunks.get(2), 4096, "515052f47f69235db5da3a12f108b13d09ba5668c2c0f0b0be0d9cd0979b69a8");
    assertChunk(chunks.get(3), 4096, "e2b18125a38f5f67e9c00f08cecf9e74d316e816af035b6dff411acd5af346cd");
    assertChunk(chunks.get(4), 4096, "32cb3ade0e4dff0f83c5e5ff6762feb3c96363676d227307e4214c90339e37dc");
    assertChunk(chunks.get(5), 4096, "42a60ca16f1b4b7b9f77c847fc73defcd616542064e86e4ecbf0c8aa5e741a58");
    assertChunk(chunks.get(6), 1000, "f9a09d1d93ea40d3863de5337a1c417cea4ba6918c4ddd303cb625330a8d1450");
    assertChunk(chunks.get(7), 0, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  private void assertChunk(AwsChunk chunk, int chunkSize, String signature) {
    assertEquals(chunkSize, chunk.chunkSize);
    assertEquals(signature, chunk.chunkSignature);
  }

  /**
   * Builds a chunked string.
   */
  private static String chunkString(String payload, int chunkSize) {
    try {
      StringBuilder result = new StringBuilder();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(payload.getBytes("UTF-8"));
      int bytesRead;
      byte[] chunkedPayload = new byte[chunkSize];
      while ((bytesRead = inputStream.read(chunkedPayload, 0, chunkedPayload.length)) != -1) {
        String chunk = buildSignedChunk(bytesRead, chunkedPayload);
        result.append(chunk);
      }

      result.append(buildSignedChunk(0, chunkedPayload));
      return result.toString();
    } catch (Exception ignore) {
      return null;
    }
  }

  /**
   * Builds a signed chunk.
   */
  private static String buildSignedChunk(int userDataLen, byte[] userData) throws Exception {
    if (userDataLen == 0) {
      userData = FINAL_CHUNK;
    } else if (userDataLen < userData.length) {
      byte[] userDataNew = new byte[userDataLen];
      System.arraycopy(userData, 0, userDataNew, 0, userDataLen);
      userData = userDataNew;
    }

    String payload = new String(userData);
    String chunkSize = Integer.toHexString(userData.length);
    String chunkSignature = Hmacv4LoginModule.digestUTF8(payload);
    return chunkSize + CHUNK_SIGNATURE_HEADER + chunkSignature + CRLF + payload + CRLF;
  }

  /**
   * Builds a Lorem Ipsum string of the {@code length}.
   */
  private static String buildString(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0, j = 0; i < length; i++, j++) {
      if (j == LOREM_IPSUM.length())
        j = 0;
      sb.append(LOREM_IPSUM.charAt(j));
    }

    return sb.toString();
  }
}