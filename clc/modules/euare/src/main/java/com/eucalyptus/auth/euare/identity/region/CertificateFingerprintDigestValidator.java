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

import java.util.Arrays;
import org.springframework.validation.Errors;
import com.google.common.base.MoreObjects;

/**
 *
 */
class CertificateFingerprintDigestValidator extends TypedValidator<String> {

  private final Errors errors;

  CertificateFingerprintDigestValidator( final Errors errors ) {
    this.errors = errors;
  }

  @Override
  public void validate( final String digest ) {
    if ( digest != null ) {
      if ( !Arrays.asList( "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512" ).contains( digest ) ) {
        errors.reject( "property.invalid.certificateFingerprintDigest", new Object[]{ digest, pathTranslate( errors.getNestedPath( ) ), "SHA-256, SHA-1" }, "Invalid certificate fingerprint digest: \"{0}\" for field: \"{1}\". Typical digests: \"{2}\"" );
      }
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
