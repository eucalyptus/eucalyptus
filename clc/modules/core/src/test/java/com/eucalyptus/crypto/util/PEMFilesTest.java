package com.eucalyptus.crypto.util;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class PEMFilesTest {

  private static final String CERT_PEM =
          "-----BEGIN CERTIFICATE-----\n" +
          "MIID6jCCAtICCQDicjjWYnwY6jANBgkqhkiG9w0BAQ0FADCBtjELMAkGA1UEBhMC\n" +
          "VVMxCzAJBgNVBAgMAkNBMRYwFAYDVQQHDA1TYW50YSBCYXJiYXJhMSEwHwYDVQQK\n" +
          "DBhFdWNhbHlwdHVzIFN5c3RlbXMsIEluYy4xIDAeBgNVBAsMF0V1Y2FseXB0dXMg\n" +
          "VXNlciBDb25zb2xlMRowGAYDVQQDDBFuYzA0LmFwcHNjYWxlLm5ldDEhMB8GCSqG\n" +
          "SIb3DQEJARYSQG5jMDQuYXBwc2NhbGUubmV0MB4XDTIwMDEyMzAyMDk1MloXDTIx\n" +
          "MDEyMjAyMDk1MlowgbYxCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJDQTEWMBQGA1UE\n" +
          "BwwNU2FudGEgQmFyYmFyYTEhMB8GA1UECgwYRXVjYWx5cHR1cyBTeXN0ZW1zLCBJ\n" +
          "bmMuMSAwHgYDVQQLDBdFdWNhbHlwdHVzIFVzZXIgQ29uc29sZTEaMBgGA1UEAwwR\n" +
          "bmMwNC5hcHBzY2FsZS5uZXQxITAfBgkqhkiG9w0BCQEWEkBuYzA0LmFwcHNjYWxl\n" +
          "Lm5ldDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKewrozUIFu5XsJ3\n" +
          "lk0Y3rjju05zx6HMXabn9kyhexKCrgT/lqw+ueA80s0crpBHTvKH3MdVAF6rPj+J\n" +
          "N7+Dang3LGNzThWmooPCFIxFopr+HAvbsHJb1SKXbzaYZ75AjfCpq9hnhT+MS53e\n" +
          "7xzLa3gMNNJgE1FM4zzfL3gxK2ZDVgJsQOxVrpLFcGAtOniLvc47KwMNuiLtLlTD\n" +
          "qzWk7ROKh4on2KS/3Zw8XRT+p1xy7H5UDZ69JUTRR9Lllyo/M9Oj+7MrAXqd7L2L\n" +
          "Tp3Fju3SeLe2eSL43AYreEh0DsKg+VXx7TdsVODjAQWQ0FYkUayogcoQMCVYkqMC\n" +
          "lwL2EvMCAwEAATANBgkqhkiG9w0BAQ0FAAOCAQEApkoAeeolIBgTNjSCqFWqDIn2\n" +
          "oRV+C6YOR/e+YVKTZdraWQ4Bv0QjzsfhCX1h+EqGhxqV+lssphWm7FdF1Us4yOVT\n" +
          "V2vGB5ripd0YN87Ef0BNKohkdVsTWvyY/LTMCvjJFpDc5KxYT4GH1ZzviAmGiud3\n" +
          "Br8aSZGl0AJU2d5VZ0fN1SW9V21fo0X6qJqhQhTIt98cZ5uvc49b2ZtyIZbbxodi\n" +
          "OAsRFnOrYq2/s1Q20WulIKSpy+nMW2zkLz7RIVneyJFONZNdpBZOgoBIx6o42qnk\n" +
          "v0J04FqCEUJzedMbSMFqvPkZttCs+JaQQg0QMxeoV/IVJtsqSA/Kc73CIZZC0A==\n" +
          "-----END CERTIFICATE-----\n";

  private static final String CERT_CHAIN1_PEM = CERT_PEM;

  private static final String CERT_CHAIN2_PEM = CERT_PEM + CERT_PEM;

  private static final String CERT_CHAIN3_PEM = CERT_PEM + CERT_PEM + CERT_PEM;

  @BeforeClass
  public static void beforeClass() {
    if ( Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null ) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  @Test
  public void testGetCertificate( ) {
    Assert.assertNotNull(
        "Certificate",
        PEMFiles.getCert( CERT_PEM.getBytes( StandardCharsets.UTF_8 ) ) );
  }

  @Test
  public void testGetCertificateChain1( ) {
    Assert.assertEquals(
        "Certificate chain length",
        1,
        PEMFiles.getCertChain( CERT_CHAIN1_PEM.getBytes( StandardCharsets.UTF_8 ) ).size( ) );
  }

  @Test
  public void testGetCertificateChain2( ) {
    Assert.assertEquals(
        "Certificate chain length",
        2,
        PEMFiles.getCertChain( CERT_CHAIN2_PEM.getBytes( StandardCharsets.UTF_8 ) ).size( ) );
  }

  @Test
  public void testGetCertificateChain3( ) {
    Assert.assertEquals(
        "Certificate chain length",
        3,
        PEMFiles.getCertChain( CERT_CHAIN3_PEM.getBytes( StandardCharsets.UTF_8 ) ).size( ) );
  }
}
