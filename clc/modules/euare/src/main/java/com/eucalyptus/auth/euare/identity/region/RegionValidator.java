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
import java.util.regex.Pattern;
import org.springframework.validation.Errors;
import com.google.common.base.MoreObjects;

/**
 *
 */
class RegionValidator extends TypedValidator<Region> {

  public static final Pattern REGION_NAME_PATTERN = Pattern.compile( "^(?!-)[a-z0-9-]{1,63}(?<!-)" );
  private final Errors errors;

  RegionValidator( final Errors errors ) {
    this.errors = errors;
  }

  @Override
  public void validate( final Region region ) {
    require( NamedProperty.of( "getName", region::getName ) );
    require( NamedProperty.of( "getCertificateFingerprint", region::getCertificateFingerprint ) );
    require( NamedProperty.of( "getIdentifierPartitions", region::getIdentifierPartitions ) );
    require( NamedProperty.of( "getServices", region::getServices ) );
    validate( NamedProperty.of( "getName", region::getName ), new RegexValidator( errors, REGION_NAME_PATTERN, "Invalid region name ([a-z0-9-]*) \"{0}\": \"{1}\"" ) );
    validate( NamedProperty.of( "getCertificateFingerprint", region::getCertificateFingerprint ), new CertificateFingerprintValidator( errors ) );
    validate( NamedProperty.of( "getCertificateFingerprintDigest", region::getCertificateFingerprintDigest ), new CertificateFingerprintDigestValidator( errors ) );
    validate( NamedProperty.of( "getSslCertificateFingerprint", region::getSslCertificateFingerprint ), new CertificateFingerprintValidator( errors ) );
    validate( NamedProperty.of( "getSslCertificateFingerprintDigest", region::getCertificateFingerprintDigest ), new CertificateFingerprintDigestValidator( errors ) );
    validateAll( NamedProperty.of( "getIdentifierPartitions", region::getIdentifierPartitions ), new IdentifierPartitionValidator( errors ) );
    validateAll( NamedProperty.of( "getServices", region::getServices ), new ServiceValidator( errors ) );
    validateAll( NamedProperty.of( "getRemoteCidrs", region::getRemoteCidrs ), new CidrValidator( errors ) );
    validateAll( NamedProperty.of( "getForwardedForCidrs", region::getForwardedForCidrs ), new CidrValidator( errors ) );

    final List<Integer> partitions = ( region == null ? null : region.getIdentifierPartitions( ) );
    if ( ( partitions != null && partitions.isEmpty( ) ) ) {
      errors.reject( "property.invalid.identifier", new Object[]{ pathTranslate( errors.getNestedPath( ), "identifierPartitions" ) }, "No values given for \"{0}\"" );
    }

    final List<Service> services = ( region == null ? null : region.getServices( ) );
    if ( ( services != null && services.isEmpty( ) ) ) {
      errors.reject( "property.invalid.services", new Object[]{ pathTranslate( errors.getNestedPath( ), "services" ) }, "No values given for \"{0}\"" );
    }

  }

  public final Errors getErrors( ) {
    return errors;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .toString( );
  }
}
