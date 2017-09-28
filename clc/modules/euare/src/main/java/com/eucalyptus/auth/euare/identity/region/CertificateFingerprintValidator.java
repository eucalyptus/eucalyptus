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

import org.springframework.validation.Errors;
import com.google.common.base.MoreObjects;

/**
 *
 */
class CertificateFingerprintValidator extends TypedValidator<String> {

  private final Errors errors;

  CertificateFingerprintValidator( final Errors errors ) {
    this.errors = errors;
  }

  @Override
  public void validate( final String fingerprint ) {
    if ( fingerprint != null ) {
      if ( !fingerprint.matches( "[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){9,255}" ) ) {
        errors.reject( "property.invalid.certificateFingerprint", new Object[]{ pathTranslate( errors.getNestedPath( ) ) }, "Invalid certificate fingerprint (e.g. EC:E7:...): \"{0}\"" );
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
