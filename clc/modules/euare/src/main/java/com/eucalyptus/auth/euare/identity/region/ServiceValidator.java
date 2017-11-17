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
