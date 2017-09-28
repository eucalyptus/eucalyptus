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
class RegexValidator extends TypedValidator<String> {

  private final Errors errors;
  private final Pattern pattern;
  private final String errorMessage;

  RegexValidator( final Errors errors, final Pattern pattern, final String errorMessage ) {
    this.errors = errors;
    this.pattern = pattern;
    this.errorMessage = errorMessage;
  }

  @Override
  public void validate( final String value ) {
    if ( value != null && !pattern.matcher( value ).matches( ) ) {
      errors.reject( "property.invalid.regex", new Object[]{ pathTranslate( errors.getNestedPath( ) ), value }, errorMessage );
    }

  }

  public final Errors getErrors( ) {
    return errors;
  }

  public final Pattern getPattern( ) {
    return pattern;
  }

  public final String getErrorMessage( ) {
    return errorMessage;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "pattern", pattern )
        .add( "errorMessage", errorMessage )
        .toString( );
  }
}
