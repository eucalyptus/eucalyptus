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
package com.eucalyptus.empyrean.configuration;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.Filter;
import com.google.common.collect.Lists;

/**
 *
 */
@ComponentPart( Empyrean.class )
public interface ServiceRegistrationClient {

  // sync

  DescribeServicesResponseType describeServices( DescribeServicesType request );

  DescribeAvailableServiceTypesResponseType describeServiceTypes( DescribeAvailableServiceTypesType request );

  RegisterServiceResponseType registerService( RegisterServiceType request );

  DeregisterServiceResponseType deregisterService( DeregisterServiceType request );

  // convenience

  default DescribeServicesResponseType describeServices( ) {
    return describeServices( new DescribeServicesType( ) );
  }

  default DescribeServicesResponseType describePublicServices( ) {
    final DescribeServicesType describeServices = new DescribeServicesType( );
    final Filter filter = new Filter( );
    filter.setName( "public" );
    filter.setValues( Lists.newArrayList( "true" ) );
    describeServices.getFilters( ).add( filter );
    return describeServices( describeServices );
  }

  default DescribeAvailableServiceTypesResponseType describeServiceTypes( ) {
    return describeServiceTypes( new DescribeAvailableServiceTypesType( ) );
  }

  default RegisterServiceResponseType registerService(
      final String type,
      final String partition,
      final String host,
      final Integer port,
      final String name
  ) {
    final RegisterServiceType registerService = new RegisterServiceType( );
    registerService.setType( type );
    registerService.setPartition( partition );
    registerService.setHost( host );
    registerService.setPort( port );
    registerService.setName( name );
    return registerService( registerService );
  }
}
