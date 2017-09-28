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

import java.net.URI;
import org.springframework.validation.Errors;
import com.google.common.base.MoreObjects;

/**
 *
 */
class EndpointValidator extends TypedValidator<String> {

  private final Errors errors;

  EndpointValidator( final Errors errors ) {
    this.errors = errors;
  }

  @Override
  public void validate( final String endpoint ) {
    try {
      URI endpointUri = new URI( endpoint );
      if ( !endpointUri.isAbsolute( ) || ( !endpointUri.getScheme( ).equalsIgnoreCase( "https" ) && !endpointUri.getScheme( ).equalsIgnoreCase( "http" ) ) ) {
        errors.reject( "property.invalid.endpoint", new Object[]{ pathTranslate( errors.getNestedPath( ) ), endpoint }, "Invalid service endpoint (e.g. https://...) \"{0}\": \"{1}\"" );
      }

    } catch ( Exception e ) {
      errors.reject( "property.invalid.endpoint", new Object[]{ pathTranslate( errors.getNestedPath( ) ), endpoint }, "Invalid service endpoint (e.g. https://...) \"{0}\": \"{1}\"" );
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
