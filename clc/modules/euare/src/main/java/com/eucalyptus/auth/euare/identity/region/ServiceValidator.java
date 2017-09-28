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

import java.util.regex.Pattern;
import org.springframework.validation.Errors;
import com.google.common.base.MoreObjects;

/**
 *
 */
class ServiceValidator extends TypedValidator<Service> {

  private static final Pattern SERVICE_TYPE_PATTERN = Pattern.compile( "[a-z][0-9a-z-]{0,62}" );
  private final Errors errors;

  ServiceValidator( final Errors errors ) {
    this.errors = errors;
  }

  @Override
  public void validate( final Service service ) {
    require( NamedProperty.of( "getType", service::getType ) );
    require( NamedProperty.of( "getEndpoints", service::getEndpoints ) );
    validate( NamedProperty.of( "getType", service::getType ), new RegexValidator( errors, SERVICE_TYPE_PATTERN, "Invalid service type \"{0}\": \"{1}\"" ) );
    validateAll( NamedProperty.of( "getEndpoints", service::getEndpoints ), new EndpointValidator( errors ) );
    if ( service.getEndpoints( ).isEmpty( ) ) {
      errors.reject( "property.invalid.endpoints", new Object[]{ pathTranslate( errors.getNestedPath( ), "endpoints" ) }, "No values given for \"{0}\"" );
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
