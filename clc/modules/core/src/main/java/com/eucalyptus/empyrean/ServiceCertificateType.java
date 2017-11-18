/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.empyrean;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ServiceCertificateType extends EucalyptusData {

  private String serviceType;
  private String certificateUsage;
  private String certificateFormat;
  private String certificate;
  private String certificateFingerprint;
  private String certificateFingerprintDigest;

  public String getServiceType( ) {
    return serviceType;
  }

  public void setServiceType( String serviceType ) {
    this.serviceType = serviceType;
  }

  public String getCertificateUsage( ) {
    return certificateUsage;
  }

  public void setCertificateUsage( String certificateUsage ) {
    this.certificateUsage = certificateUsage;
  }

  public String getCertificateFormat( ) {
    return certificateFormat;
  }

  public void setCertificateFormat( String certificateFormat ) {
    this.certificateFormat = certificateFormat;
  }

  public String getCertificate( ) {
    return certificate;
  }

  public void setCertificate( String certificate ) {
    this.certificate = certificate;
  }

  public String getCertificateFingerprint( ) {
    return certificateFingerprint;
  }

  public void setCertificateFingerprint( String certificateFingerprint ) {
    this.certificateFingerprint = certificateFingerprint;
  }

  public String getCertificateFingerprintDigest( ) {
    return certificateFingerprintDigest;
  }

  public void setCertificateFingerprintDigest( String certificateFingerprintDigest ) {
    this.certificateFingerprintDigest = certificateFingerprintDigest;
  }
}
