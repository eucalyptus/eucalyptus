/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.network.config

import com.eucalyptus.network.IPRange
import com.google.common.base.CaseFormat
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils
import org.springframework.validation.Validator

import java.lang.reflect.ParameterizedType

@CompileStatic
@Canonical
class NetworkConfiguration {
  String instanceDnsDomain
  List<String> instanceDnsServers
  List<String> publicIps  // List of ip address ranges
  List<String> privateIps // List of ip address ranges
  List<Subnet> subnets
  List<Cluster> clusters
}

@CompileStatic
@Canonical
class Subnet {
  String name
  String subnet
  String netmask
  String gateway
}

@CompileStatic
@Canonical
class Cluster {
  String name
  String macPrefix
  Subnet subnet
  List<String> privateIps // List of ip address ranges
}

@CompileStatic
@Canonical
@PackageScope
abstract class TypedValidator<T> implements Validator {

  @Override
  boolean supports( final Class<?> aClass ) {
    aClass == getTargetClass( )
  }

  @Override
  void validate( final Object o, final Errors errors ) {
    validate( (T) o )
  }

  abstract Errors getErrors()

  Class<?> getTargetClass( ) {
    (Class<?>)((ParameterizedType)getClass().getGenericSuperclass()).actualTypeArguments[0]
  }

  void validate( T target ) {
  }

  String toPropertyName( String methodName ) {
   CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.substring( 3 ) )
  }

  void require( Closure<?> closure ) {
    MethodClosure methodClosure = (MethodClosure) closure
    ValidationUtils.rejectIfEmptyOrWhitespace( errors, toPropertyName(methodClosure.method), "field.required" );
  }

  void validate( Closure<?> closure, Validator validator ) {
    MethodClosure methodClosure = (MethodClosure) closure
    validate( closure.call(), toPropertyName( methodClosure.method ), validator );
  }

  void validate( Object target, String path, Validator validator ) {
    try {
      errors.pushNestedPath( path );
      ValidationUtils.invokeValidator( validator, target, errors );
    } finally {
      errors.popNestedPath();
    }
  }

  void validateAll( Closure<List<?>> closure, Validator validator ) {
    MethodClosure methodClosure = (MethodClosure) closure
    String property = toPropertyName(methodClosure.method)
    closure.call().eachWithIndex{ Object target, Integer index ->
      try {
        errors.pushNestedPath("${property}[${index}]");
        ValidationUtils.invokeValidator( validator, target, errors );
      } finally {
        errors.popNestedPath();
      }
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class NetworkConfigurationValidator extends TypedValidator<NetworkConfiguration> {
  Errors errors

  @Override
  void validate( final NetworkConfiguration configuration ) {
    require( configuration.&getPublicIps );
    require( configuration.&getSubnets );
    require( configuration.&getClusters );
    validateAll( configuration.&getPublicIps, new IPRangeValidator(errors) )
    validateAll( configuration.&getPrivateIps, new IPRangeValidator(errors) )
    validateAll( configuration.&getSubnets, new SubnetValidator(errors) )
    validateAll( configuration.&getClusters, new ClusterValidator(errors, configuration.subnets.collect{Subnet subnet -> subnet.name?:subnet.subnet}) )
  }
}

@CompileStatic
@Canonical
@PackageScope
class IPRangeValidator extends TypedValidator<String> {
  Errors errors

  @Override
  void validate( final String range ) {
    if ( !IPRange.isIPRange( range ) ) {
      errors.reject( "field.invalid" )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class SubnetValidator extends TypedValidator<Subnet> {
  Errors errors

  @Override
  void validate( final Subnet subnet ) {
    require( subnet.&getSubnet )
    require( subnet.&getNetmask )
    require( subnet.&getGateway )
  }
}

@CompileStatic
@Canonical
@PackageScope
class ReferenceSubnetValidator extends TypedValidator<Subnet> {
  Errors errors
  List<String> subnetNames

  @Override
  void validate( final Subnet subnet) {
    if ( subnet.name == null || !subnetNames.contains( subnet.name ) ) {
      new SubnetValidator( errors ).validate( subnet )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class ClusterValidator extends TypedValidator<Cluster> {
  Errors errors
  List<String> subnetNames

  @Override
  void validate( final Cluster cluster ) {
    require( cluster.&getName )
    if ( subnetNames.size() > 1 || cluster.subnet ) {
      require( cluster.&getSubnet )
      validate( cluster.&getSubnet, new ReferenceSubnetValidator( errors, subnetNames ) )
    }
    validateAll( cluster.&getPrivateIps, new IPRangeValidator( errors ) )
  }
}
