package com.eucalyptus.objectstorage.pipeline.auth;

import com.eucalyptus.objectstorage.exceptions.s3.RequestTimeTooSkewedException;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests {@link S3V4Authentication}.
 */
public class S3AuthenticationTest {
  @Test(expected = RequestTimeTooSkewedException.class)
  public void testAssertDateNotSkewedForPastDate() throws Throwable {
    S3Authentication.assertDateNotSkewed(DateTime.now().minusMinutes(17).toDate());
    fail("Date should have been expired");
  }

  @Test(expected = RequestTimeTooSkewedException.class)
  public void testAssertDateNotSkewedForFutureDate() throws Throwable {
    S3Authentication.assertDateNotSkewed(DateTime.now().plusMinutes(17).toDate());
    fail("Date should have been expired");
  }
}
