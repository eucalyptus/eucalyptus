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
