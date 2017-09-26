/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
