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
class RegionConfigurationValidator extends TypedValidator<RegionConfiguration> {

  private final Errors errors;

  RegionConfigurationValidator( final Errors errors ) {
    this.errors = errors;
  }

  @Override
  public void validate( final RegionConfiguration configuration ) {
    validateAll( NamedProperty.of( "getRegions", configuration::getRegions ), new RegionValidator( errors ) );
    validateAll( NamedProperty.of( "getRemoteCidrs", configuration::getRemoteCidrs ), new CidrValidator( errors ) );
    validateAll( NamedProperty.of( "getForwardedForCidrs", configuration::getForwardedForCidrs ), new CidrValidator( errors ) );
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
