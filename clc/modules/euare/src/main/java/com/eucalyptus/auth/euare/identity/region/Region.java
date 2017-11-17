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
package com.eucalyptus.auth.euare.identity.region;

import java.util.List;
import java.util.Objects;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class Region {

  private String name;
  private String certificateFingerprintDigest;
  private String certificateFingerprint;
  private String sslCertificateFingerprintDigest;
  private String sslCertificateFingerprint;
  private List<Integer> identifierPartitions;
  private List<Service> services;
  private List<String> remoteCidrs;
  private List<String> forwardedForCidrs;

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getCertificateFingerprintDigest( ) {
    return certificateFingerprintDigest;
  }

  public void setCertificateFingerprintDigest( String certificateFingerprintDigest ) {
    this.certificateFingerprintDigest = certificateFingerprintDigest;
  }

  public String getCertificateFingerprint( ) {
    return certificateFingerprint;
  }

  public void setCertificateFingerprint( String certificateFingerprint ) {
    this.certificateFingerprint = certificateFingerprint;
  }

  public String getSslCertificateFingerprintDigest( ) {
    return sslCertificateFingerprintDigest;
  }

  public void setSslCertificateFingerprintDigest( String sslCertificateFingerprintDigest ) {
    this.sslCertificateFingerprintDigest = sslCertificateFingerprintDigest;
  }

  public String getSslCertificateFingerprint( ) {
    return sslCertificateFingerprint;
  }

  public void setSslCertificateFingerprint( String sslCertificateFingerprint ) {
    this.sslCertificateFingerprint = sslCertificateFingerprint;
  }

  public List<Integer> getIdentifierPartitions( ) {
    return identifierPartitions;
  }

  public void setIdentifierPartitions( List<Integer> identifierPartitions ) {
    this.identifierPartitions = identifierPartitions;
  }

  public List<Service> getServices( ) {
    return services;
  }

  public void setServices( List<Service> services ) {
    this.services = services;
  }

  public List<String> getRemoteCidrs( ) {
    return remoteCidrs;
  }

  public void setRemoteCidrs( List<String> remoteCidrs ) {
    this.remoteCidrs = remoteCidrs;
  }

  public List<String> getForwardedForCidrs( ) {
    return forwardedForCidrs;
  }

  public void setForwardedForCidrs( List<String> forwardedForCidrs ) {
    this.forwardedForCidrs = forwardedForCidrs;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final Region region = (Region) o;
    return Objects.equals( getName( ), region.getName( ) ) &&
        Objects.equals( getCertificateFingerprintDigest( ), region.getCertificateFingerprintDigest( ) ) &&
        Objects.equals( getCertificateFingerprint( ), region.getCertificateFingerprint( ) ) &&
        Objects.equals( getSslCertificateFingerprintDigest( ), region.getSslCertificateFingerprintDigest( ) ) &&
        Objects.equals( getSslCertificateFingerprint( ), region.getSslCertificateFingerprint( ) ) &&
        Objects.equals( getIdentifierPartitions( ), region.getIdentifierPartitions( ) ) &&
        Objects.equals( getServices( ), region.getServices( ) ) &&
        Objects.equals( getRemoteCidrs( ), region.getRemoteCidrs( ) ) &&
        Objects.equals( getForwardedForCidrs( ), region.getForwardedForCidrs( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getName( ), getCertificateFingerprintDigest( ), getCertificateFingerprint( ),
        getSslCertificateFingerprintDigest( ), getSslCertificateFingerprint( ), getIdentifierPartitions( ),
        getServices( ), getRemoteCidrs( ), getForwardedForCidrs( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "name", name )
        .add( "certificateFingerprintDigest", certificateFingerprintDigest )
        .add( "certificateFingerprint", certificateFingerprint )
        .add( "sslCertificateFingerprintDigest", sslCertificateFingerprintDigest )
        .add( "sslCertificateFingerprint", sslCertificateFingerprint )
        .add( "identifierPartitions", identifierPartitions )
        .add( "services", services )
        .add( "remoteCidrs", remoteCidrs )
        .add( "forwardedForCidrs", forwardedForCidrs )
        .toString( );
  }
}
