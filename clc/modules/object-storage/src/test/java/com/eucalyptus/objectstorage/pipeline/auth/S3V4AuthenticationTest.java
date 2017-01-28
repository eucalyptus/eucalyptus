package com.eucalyptus.objectstorage.pipeline.auth;

import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException;
import com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication.V4AuthComponent;
import com.google.common.collect.Maps;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.nio.charset.Charset;

import java.util.HashMap;
import java.util.Map;

import static com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication.AWS_EXPIRES_PARAM;
import static org.junit.Assert.*;

/**
 * Tests {@link S3V4Authentication}.
 */
public class S3V4AuthenticationTest {
  static String V4_HEADER = "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request, " +
      "SignedHeaders=host;range;x-amz-date, Signature=fe5f80f77d5fa3beca038a248ff027d0445342fe2855ddc963176630326f1024";
  static String CANONICAL_HEADERS = "date:Fri, 24 May 2013 00:00:00 GMT\n" + "host:examplebucket.s3.amazonaws.com\n" +
      "x-amz-content-sha256:44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072\n" + "x-amz-date:20130524T000000Z\n" +
      "x-amz-storage-class:REDUCED_REDUNDANCY\n";
  static String CANONICAL_QUERY_STRING = "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F20130524%2Fus-east-1"
      + "%2Fs3%2Faws4_request&X-Amz-Date=20130524T000000Z&X-Amz-Expires=86400&X-Amz-SignedHeaders=host";
  static String CANONICAL_PRESIGNED_REQUEST = "PUT\n" + "/test.txt\n" +
      "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F20130524%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date" +
      "=20130524T000000Z&X-Amz-Expires=86400&X-Amz-SignedHeaders=host\n" + "host:examplebucket.s3.amazonaws.com\n" + "\n" + "host\n" +
      "UNSIGNED-PAYLOAD";
  static String CANONICAL_HEADERS_REQUEST = "PUT\n" + "/test$file.text\n" + "\n" + "date:Fri, 24 May 2013 00:00:00 GMT\n" +
      "host:examplebucket.s3.amazonaws.com\n" + "x-amz-content-sha256:44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072\n"
      + "x-amz-date:20130524T000000Z\n" + "x-amz-storage-class:REDUCED_REDUNDANCY\n\n" + "date;host;x-amz-content-sha256;x-amz-date;" +
      "x-amz-storage-class\n" + "44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072";
  static String SIGNED_HEADERS = "date;host;x-amz-content-sha256;x-amz-date;x-amz-storage-class";

  MappingHttpRequest headersRequest;
  MappingHttpRequest paramsRequest;

  @Before
  public void beforeMethod() {
    headersRequest = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "/test$file.text");
    headersRequest.addHeader("Host", "examplebucket.s3.amazonaws.com");
    headersRequest.addHeader("Date", "Fri, 24 May 2013 00:00:00 GMT");
    headersRequest.addHeader("x-amz-date", "20130524T000000Z");
    headersRequest.addHeader("x-amz-storage-class", "REDUCED_REDUNDANCY");
    headersRequest.addHeader("x-amz-content-sha256", "44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072");
    headersRequest.setContent(ChannelBuffers.copiedBuffer("Welcome to Amazon S3.", Charset.defaultCharset()));

    paramsRequest = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "/test.txt" +
        "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F20130524%2Fus-east-1%2Fs3%2Faws4_request" +
        "&X-Amz-Date=20130524T000000Z&X-Amz-Expires=86400&X-Amz-SignedHeaders=host&X-Amz-Signature=FOO");
    paramsRequest.addHeader("Host", "examplebucket.s3.amazonaws.com");
    paramsRequest.setContent(ChannelBuffers.copiedBuffer("Welcome to Amazon S3.", Charset.defaultCharset()));
  }

  @Test
  public void testBuildCanonicalResourcePath() throws Throwable {
    URL url = new URL("http://eucalyptus:1234/services/objectstorage/test-bucket/test-object?foo=bar");
    assertEquals("/services/objectstorage/test-bucket/test-object", S3V4Authentication.buildCanonicalResourcePath(url.getPath()));
  }

  @Test
  public void testBuildAndVerifyPayloadHash() throws Throwable {
    // Unsigned
    MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "/test$file.text");
    request.addHeader("x-amz-content-sha256", "UNSIGNED-PAYLOAD");
    String result = S3V4Authentication.getUnverifiedPayloadHash(request);
    assertEquals("UNSIGNED-PAYLOAD", result);

    // Signed
    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "/test$file.text");
    request.addHeader("x-amz-content-sha256", "44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072");
    request.setContent(ChannelBuffers.copiedBuffer("Welcome to Amazon S3.", Charset.defaultCharset()));
    result = S3V4Authentication.getUnverifiedPayloadHash(request);
    assertEquals("44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072", result);

    // Chunked
    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "/test$file.text");
    request.addHeader("x-amz-content-sha256", "STREAMING-AWS4-HMAC-SHA256-PAYLOAD");
    result = S3V4Authentication.getUnverifiedPayloadHash(request);
    assertEquals("STREAMING-AWS4-HMAC-SHA256-PAYLOAD", result);
  }

  @Test
  public void testBuildCanonicalHeaders() {
    StringBuilder sb = new StringBuilder();
    S3V4Authentication.buildCanonicalHeaders(headersRequest, SIGNED_HEADERS, sb);
    assertEquals(CANONICAL_HEADERS, sb.toString());
  }

  @Test
  public void testBuildCanonicalQueryString() {
    StringBuilder sb = new StringBuilder();
    S3V4Authentication.buildCanonicalQueryString(paramsRequest, sb);
    assertEquals(CANONICAL_QUERY_STRING, sb.toString());
  }

  @Test
  public void testBuildCanonicalRequestForHeaders() {
    StringBuilder canonicalRequest = S3V4Authentication.buildCanonicalRequest(headersRequest, SIGNED_HEADERS, headersRequest.getHeader
        ("x-amz-content-sha256"));
    assertEquals(CANONICAL_HEADERS_REQUEST, canonicalRequest.toString());
  }

  @Test
  public void testBuildCanonicalRequestForPresigned() {
    StringBuilder canonicalRequest = S3V4Authentication.buildCanonicalRequest(paramsRequest, "host", "UNSIGNED-PAYLOAD");
    assertEquals(CANONICAL_PRESIGNED_REQUEST, canonicalRequest.toString());
  }

  @Test
  public void testParseAuthHeader() throws Throwable {
    assertEquals(S3V4Authentication.getV4AuthComponents("AWS").size(), 0);

    Map<V4AuthComponent, String> auth = S3V4Authentication.getV4AuthComponents(V4_HEADER);
    assert auth.get(V4AuthComponent.Credential).equals("AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request");
    assert auth.get(V4AuthComponent.SignedHeaders).equals("host;range;x-amz-date");
    assert auth.get(V4AuthComponent.Signature).equals("fe5f80f77d5fa3beca038a248ff027d0445342fe2855ddc963176630326f1024");
  }

  @Test
  public void testParseDateAndAssertNotExpired() throws Throwable {
    DateTime dt = DateTime.now();
    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    S3Authentication.parseDate(fmt.print(dt));
  }

  @Test
  public void testValidateExpiresFromParams() throws Throwable {
    Map<String, String> params = new HashMap<>();
    params.put(AWS_EXPIRES_PARAM, "10");

    // Current date, future expires
    S3V4Authentication.validateExpiresFromParams(params, DateTime.now().toDate());

    // Future date
    try {
      S3V4Authentication.validateExpiresFromParams(params, DateTime.now().plusMinutes(20).toDate());
      fail("Date should have been not yet valid");
    } catch (AccessDeniedException expected) {
      assertTrue(expected.getMessage().contains("not yet valid"));
    }

    // Past date, not expired
    S3V4Authentication.validateExpiresFromParams(params, DateTime.now().minusSeconds(8).toDate());

    // Past date, expired
    try {
      S3V4Authentication.validateExpiresFromParams(params, DateTime.now().minusSeconds(40).toDate());
      fail("Date should have been expired");
    } catch (AccessDeniedException expected) {
      assertTrue(expected.getMessage().toLowerCase().contains("expired"));
    }
  }
}
