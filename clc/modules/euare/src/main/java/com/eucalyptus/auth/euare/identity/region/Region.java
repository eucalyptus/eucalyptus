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
