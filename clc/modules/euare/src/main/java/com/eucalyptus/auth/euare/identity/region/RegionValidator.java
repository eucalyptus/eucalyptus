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
