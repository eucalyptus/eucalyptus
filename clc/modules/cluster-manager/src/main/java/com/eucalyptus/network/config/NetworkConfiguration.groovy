/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import com.google.common.primitives.Longs
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import java.lang.reflect.ParameterizedType
import java.util.regex.Pattern

import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils
import org.springframework.validation.Validator

import com.eucalyptus.network.IPRange
import com.eucalyptus.network.ManagedSubnets
import com.eucalyptus.network.NetworkMode
import com.eucalyptus.network.PrivateAddresses
import com.eucalyptus.util.Cidr
import com.google.common.base.CaseFormat
import com.google.common.base.Function
import com.google.common.base.Functions
import com.google.common.base.Joiner
import com.google.common.base.Strings
import com.google.common.collect.Iterables
import com.google.common.net.InetAddresses
import com.google.common.net.InternetDomainName

/**
 * Class representing the global network configuration. The networking configuration include
 * the instance DNS domain and server information, the global MAC prefix to use (unless a
 * Cluster redefines it), the list of public IPs available to use, a list of private IPs available
 * to all clusters, a list of subnets available to all clusters
 */
@CompileStatic
@Canonical
class NetworkConfiguration {
  String mode
  String instanceDnsDomain
  List<String> instanceDnsServers
  String macPrefix
  Midonet mido
  String publicGateway
  List<String> publicIps  // List of ip address ranges
  List<String> privateIps // List of ip address ranges
  List<EdgeSubnet> subnets
  ManagedSubnet managedSubnet
  List<Cluster> clusters
}

@CompileStatic
@Canonical
class Midonet {
  String eucanetdHost
  // old config format, top level,single gateway specification
  String gatewayHost
  String gatewayIP
  String gatewayInterface
  // gateway list
  List<MidonetGateway> gateways
  // old config format
  String publicNetworkCidr
  String publicGatewayIP
  // new format
  String bgpAsn
}

@CompileStatic
@Canonical
class MidonetGateway {
  // old config format
  String gatewayIP
  String gatewayHost
  String gatewayInterface
  // new format
  String ip
  String externalCidr
  String externalDevice
  String externalIp
  String externalRouterIp
  String bgpPeerIp
  String bgpPeerAsn
  List<String> bgpAdRoutes
}

/**
 * Class representation of a private network subnet. At the minimum, a subnet instance
 * contains a representative name, a subnet ID and a netmask.
 *
 * Each subnet must have a unique name to identify them in the JSON configuration string. When
 * passed to the back-end in the Global Network Information XML format, the name will match
 * the subnet ID as this is more important to have a short unique name than a descriptive one.
 */
@CompileStatic
@Canonical
class Subnet {
  String name
  String subnet
  String netmask
}

/**
 * Class representation of an EDGE private network subnet. An EDGE subnet differentiate itself
 * by having a specific network gateway.
 */
@CompileStatic
@Canonical
class EdgeSubnet extends Subnet {
  String gateway
}

/**
 * Class representation of a MANAGED private network subnet. An MANAGED subnet differentiate itself
 * by having a specific network segment size. A subnet has a defined set of IPs and this segment size
 * will divide the main subnet into X smaller subnets of "segmentSize" IP addresses where X = number of
 * IPs in subnet / segment size. The segmentSize value MUST be a valid power of 2 number greater or
 * equal to 16. There can only be up to 4095 resulting network segment. The segment list is ordered from
 * the smallest IPs to the highest IP and referred by an index from [1..4095] inclusively. This index also
 * refers to a matching network vlan which can further bound the segment list.
 */
@CompileStatic
@Canonical
class ManagedSubnet extends Subnet {
  // Minimum and Maximum values for validation and comparision
  static final int MIN_VLAN = 2
  static final int MAX_VLAN = 4095
  static final int MIN_INDEX = 9
  static final int MIN_SEGMENT_SIZE = 16
  static final int DEF_SEGMENT_SIZE = 32
  static final int MAX_SEGMENT_SIZE = 2048

  // The fields we need configured
  Integer minVlan
  Integer maxVlan
  Integer segmentSize  // This must be a power of 2 (e.g. 16, 32, 64, 128, ..., 2048)
}

/**
 * Class representation of a cluster. At the minimum, a cluster contain a name, macPrefix and a subnet. A
 * subnet can be a reference to a globally declared subnet or it can be a local subnet declaration for this
 * cluster only. In both MANAGED modes, the subnet declaration isn't used. Instead a global ManagedSubnet
 * declaration is used. Based on the networking mode, the list of private IPs could be empty (both MANAGED modes)
 * or contain private IP addresses that are valid and within the subnet range (EDGE networking mode).
 */
@CompileStatic
@Canonical
class Cluster {
  String name
  String macPrefix
  EdgeSubnet subnet
  List<String> privateIps
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
    String pathPrefix  = Strings.isNullOrEmpty( path ) ? '' : "${path}${path.endsWith('.')?'':'.'}"
    String fullPath = name ? "${pathPrefix}${name}" : path
    fullPath.split('\\.').collect{ String item -> toPropertyName(item) }.join('.')
  }

  void require( Closure<?> closure ) {
    MethodClosure methodClosure = (MethodClosure) closure
    String fieldName = toFieldName(methodClosure.method)
    ValidationUtils.rejectIfEmptyOrWhitespace( errors, fieldName, 'property.required', [pathTranslate(errors.nestedPath,fieldName)] as Object[], 'Missing required property \"{0}\"' );
  }

  void forbid( Closure<?> closure ) {
    MethodClosure methodClosure = (MethodClosure) closure
    String fieldName = toFieldName(methodClosure.method)
    Object value = errors.getFieldValue( fieldName );
    if( value != null ) {
      errors.rejectValue( fieldName, 'property.invalid', [pathTranslate(errors.nestedPath,fieldName)] as Object[], 'Invalid use of property \"{0}\"');
    }
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
  public static final Pattern MODE_PATTERN = Pattern.compile(
    Joiner.on('|').join( Iterables.transform( Arrays.<NetworkMode>asList( NetworkMode.values( ) ), (Function<NetworkMode, String>)Functions.toStringFunction( ) ) )
  )

  Errors errors

  @Override
  void validate( final NetworkConfiguration configuration ) {
    require( configuration.&getPublicIps )
    validate( configuration.&getMode, new RegexValidator( errors, MODE_PATTERN, 'Invalid mode "{0}": "{1}"' ) )
    validate( configuration.&getInstanceDnsDomain, new DomainValidator(errors) )
    validateAll( configuration.&getInstanceDnsServers, new IPValidator(errors) )
    validate( configuration.&getMacPrefix, new RegexValidator( errors, MAC_PREFIX_PATTERN, 'Invalid MAC prefix "{0}": "{1}"' ) )
    validateAll( configuration.&getPublicIps, new IPRangeValidator( errors ) )
    validateAll( configuration.&getPrivateIps, new IPRangeValidator( errors ) )
    if ( configuration.mode == 'VPCMIDO' ) {
      require( configuration.&getMido )
      if ( configuration.mido )
          validate( configuration.&getMido, new MidonetValidator( errors ) )
    } else if ( 'EDGE'.equals( configuration.mode ) || !configuration.mode ) {
      // In EDGE modes, we need the subnets information which is optional globally. If a managed subnet
      // is provided, then we have an error
      validateAll( configuration.&getSubnets, new EdgeSubnetValidator( errors ) )
      if ( configuration.managedSubnet ) {
        errors.reject( "property.invalid.subnet", [ pathTranslate( errors.getNestedPath( ) ), configuration.mode ] as Object[ ], 'Unexpected ManagedSubnet declaration in EDGE mode "{0}": "{1}"' )
      }
    } else {
      // In MANAGED modes, we need the managed subnet information. If subnets are provided, then we have an error
      require( configuration.&getManagedSubnet )
      validate( configuration.&getManagedSubnet, new ManagedSubnetValidator( errors ) )
      if ( configuration.getSubnets( ) ) {
        errors.reject( "property.invalid.subnet", [ pathTranslate( errors.getNestedPath( ) ), configuration.mode ] as Object[ ], 'Unexpected Subnets declaration for non-EDGE mode "{0}": "{1}"' )
      }
    }
    validateAll( configuration.&getClusters, new ClusterValidator(errors, configuration.subnets?.collect{EdgeSubnet subnet -> subnet.name?:subnet.subnet}?:[ ] as List<String>) )
  }
}

@CompileStatic
@Canonical
@PackageScope
class MidonetValidator extends TypedValidator<Midonet> {
  Errors errors

  private Closure<Boolean> gatewayPredicate( ) {
    { MidonetGateway midonetGateway ->
          midonetGateway.ip ||
          midonetGateway.externalDevice ||
          midonetGateway.externalCidr ||
          midonetGateway.externalIp ||
          midonetGateway.externalRouterIp ||
          midonetGateway.bgpPeerIp ||
          midonetGateway.bgpPeerAsn ||
          midonetGateway.bgpAdRoutes != null
    }
  }

  @Override
  void validate( final Midonet midonet ) {
    boolean regularValidation = midonet?.gateways?.find( gatewayPredicate( ) ) ?: false
    if ( !regularValidation && !midonet?.gateways && !midonet.publicNetworkCidr && !midonet.publicGatewayIP ) {
      regularValidation = true;
    }
    if ( regularValidation || !midonet.gatewayHost || !midonet.gatewayIP || !midonet.gatewayInterface ) {
      require( midonet.&getGateways )
      validateAll( midonet.&getGateways, regularValidation ?
          new MidonetGatewayValidator( errors, midonet.bgpAsn != null ) :
          new MidonetGatewayLegacyValidator( errors ) )
      if ( midonet.gateways != null && midonet.gateways.empty ) {
        errors.reject( "property.invalid.gateways", [ pathTranslate( errors.getNestedPath( ), "Gateways" ) ] as Object[ ], 'At least one gateway is required "{0}"' )
      } else if ( midonet.gateways != null && midonet.gateways.size( ) > 6 ) {
        errors.reject( "property.invalid.gateways", [ pathTranslate( errors.getNestedPath( ), "Gateways" ), midonet.gateways.size( ) ] as Object[ ], 'Maximum allowed gateways (6) exceeded "{0}": {1}' )
      }
    }
    if ( !regularValidation ) {
      require( midonet.&getPublicNetworkCidr )
      require( midonet.&getPublicGatewayIP )
    } else {
      forbid( midonet.&getPublicNetworkCidr )
      forbid( midonet.&getPublicGatewayIP )
    }
    validate( midonet.&getEucanetdHost, new HostValidator(errors) )
    validate( midonet.&getGatewayHost, new HostValidator(errors) )
    validate( midonet.&getGatewayIP, new IPValidator(errors) )
    validate( midonet.&getPublicNetworkCidr, new CidrValidator(errors) )
    validate( midonet.&getPublicGatewayIP, new IPValidator(errors) )
    validate( midonet.&getBgpAsn, new InclusiveRangeValidator( errors, 1L, 4294967295L, 'Invalid ASN "{0}": "{1}"' ) )

  }
}

@CompileStatic
@Canonical
@PackageScope
class MidonetGatewayLegacyValidator extends TypedValidator<MidonetGateway> {
  Errors errors

  @Override
  void validate( final MidonetGateway midonetGateway ) {
    forbid( midonetGateway.&getIp )
    forbid( midonetGateway.&getExternalCidr )
    forbid( midonetGateway.&getExternalDevice )
    forbid( midonetGateway.&getExternalIp )
    forbid( midonetGateway.&getExternalRouterIp )
    forbid( midonetGateway.&getBgpPeerIp )
    forbid( midonetGateway.&getBgpPeerAsn )
    forbid( midonetGateway.&getBgpAdRoutes )

    require( midonetGateway.&getGatewayHost )
    require( midonetGateway.&getGatewayIP )
    require( midonetGateway.&getGatewayInterface )

    validate( midonetGateway.&getGatewayHost, new HostValidator(errors) )
    validate( midonetGateway.&getGatewayIP, new IPValidator(errors) )
  }
}

@CompileStatic
@Canonical
@PackageScope
class MidonetGatewayValidator extends TypedValidator<MidonetGateway> {
  Errors errors
  Boolean requireBgp

  @Override
  void validate( final MidonetGateway midonetGateway ) {
    forbid( midonetGateway.&getGatewayHost )
    forbid( midonetGateway.&getGatewayIP )
    forbid( midonetGateway.&getGatewayInterface )

    require( midonetGateway.&getIp )
    require( midonetGateway.&getExternalCidr )
    require( midonetGateway.&getExternalDevice )
    require( midonetGateway.&getExternalIp )
    if ( requireBgp ) {
      require( midonetGateway.&getBgpPeerIp )
      require( midonetGateway.&getBgpPeerAsn )
      require( midonetGateway.&getBgpAdRoutes )
      if ( midonetGateway.bgpAdRoutes.empty ) {
        errors.reject("property.invalid.bgproutes", [pathTranslate(errors.getNestedPath()),"BgpAdRoutes"] as Object[], 'At least one route is required "{0}"')
      }
    } else {
      require( midonetGateway.&getExternalRouterIp )
    }
    validate( midonetGateway.&getIp, new IPValidator(errors) )
    validate( midonetGateway.&getExternalCidr, new CidrValidator(errors) )
    validate( midonetGateway.&getExternalIp, new IPValidator(errors) )
    validate( midonetGateway.&getExternalRouterIp, new IPValidator(errors) )
    validate( midonetGateway.&getBgpPeerIp, new IPValidator(errors) )
    validate( midonetGateway.&getBgpPeerAsn, new InclusiveRangeValidator( errors, 1L, 4294967295L, 'Invalid ASN "{0}": "{1}"' ) )
    validateAll( midonetGateway.&getBgpAdRoutes, new CidrValidator(errors) )

    if ( midonetGateway.externalCidr ) {
      IPRange range = IPRange.fromCidr( Cidr.parse( midonetGateway.externalCidr ) )
      [
          [ midonetGateway.externalIp, 'ExternalIp' ],
          [ midonetGateway.externalRouterIp, 'ExternalRouterIp' ],
          [ midonetGateway.bgpPeerIp, 'BgpPeerIp' ]
      ].each { String value, String property ->
        if ( value && !range.contains( IPRange.parse( value ) ) ) {
          errors.reject("property.invalid.forexternalcidr", [pathTranslate(errors.getNestedPath(),property),property] as Object[], '{1} must be within ExternalCidr "{0}"')
        }
      }
    }
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
class HostValidator extends TypedValidator<String> {
  private static final boolean skipHostValidation = Boolean.valueOf( System.getProperty( 'com.eucalyptus.network.config.skipHostValidation', 'false' ) )
  Errors errors

  @Override
  void validate( final String host ) {
    if ( host && !skipHostValidation ) {
      boolean valid
      try {
        InetAddresses.forString( host );
        valid = true;
      } catch ( IllegalArgumentException ) {
        valid = InternetDomainName.isValid( host )
      }
      if ( !valid ) {
        errors.reject( "property.invalid.host", [pathTranslate( errors.getNestedPath( ) ), host ] as Object[], 'Invalid host \"{0}\": \"{1}\"' )
      }
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

/**
 * EdgeSubnet validator class. An EDGE style subnet is considered valid if ALL of the following
 * conditions are met:
 *    - It has a subnet AND a netmask AND a gateway provided
 *    - The provided subnet ID is a valid IP address format
 *    - The provided netmask is a valid netmask format
 *    - The provided gateway is a valid IP format and be within the netmask of the subnet.
 */
@CompileStatic
@Canonical
@PackageScope
class CidrValidator extends TypedValidator<String> {
  Errors errors

  @Override
  void validate( final String cidr ) {
    if ( cidr && !Cidr.parse( ).apply( cidr ).isPresent( ) ) {
      errors.reject( "property.invalid.cidr", [pathTranslate( errors.getNestedPath( ) ), cidr ] as Object[], 'Invalid CIDR for \"{0}\": \"{1}\"' )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class EdgeSubnetValidator extends TypedValidator<EdgeSubnet> {
  Errors errors

  @Override
  void validate( final EdgeSubnet subnet ) {
    // Subnet and Netmask are required and must be valid
    require( subnet.&getSubnet )
    require( subnet.&getNetmask )
    require( subnet.&getGateway )
    validate( subnet.&getSubnet, new IPValidator( errors ) )
    validate( subnet.&getNetmask, new NetmaskValidator( errors ) )
    validate( subnet.&getGateway, new IPValidator( errors ) )

    if ( !errors.hasErrors( ) ) {
      // The subnet must be a valid ID when tested against the netmask
      int subnetInt = PrivateAddresses.asInteger( subnet.subnet )
      int netmaskInt = PrivateAddresses.asInteger( subnet.netmask )
      if ( ( subnetInt & netmaskInt ) != subnetInt ) {
        errors.reject( "property.invalid.subnet", [ pathTranslate( errors.getNestedPath( ) ), subnet.subnet ] as Object[ ], 'Invalid subnet due to netmask for subnet \"{0}\": \"{1}\"' )
      }

      // The gateway must be within the subnet
      int gatewayInt = PrivateAddresses.asInteger(subnet.gateway)
      if ( ( gatewayInt == subnetInt ) || ( ( gatewayInt & netmaskInt ) != subnetInt ) ) {
        errors.reject( "property.invalid.subnet", [ pathTranslate(errors.getNestedPath( ) ), subnet.gateway ] as Object[ ], 'Invalid gateway due to subnet/netmask for subnet \"{0}\": \"{1}\"')
      }
    }
  }
}

/**
 * EdgeSubnet group validator class. This class allow to validate a given EdgeSubnet against
 * a group of subnets provided by subnetNames which is a list of all the subnets name configured.
 * If a given EdgeSubnet is contained in the subnetNames list, its already considered valid or
 * else a previous call would have failed.
 */
@CompileStatic
@Canonical
@PackageScope
class ReferenceEdgeSubnetValidator extends TypedValidator<EdgeSubnet> {
  Errors errors
  List<String> subnetNames

  @Override
  void validate( final EdgeSubnet subnet ) {
    if ( ( subnet.name == null ) || !subnetNames.contains( subnet.name ) || subnet.subnet || subnet.netmask || subnet.gateway ) {
      new EdgeSubnetValidator( errors ).validate( subnet )
    }
  }
}

/**
 * ManagedSubnet validator class. A MANAGED style subnet is considered valid if ALL of the following
 * conditions are met:
 *    - It has a subnet AND a netmask AND a network segment size is provided
 *    - The provided subnet ID is a valid IP address format
 *    - The provided netmask is a valid netmask format
 *    - The segmentSize is provided, it must be greater or equal to 16 and a valid power of 2
 *    - If minVlan is provided, then it should not be greater than maxVlan (if provided) and no less than 1
 *    - If maxVlan is provided, then it should not be less than minVlan (if provided) and no more than 4095
 */
@CompileStatic
@Canonical
@PackageScope
class ManagedSubnetValidator extends TypedValidator<ManagedSubnet> {
  Errors errors

  @Override
  void validate( final ManagedSubnet subnet ) {
    if ( !subnet ) return

    // Subnet and Netmask are required and must be valid
    require( subnet.&getSubnet )
    require( subnet.&getNetmask )
    validate( subnet.&getSubnet, new IPValidator( errors ) )
    validate( subnet.&getNetmask, new NetmaskValidator( errors ) )

    if ( !errors.hasErrors( ) ) {
      // The subnet must be a valid ID when tested against the netmask
      int subnetInt = PrivateAddresses.asInteger( subnet.subnet )
      int netmaskInt = PrivateAddresses.asInteger( subnet.netmask )
      if ( ( subnetInt & netmaskInt ) != subnetInt ) {
        errors.reject( "property.invalid.managedsubnet", [ pathTranslate( errors.getNestedPath( ) ), subnet.netmask ] as Object[ ], 'Invalid managed subnet due to netmask for subnet \"{0}\": \"{1}\"' )
      }

      // The segment size must be a power of 2 no less than 16 and no more than 2048 (e.g. 16, 32, 64, 128, 256, 512, etc.)
      if ( subnet.segmentSize ) {
        int segSizeInt = subnet.segmentSize
        if (segSizeInt < ManagedSubnet.MIN_SEGMENT_SIZE) {
          errors.reject("property.invalid.managedsubnet", [pathTranslate(errors.getNestedPath()), subnet.segmentSize] as Object[], 'Invalid managed subnet due to SegmentSize size being less than 16 for subnet "{0}": "{1}"')
        } else if (segSizeInt > ManagedSubnet.MAX_SEGMENT_SIZE) {
          errors.reject("property.invalid.managedsubnet", [pathTranslate(errors.getNestedPath()), subnet.segmentSize] as Object[], 'Invalid managed subnet due to SegmentSize size being more than 2048 for subnet "{0}": "{1}"')
        } else if ((segSizeInt & (segSizeInt - 1)) != 0) {
          errors.reject("property.invalid.managedsubnet", [pathTranslate(errors.getNestedPath()), subnet.segmentSize] as Object[], 'Invalid managed subnet due to SegmentSize not being a power of 2 for subnet "{0}": "{1}"')
        }
      }

      // Were we provided with the min VLAN?
      if ( subnet.minVlan ) {
        int minVlanInt = subnet.minVlan

        // Check if minVlanInt is less than 1 or greater than 4095. If we have a max VLAN provided
        // it should not exceed this value either
        if ( minVlanInt < ManagedSubnet.MIN_VLAN ) {
          errors.reject( "property.invalid.managedsubnet", [ pathTranslate( errors.getNestedPath( ) ), subnet.minVlan, ManagedSubnet.MIN_VLAN ] as Object[ ], 'Invalid managed subnet due to MinVlan for subnet "{0}": "{1}" < "{2}"' )
        } else if ( minVlanInt > ManagedSubnet.MAX_VLAN ) {
          errors.reject( "property.invalid.managedsubnet", [ pathTranslate( errors.getNestedPath( ) ), subnet.minVlan, ManagedSubnet.MAX_VLAN ] as Object[ ], 'Invalid managed subnet due to MinVlan for subnet "{0}": "{1}" > "{2}"' )
        } else if ( !ManagedSubnets.validSegmentForSubnet( subnet, minVlanInt ) ) {
          errors.reject( "property.invalid.managedsubnet", [ pathTranslate( errors.getNestedPath( ) ), subnet.minVlan, ManagedSubnets.restrictToMaximumSegment( subnet, minVlanInt ) ] as Object[ ], 'Invalid managed subnet due to MinVlan being greater than the maximum usable vlan for the subnet and segment size "{0}": MinVlan "{1}" > maximum usable min vlan "{2}"' )
        } else if ( subnet.maxVlan ) {
          int maxVlanInt = subnet.maxVlan
          if ( minVlanInt > maxVlanInt ) {
            errors.reject( "property.invalid.managedsubnet", [ pathTranslate( errors.getNestedPath( ) ), subnet.minVlan, subnet.maxVlan ] as Object[ ], 'Invalid managed subnet due to MinVlan being greater than MaxVlan for subnet "{0}": MinVlan "{1}" > MaxVlan "{2}"' )
          }
        }
      }

      // Were we provided with the max VLAN?
      if ( subnet.maxVlan ) {
        int maxVlanInt = subnet.maxVlan

        // We only have to validate max VLAN against 1 and 4095. If both max and min VLAN were
        // provided, they would have been validated in the "min VLAN" case above.
        if ( maxVlanInt < ManagedSubnet.MIN_VLAN ) {
          errors.reject( "property.invalid.managedsubnet", [ pathTranslate( errors.getNestedPath( ) ), subnet.maxVlan, ManagedSubnet.MIN_VLAN ] as Object[ ], 'Invalid managed subnet due to MaxVlan for subnet "{0}": "{1}" < "{2}"')
        } else if ( maxVlanInt > ManagedSubnet.MAX_VLAN ) {
          errors.reject( "property.invalid.managedsubnet", [ pathTranslate( errors.getNestedPath( ) ), subnet.maxVlan, ManagedSubnet.MAX_VLAN ] as Object[ ], 'Invalid managed subnet due to MaxVlan for subnet "{0}": "{1}" > "{2}"')
        }
      }
    }
  }
}

/**
 * Cluster validator class. A cluster is considered valid if ALL of the following conditions are met:
 *    - It has a unique name
 *    - The provided MAC prefix is a valid MAC prefix
 *    - Each provided subnet provided is a valid subnet
 *    - Each private IP provided is a valid IP address
 */
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
    if ( ( subnetNames.size( ) > 1 ) || cluster.subnet ) {
      require( cluster.&getSubnet )
      if ( cluster.subnet )
          validate( cluster.&getSubnet, new ReferenceEdgeSubnetValidator( errors, subnetNames ) )
    }
    if ( cluster.privateIps && ( cluster.privateIps.size( ) > 0 ) )
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
    if ( value && !pattern.matcher( value ).matches( ) ) {
      errors.reject( "property.invalid.regex", [pathTranslate( errors.getNestedPath( ) ), value ] as Object[], errorMessage )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class InclusiveRangeValidator extends TypedValidator<String> {
  Errors errors
  Long min
  Long max
  String errorMessage

  @Override
  void validate( final String value ) {
    Long longValue
    if ( value && ( ( longValue = Longs.tryParse( value ) ) == null || longValue < min || longValue > max ) ) {
      errors.reject( "property.invalid.integer", [pathTranslate( errors.getNestedPath( ) ), value ] as Object[], errorMessage )
    }
  }
}
