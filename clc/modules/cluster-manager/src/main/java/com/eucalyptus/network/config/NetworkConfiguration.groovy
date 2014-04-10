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
import com.google.common.base.Strings
import com.google.common.net.InetAddresses
import com.google.common.net.InternetDomainName
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils
import org.springframework.validation.Validator

import java.lang.reflect.ParameterizedType
import java.util.regex.Pattern

@CompileStatic
@Canonical
class NetworkConfiguration {
  String instanceDnsDomain
  List<String> instanceDnsServers
  String macPrefix
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

  String toFieldName( String methodName ) {
   CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.substring( 3 ) )
  }

  String toPropertyName( String fieldName ) {
    CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName )
  }

  String pathTranslate( String path, String name = null ) {
    String pathPrefix  = Strings.isNullOrEmpty( path ) ? '' : "${path}."
    String fullPath = name ? "${pathPrefix}${name}" : path
    fullPath.split('\\.').collect{ String item -> toPropertyName(item) }.join('.')
  }

  void require( Closure<?> closure ) {
    MethodClosure methodClosure = (MethodClosure) closure
    String fieldName = toFieldName(methodClosure.method)
    ValidationUtils.rejectIfEmptyOrWhitespace( errors, fieldName, 'property.required', [pathTranslate(errors.nestedPath,fieldName)] as Object[], 'Missing required property \"{0}\"' );
  }

  void validate( Closure<?> closure, Validator validator ) {
    MethodClosure methodClosure = (MethodClosure) closure
    validate( closure.call(), toFieldName( methodClosure.method ), validator );
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
    String field = toFieldName(methodClosure.method)
    closure.call()?.eachWithIndex{ Object target, Integer index ->
      try {
        errors.pushNestedPath("${field}[${index}]");
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
  public static final Pattern MAC_PREFIX_PATTERN = Pattern.compile( '[0-9a-fA-F]{2}:[0-9a-fA-F]{2}' )
  Errors errors

  @Override
  void validate( final NetworkConfiguration configuration ) {
    require( configuration.&getPublicIps );
    validate( configuration.&getInstanceDnsDomain, new DomainValidator(errors) )
    validateAll( configuration.&getInstanceDnsServers, new IPValidator(errors) )
    validate( configuration.&getMacPrefix, new RegexValidator( errors, MAC_PREFIX_PATTERN, 'Invalid MAC prefix "{0}": "{1}"' ) )
    validateAll( configuration.&getPublicIps, new IPRangeValidator(errors) )
    validateAll( configuration.&getPrivateIps, new IPRangeValidator(errors) )
    validateAll( configuration.&getSubnets, new SubnetValidator(errors) )
    validateAll( configuration.&getClusters, new ClusterValidator(errors, configuration.subnets?.collect{Subnet subnet -> subnet.name?:subnet.subnet}?:[] as List<String>) )
  }
}

@CompileStatic
@Canonical
@PackageScope
class DomainValidator extends TypedValidator<String> {
  Errors errors

  @Override
  void validate( final String domain ) {
    if ( domain && !InternetDomainName.isValid( domain ) ) {
      errors.reject( "property.invalid.domain", [pathTranslate( errors.getNestedPath( ) ), domain ] as Object[], 'Invalid domain \"{0}\": \"{1}\"' )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class IPValidator extends TypedValidator<String> {
  Errors errors

  @Override
  void validate( final String ip ) {
    if ( ip && !InetAddresses.isInetAddress( ip ) ) {
      errors.reject( "property.invalid.ip", [pathTranslate( errors.getNestedPath( ) ), ip ] as Object[], 'Invalid IP \"{0}\": \"{1}\"' )
    }
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
      errors.reject( "property.invalid.range", [pathTranslate( errors.getNestedPath( ) ), range ] as Object[], 'Invalid IP or IP range for \"{0}\": \"{1}\"' )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class NetmaskValidator extends TypedValidator<String> {
  Errors errors

  @Override
  void validate( final String netmask ) {
    new IPValidator( errors ).validate( netmask )
    if ( netmask && !errors.hasErrors( ) ) {
      final BigInteger netmaskBigInteger = new BigInteger( InetAddresses.forString( netmask ).address )
      int i = 31;
      for ( ; i > -1 ; i-- ) {
        if ( !netmaskBigInteger.testBit( i ) ) break;
      }
      for ( ; i > -1 ; i-- ) {
        if ( netmaskBigInteger.testBit( i ) ) break;
      }
      if ( i!=-1 ) {
        errors.reject( "property.invalid.netmask", [pathTranslate( errors.getNestedPath( ) ), netmask] as Object[], 'Invalid netmask for \"{0}\": \"{1}\"' )
      }
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
    validate( subnet.&getSubnet, new IPValidator(errors) )
    validate( subnet.&getNetmask, new NetmaskValidator(errors) )
    validate( subnet.&getGateway, new IPValidator(errors) )

    if ( !errors.hasErrors( ) ) {
      int subnetInt = InetAddresses.coerceToInteger( InetAddresses.forString( subnet.subnet ) )
      int netmaskInt = InetAddresses.coerceToInteger( InetAddresses.forString( subnet.netmask ) )
      int gatewayInt = InetAddresses.coerceToInteger( InetAddresses.forString( subnet.gateway ) )
      if ( ( subnetInt & netmaskInt ) != subnetInt ) {
        errors.reject( "property.invalid.subnet", [pathTranslate( errors.getNestedPath( ) ), subnet.subnet] as Object[], 'Invalid subnet due to netmask for subnet \"{0}\": \"{1}\"' )
      }

      if ( ( gatewayInt == subnetInt ) || ( ( gatewayInt & netmaskInt ) != subnetInt ) ) {
        errors.reject( "property.invalid.subnet", [pathTranslate( errors.getNestedPath( ) ), subnet.gateway] as Object[], 'Invalid gateway due to subnet/netmask for subnet \"{0}\": \"{1}\"' )
      }
    }
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
    if ( subnet.name == null ||
        !subnetNames.contains( subnet.name ) ||
        subnet.subnet || // if any values are specified they must all be specified
        subnet.netmask ||
        subnet.gateway ) {
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
    validate( cluster.&getMacPrefix, new RegexValidator( errors, NetworkConfigurationValidator.MAC_PREFIX_PATTERN, 'Invalid MAC prefix "{0}": "{1}"' ) )
    if ( subnetNames.size() > 1 || cluster.subnet ) {
      require( cluster.&getSubnet )
      validate( cluster.&getSubnet, new ReferenceSubnetValidator( errors, subnetNames ) )
    }
    validateAll( cluster.&getPrivateIps, new IPRangeValidator( errors ) )
  }
}

@CompileStatic
@Canonical
@PackageScope
class RegexValidator extends TypedValidator<String> {
  Errors errors
  Pattern pattern
  String errorMessage

  @Override
  void validate( final String value ) {
    if ( value && !pattern.matcher( value ).matches(  ) ) {
      errors.reject( "property.invalid.regex", [pathTranslate( errors.getNestedPath( ) ), value ] as Object[], errorMessage )
    }
  }
}
