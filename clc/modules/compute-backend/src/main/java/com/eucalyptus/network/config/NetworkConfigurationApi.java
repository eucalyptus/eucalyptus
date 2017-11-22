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
package com.eucalyptus.network.config;

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.math.BigInteger;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.vavr.encodings.VavrEncodingEnabled;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import com.eucalyptus.network.IPRange;
import com.eucalyptus.network.ManagedSubnets;
import com.eucalyptus.network.NetworkMode;
import com.eucalyptus.network.PrivateAddresses;
import com.eucalyptus.network.config.NetworkConfigurationApi.ImmutableNetworkConfigurationStyle;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CompatSupplier;
import com.eucalyptus.util.Parameters;
import com.fasterxml.jackson.databind.AbstractTypeResolver;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import com.google.common.primitives.Longs;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.jackson.datatype.VavrModule;

/**
 *
 */
@Enclosing
@ImmutableNetworkConfigurationStyle
public interface NetworkConfigurationApi {

  static NetworkConfiguration parse( final String config ) throws IOException {
    return Mapping.mapper( ).readValue( new StringReader( config ) {
      @Override
      public String toString( ) {
        return "property";
      }

    }, NetworkConfigurationApi.NetworkConfiguration.class );
  }

  @Immutable
  interface NetworkConfiguration {
    Option<String> mode();
    Option<String> instanceDnsDomain();
    Array<String> instanceDnsServers();
    Option<String> macPrefix();
    Option<Midonet> mido();
    Option<String> publicGateway();
    Array<String> publicIps();  // List of ip address ranges
    Array<String> privateIps(); // List of ip address ranges
    Array<EdgeSubnet> subnets();
    Option<ManagedSubnet> managedSubnet();
    Array<Cluster> clusters();
  }

  @Immutable
  interface Midonet {
    Option<String> eucanetdHost();
    // old config format, top level,single gateway specification
    Option<String> gatewayHost();
    Option<String> gatewayIP();
    Option<String> gatewayInterface();
    // gateway list
    Array<MidonetGateway> gateways();
    // old config format
    Option<String> publicNetworkCidr();
    Option<String> publicGatewayIP();
    // new format
    Option<String> bgpAsn();
  }

  @Immutable
  interface MidonetGateway {
    // old config format
    Option<String> gatewayIP();
    Option<String> gatewayHost();
    Option<String> gatewayInterface();
    // new format
    Option<String> ip();
    Option<String> externalCidr();
    Option<String> externalDevice();
    Option<String> externalIp();
    Option<String> externalRouterIp();
    Option<String> bgpPeerIp();
    Option<String> bgpPeerAsn();
    Array<String> bgpAdRoutes();
  }

  /**
   * Class representation of a private network subnet. At the minimum, a subnet instance
   * contains a representative name, a subnet ID and a netmask.
   *
   * Each subnet must have a unique name to identify them in the JSON configuration string. When
   * passed to the back-end in the Global Network Information XML format, the name will match
   * the subnet ID as this is more important to have a short unique name than a descriptive one.
   */
  interface Subnet {
    Option<String> name();
    Option<String> subnet();
    Option<String> netmask();
  }

  /**
   * Class representation of an EDGE private network subnet. An EDGE subnet differentiate itself
   * by having a specific network gateway.
   */
  @Immutable
  interface EdgeSubnet extends Subnet {
    Option<String> gateway();
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
  @Immutable
  interface ManagedSubnet extends Subnet {
    // Minimum and Maximum values for validation and comparision
    int MIN_VLAN = 2;
    int MAX_VLAN = 4095;
    int MIN_SEGMENT_SIZE = 16;
    int DEF_SEGMENT_SIZE = 32;
    int MAX_SEGMENT_SIZE = 2048;

    // The fields we need configured
    Option<Integer> minVlan();
    Option<Integer> maxVlan();
    Option<Integer> segmentSize();  // This must be a power of 2 (e.g. 16, 32, 64, 128, ..., 2048)
  }

  /**
   * Class representation of a cluster. At the minimum, a cluster contain a name, macPrefix and a subnet. A
   * subnet can be a reference to a globally declared subnet or it can be a local subnet declaration for this
   * cluster only. In both MANAGED modes, the subnet declaration isn't used. Instead a global ManagedSubnet
   * declaration is used. Based on the networking mode, the list of private IPs could be empty (both MANAGED modes)
   * or contain private IP addresses that are valid and within the subnet range (EDGE networking mode).
   */
  @Immutable
  interface Cluster {
    Option<String> name();
    Option<String> macPrefix();
    Option<EdgeSubnet> subnet();
    Array<String> privateIps();
  }

  @Target( ElementType.TYPE )
  @Retention( RetentionPolicy.CLASS )
  @Style( add = "", build = "o", depluralize = true, forceJacksonPropertyNames = false, defaults = @Immutable() )
  @JsonSerialize
  @VavrEncodingEnabled
  @interface ImmutableNetworkConfigurationStyle { }

  class NamedProperty<T> {

    private final String method;
    private final Supplier<T> methodSupplier;

    NamedProperty( final String method, final Supplier<T> methodSupplier ) {
      this.method = Parameters.checkParamNotNullOrEmpty( "method", method );
      this.methodSupplier = methodSupplier;
    }

    static <T> NamedProperty<T> of( final String method, final Supplier<T> methodSupplier ) {
      return new NamedProperty<>( method, methodSupplier );
    }

    public T get( ) {
      return methodSupplier.get( );
    }

    public final String getMethod( ) {
      return method;
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  abstract class TypedValidator<T> implements Validator {

    protected final Errors errors;

    protected TypedValidator( final Errors errors ) {
      this.errors = errors;
    }

    protected Errors getErrors( ) {
      return errors;
    }

    @Override
    public boolean supports( final Class<?> aClass ) {
      return getTargetClass( ).isAssignableFrom( aClass );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public void validate( final Object o, final Errors errors ) {
      validate( (T) o );
    }

    public Class<?> getTargetClass( ) {
      return (Class<?>) ( (ParameterizedType) getClass( ).getGenericSuperclass( ) ).getActualTypeArguments( )[ 0 ];
    }

    public void validate( T target ) {
    }

    public String pathTranslate( String path, String name ) {
      String pathPrefix = Strings.isNullOrEmpty( path ) ? "" : path + (path.endsWith( "." ) ? "" : ".");
      String fullPath = name!=null ? pathPrefix + name : path;
      return Stream.of( fullPath.split( "\\." ) ).map( TypedValidator::toPathName ).mkString( "." );
    }

    public String pathTranslate( String path ) {
      return pathTranslate( path, null );
    }

    public void require( NamedProperty<Option<?>> namedProperty ) {
      final String fieldName = namedProperty.getMethod( );
      final Option<?> optionValue = namedProperty.get( );
      final Object value = optionValue.getOrNull( );
      if (value == null ||!StringUtils.hasText(value.toString())) {
        errors.reject("property.required",
            new Object[]{ pathTranslate( getErrors( ).getNestedPath( ), fieldName ) },
            "Missing required property \"{0}\"" );
      }
    }

    @SuppressWarnings( "unused" )
    public void requireAny( NamedProperty<Array<?>> namedProperty ) {
      // in the previous implementation this would have checked if the property was defined
      // in the json. With this implementation we need a way to determine if the property
      // was absent or was defined as an empty list
    }

    public void forbid( NamedProperty<Option<?>> namedProperty ) {
      final String fieldName = namedProperty.getMethod( );
      final Option<?> optionValue = namedProperty.get( );
      if( optionValue.isDefined( ) ) {
        errors.reject( "property.invalid",
            new Object[]{ pathTranslate(getErrors( ).getNestedPath( ),fieldName) },
            "Invalid use of property \"{0}\"" );
      }
    }

    public void forbidAny( NamedProperty<Array<?>> namedProperty ) {
      forbid( NamedProperty.of( namedProperty.getMethod( ), namedProperty.get( )::headOption ) );
    }

    public void validate( NamedProperty<Option<?>> namedProperty, Validator validator ) {
      validate( namedProperty.get( ).getOrNull( ), namedProperty.getMethod( ), validator );
    }

    public void validate( Object target, String path, Validator validator ) {
      try {
        getErrors( ).pushNestedPath( path );
        ValidationUtils.invokeValidator( validator, target, getErrors( ) );
      } finally {
        getErrors( ).popNestedPath( );
      }
    }

    public void validateAll( NamedProperty<Array<?>> namedProperty, Validator validator ) {
      final String field = namedProperty.getMethod( );
      for ( Tuple2<?, Integer> itemAndIndex : Stream.ofAll( MoreObjects.firstNonNull( namedProperty.get( ), Collections.emptyList( ) ) ).zipWithIndex( ) ) {
        Object target = itemAndIndex._1;
        final Integer index = itemAndIndex._2;
        try {
          getErrors( ).pushNestedPath( field + "[" + String.valueOf( index ) + "]" );
          ValidationUtils.invokeValidator( validator, target, getErrors( ) );
        } finally {
          getErrors( ).popNestedPath( );
        }
      }
    }

    public static String toPathName( String name ) {
      return CaseFormat.LOWER_CAMEL.to( CaseFormat.UPPER_CAMEL, name );
    }
  }

  class NetworkConfigurationValidator extends TypedValidator<NetworkConfiguration> {
    static final Pattern MAC_PREFIX_PATTERN = Pattern.compile( "[0-9a-fA-F]{2}:[0-9a-fA-F]{2}" );
    static final Pattern MODE_PATTERN = Pattern.compile(
        Joiner.on('|').join( Array.of( NetworkMode.values( ) ).map( Object::toString ) )
    );

    NetworkConfigurationValidator( final Errors errors ) {
      super( errors );
    }

    @Override
    public void validate( final NetworkConfiguration configuration ) {
      requireAny( NamedProperty.of( "publicIps", configuration::publicIps ) );
      validate( NamedProperty.of( "mode", configuration::mode ), new RegexValidator( errors, MODE_PATTERN, "Invalid mode \"{0}\": \"{1}\"" ) );
      validate( NamedProperty.of( "instanceDnsDomain", configuration::instanceDnsDomain ), new DomainValidator(errors) );
      validateAll( NamedProperty.of( "instanceDnsServers", configuration::instanceDnsServers ), new IPValidator(errors) );
      validate( NamedProperty.of( "macPrefix", configuration::macPrefix ), new RegexValidator( errors, MAC_PREFIX_PATTERN, "Invalid MAC prefix \"{0}\": \"{1}\"" ) );
      validateAll( NamedProperty.of( "publicIps", configuration::publicIps ), new IPRangeValidator( errors ) );
      validateAll( NamedProperty.of( "privateIps", configuration::privateIps ), new IPRangeValidator( errors ) );
      if ( "VPCMIDO".equals( configuration.mode().getOrNull( ) ) ) {
        require( NamedProperty.of( "mido", configuration::mido ) );
        if ( configuration.mido().isDefined() ) {
          validate( NamedProperty.of( "mido", configuration::mido ), new MidonetValidator( errors ) );
        }
      } else if ( "EDGE".equals( configuration.mode().getOrElse("EDGE") ) ) {
        // In EDGE modes, we need the subnets information which is optional globally. If a managed subnet
        // is provided, then we have an error
        validateAll( NamedProperty.of( "subnets", configuration::subnets ), new EdgeSubnetValidator( errors ) );
        if ( configuration.managedSubnet( ).isDefined( ) ) {
          errors.reject( "property.invalid.subnet", new Object[]{ pathTranslate( errors.getNestedPath( ) ), configuration.mode().getOrNull( ) }, "Unexpected ManagedSubnet declaration in EDGE mode \"{0}\": \"{1}\"" );
        }
      } else {
        // In MANAGED modes, we need the managed subnet information. If subnets are provided, then we have an error
        require( NamedProperty.of( "managedSubnet", configuration::managedSubnet ) );
        validate( NamedProperty.of( "managedSubnet", configuration::managedSubnet ), new ManagedSubnetValidator( errors ) );
        if ( !configuration.subnets( ).isEmpty( ) ) {
          errors.reject( "property.invalid.subnet", new Object[]{ pathTranslate( errors.getNestedPath( ) ), configuration.mode().getOrNull( ) }, "Unexpected Subnets declaration for non-EDGE mode \"{0}\": \"{1}\"" );
        }
      }
      validateAll( NamedProperty.of( "clusters", configuration::clusters ), new ClusterValidator(errors, configuration.subnets().flatMap(subnet -> subnet.name().orElse(subnet.subnet())) ) );
    }
  }

  class MidonetValidator extends TypedValidator<Midonet> {
    MidonetValidator( final Errors errors ) {
      super( errors );
    }

    private Predicate<MidonetGateway> gatewayPredicate( ) {
      return midonetGateway ->
            midonetGateway.ip().isDefined( ) ||
            midonetGateway.externalDevice().isDefined( ) ||
            midonetGateway.externalCidr().isDefined( ) ||
            midonetGateway.externalIp().isDefined( ) ||
            midonetGateway.externalRouterIp().isDefined( ) ||
            midonetGateway.bgpPeerIp().isDefined( ) ||
            midonetGateway.bgpPeerAsn().isDefined( ) ||
            !midonetGateway.bgpAdRoutes().isEmpty( );
    }

    @Override
    public void validate( final Midonet midonet ) {
      if ( midonet == null ) return;
      boolean regularValidation = midonet.gateways( ).find( gatewayPredicate( ) ).isDefined( );
      if ( !regularValidation && midonet.gateways().isEmpty( ) && !midonet.publicNetworkCidr().isDefined() && !midonet.publicGatewayIP().isDefined() ) {
        regularValidation = true;
      }
      if ( regularValidation || !midonet.gatewayHost().isDefined() || !midonet.gatewayIP().isDefined() || !midonet.gatewayInterface().isDefined() ) {
        requireAny( NamedProperty.of( "gateways", midonet::gateways ) );
        validateAll( NamedProperty.of( "gateways", midonet::gateways ), regularValidation ?
            new MidonetGatewayValidator( errors, midonet.bgpAsn().isDefined() ) :
            new MidonetGatewayLegacyValidator( errors ) );
        if ( midonet.gateways().isEmpty( ) ) {
          errors.reject( "property.invalid.gateways", new Object[]{ pathTranslate( errors.getNestedPath( ), "Gateways" ) }, "At least one gateway is required \"{0}\"" );
        } else if ( midonet.gateways().size( ) > 6 ) {
          errors.reject( "property.invalid.gateways", new Object[]{ pathTranslate( errors.getNestedPath( ), "Gateways" ), midonet.gateways().size( ) }, "Maximum allowed gateways (6) exceeded \"{0}\": {1}" );
        }
      }
      if ( !regularValidation ) {
        require( NamedProperty.of( "publicNetworkCidr", midonet::publicNetworkCidr ) );
        require( NamedProperty.of( "publicGatewayIP", midonet::publicGatewayIP ) );
      } else {
        forbid( NamedProperty.of( "publicNetworkCidr", midonet::publicNetworkCidr ) );
        forbid( NamedProperty.of( "publicGatewayIP", midonet::publicGatewayIP ) );
      }
      validate( NamedProperty.of( "eucanetdHost", midonet::eucanetdHost ), new HostValidator(errors) );
      validate( NamedProperty.of( "gatewayHost", midonet::gatewayHost ), new HostValidator(errors) );
      validate( NamedProperty.of( "gatewayIP", midonet::gatewayIP ), new IPValidator(errors) );
      validate( NamedProperty.of( "publicNetworkCidr", midonet::publicNetworkCidr ), new CidrValidator(errors) );
      validate( NamedProperty.of( "publicGatewayIP", midonet::publicGatewayIP ), new IPValidator(errors) );
      validate( NamedProperty.of( "bgpAsn", midonet::bgpAsn ), new InclusiveRangeValidator( errors, 1L, 4294967295L, "Invalid ASN \"{0}\": \"{1}\"" ) );

    }
  }

  class MidonetGatewayLegacyValidator extends TypedValidator<MidonetGateway> {
    MidonetGatewayLegacyValidator( final Errors errors ) {
      super( errors );
    }

    @Override
    public void validate( final MidonetGateway midonetGateway ) {
      forbid( NamedProperty.of( "ip", midonetGateway::ip ) );
      forbid( NamedProperty.of( "externalCidr", midonetGateway::externalCidr ) );
      forbid( NamedProperty.of( "externalDevice", midonetGateway::externalDevice ) );
      forbid( NamedProperty.of( "externalIp", midonetGateway::externalIp ) );
      forbid( NamedProperty.of( "externalRouterIp", midonetGateway::externalRouterIp ) );
      forbid( NamedProperty.of( "bgpPeerIp", midonetGateway::bgpPeerIp ) );
      forbid( NamedProperty.of( "bgpPeerAsn", midonetGateway::bgpPeerAsn ) );
      forbidAny( NamedProperty.of( "bgpAdRoutes", midonetGateway::bgpAdRoutes ) );

      require( NamedProperty.of( "gatewayHost", midonetGateway::gatewayHost ) );
      require( NamedProperty.of( "gatewayIP", midonetGateway::gatewayIP ) );
      require( NamedProperty.of( "gatewayInterface", midonetGateway::gatewayInterface ) );

      validate( NamedProperty.of( "gatewayHost", midonetGateway::gatewayHost ), new HostValidator(errors) );
      validate( NamedProperty.of( "gatewayIP", midonetGateway::gatewayIP ), new IPValidator(errors) );
    }
  }

  class MidonetGatewayValidator extends TypedValidator<MidonetGateway> {
    private final boolean requireBgp;

    MidonetGatewayValidator( final Errors errors, final boolean requireBgp ) {
      super( errors );
      this.requireBgp = requireBgp;
    }

    @Override
    public void validate( final MidonetGateway midonetGateway ) {
      forbid( NamedProperty.of( "gatewayHost", midonetGateway::gatewayHost ) );
      forbid( NamedProperty.of( "gatewayIP", midonetGateway::gatewayIP ) );
      forbid( NamedProperty.of( "gatewayInterface", midonetGateway::gatewayInterface ) );

      require( NamedProperty.of( "ip", midonetGateway::ip ) );
      require( NamedProperty.of( "externalCidr", midonetGateway::externalCidr ) );
      require( NamedProperty.of( "externalDevice", midonetGateway::externalDevice ) );
      require( NamedProperty.of( "externalIp", midonetGateway::externalIp ) );
      if ( requireBgp ) {
        require( NamedProperty.of( "bgpPeerIp", midonetGateway::bgpPeerIp ) );
        require( NamedProperty.of( "bgpPeerAsn", midonetGateway::bgpPeerAsn ) );
        requireAny( NamedProperty.of( "bgpAdRoutes", midonetGateway::bgpAdRoutes ) );
        if ( midonetGateway.bgpAdRoutes( ).isEmpty( ) ) {
          errors.reject("property.invalid.bgproutes", new Object[]{ pathTranslate(errors.getNestedPath()),"BgpAdRoutes" }, "At least one route is required \"{0}\"") ;
        }
      } else {
        require( NamedProperty.of( "externalRouterIp", midonetGateway::externalRouterIp ) );
      }
      validate( NamedProperty.of( "ip", midonetGateway::ip ), new IPValidator(errors) );
      validate( NamedProperty.of( "externalCidr", midonetGateway::externalCidr ), new CidrValidator(errors) );
      validate( NamedProperty.of( "externalIp", midonetGateway::externalIp ), new IPValidator(errors) );
      validate( NamedProperty.of( "externalRouterIp", midonetGateway::externalRouterIp ), new IPValidator(errors) );
      validate( NamedProperty.of( "bgpPeerIp", midonetGateway::bgpPeerIp ), new IPValidator(errors) );
      validate( NamedProperty.of( "bgpPeerAsn", midonetGateway::bgpPeerAsn ), new InclusiveRangeValidator( errors, 1L, 4294967295L, "Invalid ASN \"{0}\": \"{1}\"" ) );
      validateAll( NamedProperty.of( "bgpAdRoutes", midonetGateway::bgpAdRoutes ), new CidrValidator(errors) );

      if ( midonetGateway.externalCidr( ).isDefined( ) ) {
        final IPRange range = IPRange.fromCidr( Cidr.parse( midonetGateway.externalCidr( ).get( ) ) );
        ImmutableMap.of(
            midonetGateway.externalIp(),       "ExternalIp",
            midonetGateway.externalRouterIp(), "ExternalRouterIp",
            midonetGateway.bgpPeerIp(),        "BgpPeerIp"
        ).forEach( ( valueOption, property ) -> {
          if ( valueOption.isDefined( ) && !range.contains( valueOption.flatMap( IPRange.optParse( ) ).get( ) ) ) {
            errors.reject("property.invalid.forexternalcidr", new Object[]{ pathTranslate(errors.getNestedPath(),property),property }, "{1} must be within ExternalCidr \"{0}\"");
          }
        } );
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
  class ClusterValidator extends TypedValidator<Cluster> {
    private final Array<String> subnetNames;

    ClusterValidator( final Errors errors, final Array<String> subnetNames ) {
      super( errors );
      this.subnetNames = subnetNames;
    }

    @Override
    public void validate( final Cluster cluster ) {
      require( NamedProperty.of( "name", cluster::name ) );
      validate( NamedProperty.of( "macPrefix", cluster::macPrefix ), new RegexValidator( errors, NetworkConfigurationValidator.MAC_PREFIX_PATTERN, "Invalid MAC prefix \"{0}\": \"{1}\"" ) );
      if ( ( subnetNames.size( ) > 1 ) || cluster.subnet( ).isDefined( ) ) {
        require( NamedProperty.of( "subnet", cluster::subnet ) );
        validate( NamedProperty.of( "subnet", cluster::subnet ) , new ReferenceEdgeSubnetValidator( errors, subnetNames ) );
      }
      validateAll( NamedProperty.of( "privateIps", cluster::privateIps ), new IPRangeValidator( errors ) );
    }
  }

  class EdgeSubnetValidator extends TypedValidator<EdgeSubnet> {
    EdgeSubnetValidator( final Errors errors ) {
      super( errors );
    }

    @Override
    public void validate( final EdgeSubnet subnet ) {
      // Subnet and Netmask are required and must be valid
      require( NamedProperty.of( "subnet", subnet::subnet ) );
      require( NamedProperty.of( "netmask", subnet::netmask ) );
      require( NamedProperty.of( "gateway", subnet::gateway ) );
      validate( NamedProperty.of( "subnet", subnet::subnet ), new IPValidator( errors ) );
      validate( NamedProperty.of( "netmask", subnet::netmask ), new NetmaskValidator( errors ) );
      validate( NamedProperty.of( "gateway", subnet::gateway ), new IPValidator( errors ) );

      if ( !errors.hasErrors( ) ) {
        // The subnet must be a valid ID when tested against the netmask
        int subnetInt = PrivateAddresses.asInteger( subnet.subnet( ).get( ) );
        int netmaskInt = PrivateAddresses.asInteger( subnet.netmask( ).get( ) );
        if ( ( subnetInt & netmaskInt ) != subnetInt ) {
          errors.reject( "property.invalid.subnet", new Object[]{ pathTranslate( errors.getNestedPath( ) ), subnet.subnet( ).get( ) }, "Invalid subnet due to netmask for subnet \"{0}\": \"{1}\"" );
        }

        // The gateway must be within the subnet
        int gatewayInt = PrivateAddresses.asInteger(subnet.gateway( ).get( ));
        if ( ( gatewayInt == subnetInt ) || ( ( gatewayInt & netmaskInt ) != subnetInt ) ) {
          errors.reject( "property.invalid.subnet", new Object[]{ pathTranslate(errors.getNestedPath( ) ), subnet.gateway( ).get( ) }, "Invalid gateway due to subnet/netmask for subnet \"{0}\": \"{1}\"" );
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
  class ReferenceEdgeSubnetValidator extends TypedValidator<EdgeSubnet> {
    private final Array<String> subnetNames;

    ReferenceEdgeSubnetValidator( final Errors errors, final Array<String> subnetNames ) {
      super( errors );
      this.subnetNames = subnetNames;
    }

    @Override
    public void validate( final EdgeSubnet subnet ) {
      if ( subnet == null ) return;
      if ( ( subnet.name( ).isEmpty( ) || !subnetNames.contains( subnet.name( ).get( ) ) ) ||
          subnet.subnet( ).isDefined( ) ||
          subnet.netmask( ).isDefined( ) ||
          subnet.gateway( ).isDefined( ) ) {
        new EdgeSubnetValidator( errors ).validate( subnet );
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
  class ManagedSubnetValidator extends TypedValidator<ManagedSubnet> {
    ManagedSubnetValidator( final Errors errors ) {
      super( errors );
    }

    @Override
    public void validate( final ManagedSubnet subnet ) {
      if ( subnet == null ) return;

      // Subnet and Netmask are required and must be valid
      require( NamedProperty.of( "subnet", subnet::subnet ) );
      require( NamedProperty.of( "netmask", subnet::netmask ) );
      validate( NamedProperty.of( "subnet", subnet::subnet ), new IPValidator( errors ) );
      validate( NamedProperty.of( "netmask", subnet::netmask ), new NetmaskValidator( errors ) );

      if ( !errors.hasErrors( ) ) {
        // The subnet must be a valid ID when tested against the netmask
        int subnetInt = PrivateAddresses.asInteger( subnet.subnet( ).get( ) );
        int netmaskInt = PrivateAddresses.asInteger( subnet.netmask( ).get( ) );
        if ( ( subnetInt & netmaskInt ) != subnetInt ) {
          errors.reject( "property.invalid.managedsubnet", new Object[]{ pathTranslate( errors.getNestedPath( ) ), subnet.netmask( ).get( ) }, "Invalid managed subnet due to netmask for subnet \"{0}\": \"{1}\"" );
        }

        // The segment size must be a power of 2 no less than 16 and no more than 2048 (e.g. 16, 32, 64, 128, 256, 512, etc.)
        if ( subnet.segmentSize( ).isDefined( ) ) {
          int segSizeInt = subnet.segmentSize( ).get( );
          if (segSizeInt < ManagedSubnet.MIN_SEGMENT_SIZE) {
            errors.reject("property.invalid.managedsubnet", new Object[]{ pathTranslate(errors.getNestedPath()), subnet.segmentSize( ).get( ) }, "Invalid managed subnet due to SegmentSize size being less than 16 for subnet \"{0}\": \"{1}\"");
          } else if (segSizeInt > ManagedSubnet.MAX_SEGMENT_SIZE) {
            errors.reject("property.invalid.managedsubnet", new Object[]{ pathTranslate(errors.getNestedPath()), subnet.segmentSize( ).get( ) }, "Invalid managed subnet due to SegmentSize size being more than 2048 for subnet \"{0}\": \"{1}\"");
          } else if ((segSizeInt & (segSizeInt - 1)) != 0) {
            errors.reject("property.invalid.managedsubnet", new Object[]{ pathTranslate(errors.getNestedPath()), subnet.segmentSize( ).get( ) }, "Invalid managed subnet due to SegmentSize not being a power of 2 for subnet \"{0}\": \"{1}\"");
          }
        }

        // Were we provided with the min VLAN?
        if ( subnet.minVlan( ).isDefined( ) ) {
          int minVlanInt = subnet.minVlan( ).get( );

          // Check if minVlanInt is less than 1 or greater than 4095. If we have a max VLAN provided
          // it should not exceed this value either
          if ( minVlanInt < ManagedSubnet.MIN_VLAN ) {
            errors.reject( "property.invalid.managedsubnet", new Object[]{ pathTranslate( errors.getNestedPath( ) ), subnet.minVlan( ).get( ), ManagedSubnet.MIN_VLAN }, "Invalid managed subnet due to MinVlan for subnet \"{0}\": \"{1}\" < \"{2}\"" );
          } else if ( minVlanInt > ManagedSubnet.MAX_VLAN ) {
            errors.reject( "property.invalid.managedsubnet", new Object[]{ pathTranslate( errors.getNestedPath( ) ), subnet.minVlan( ).get( ), ManagedSubnet.MAX_VLAN }, "Invalid managed subnet due to MinVlan for subnet \"{0}\": \"{1}\" > \"{2}\"" );
          } else if ( !ManagedSubnets.validSegmentForSubnet( subnet, minVlanInt ) ) {
            errors.reject( "property.invalid.managedsubnet", new Object[]{ pathTranslate( errors.getNestedPath( ) ), subnet.minVlan( ).get( ), ManagedSubnets.restrictToMaximumSegment( subnet, minVlanInt ) }, "Invalid managed subnet due to MinVlan being greater than the maximum usable vlan for the subnet and segment size \"{0}\": MinVlan \"{1}\" > maximum usable min vlan \"{2}\"" );
          } else if ( subnet.maxVlan( ).isDefined() ) {
            int maxVlanInt = subnet.maxVlan( ).get( );
            if ( minVlanInt > maxVlanInt ) {
              errors.reject( "property.invalid.managedsubnet", new Object[]{ pathTranslate( errors.getNestedPath( ) ), subnet.minVlan( ).get( ), subnet.maxVlan( ).get( ) }, "Invalid managed subnet due to MinVlan being greater than MaxVlan for subnet \"{0}\": MinVlan \"{1}\" > MaxVlan \"{2}\"" );
            }
          }
        }

        // Were we provided with the max VLAN?
        if ( subnet.maxVlan( ).isDefined( ) ) {
          int maxVlanInt = subnet.maxVlan( ).get( );

          // We only have to validate max VLAN against 1 and 4095. If both max and min VLAN were
          // provided, they would have been validated in the "min VLAN" case above.
          if ( maxVlanInt < ManagedSubnet.MIN_VLAN ) {
            errors.reject( "property.invalid.managedsubnet", new Object[]{ pathTranslate( errors.getNestedPath( ) ), subnet.maxVlan( ).get( ), ManagedSubnet.MIN_VLAN }, "Invalid managed subnet due to MaxVlan for subnet \"{0}\": \"{1}\" < \"{2}\"");
          } else if ( maxVlanInt > ManagedSubnet.MAX_VLAN ) {
            errors.reject( "property.invalid.managedsubnet", new Object[]{ pathTranslate( errors.getNestedPath( ) ), subnet.maxVlan( ).get( ), ManagedSubnet.MAX_VLAN }, "Invalid managed subnet due to MaxVlan for subnet \"{0}\": \"{1}\" > \"{2}\"");
          }
        }
      }
    }
  }

  class DomainValidator extends TypedValidator<String> {
    DomainValidator( final Errors errors ) {
      super( errors );
    }

    @Override
    public void validate( final String domain ) {
      if ( domain != null && !InternetDomainName.isValid( domain ) ) {
        errors.reject( "property.invalid.domain", new Object[]{ pathTranslate( errors.getNestedPath( ) ), domain }, "Invalid domain \"{0}\": \"{1}\"" );
      }
    }
  }

  class HostValidator extends TypedValidator<String> {
    private static final boolean skipHostValidation = Boolean.valueOf( System.getProperty( "com.eucalyptus.network.config.skipHostValidation", "false" ) );

    HostValidator( final Errors errors ) {
      super( errors );
    }

    @Override
    public void validate( final String host ) {
      if ( host != null && !skipHostValidation ) {
        boolean valid;
        try {
          InetAddresses.forString( host );
          valid = true;
        } catch ( IllegalArgumentException e ) {
          valid = InternetDomainName.isValid( host );
        }
        if ( !valid ) {
          errors.reject( "property.invalid.host", new Object[]{ pathTranslate( errors.getNestedPath( ) ), host }, "Invalid host \"{0}\": \"{1}\"" );
        }
      }
    }
  }

  class IPValidator extends TypedValidator<String> {
    IPValidator( final Errors errors ) {
      super( errors );
    }

    @Override
    public void validate( final String ip ) {
      if ( ip != null && !InetAddresses.isInetAddress( ip ) ) {
        errors.reject( "property.invalid.ip", new Object[]{ pathTranslate( errors.getNestedPath( ) ), ip }, "Invalid IP \"{0}\": \"{1}\"" );
      }
    }
  }

  class IPRangeValidator extends TypedValidator<String> {
    IPRangeValidator( final Errors errors ) {
      super( errors );
    }

    @Override
    public void validate( final String range ) {
      if ( range != null && !IPRange.isIPRange( range ) ) {
        errors.reject( "property.invalid.range", new Object[]{ pathTranslate( errors.getNestedPath( ) ), range }, "Invalid IP or IP range for \"{0}\": \"{1}\"" );
      }
    }
  }

  class NetmaskValidator extends TypedValidator<String> {
    NetmaskValidator( final Errors errors ) {
      super( errors );
    }

    @Override
    public void validate( final String netmask ) {
      new IPValidator( errors ).validate( netmask );
      if ( netmask != null && !errors.hasErrors( ) ) {
        final BigInteger netmaskBigInteger = new BigInteger( InetAddresses.forString( netmask ).getAddress( ) );
        int i = 31;
        for ( ; i > -1 ; i-- ) {
          if ( !netmaskBigInteger.testBit( i ) ) break;
        }
        for ( ; i > -1 ; i-- ) {
          if ( netmaskBigInteger.testBit( i ) ) break;
        }
        if ( i!=-1 ) {
          errors.reject( "property.invalid.netmask", new Object[]{ pathTranslate( errors.getNestedPath( ) ), netmask }, "Invalid netmask for \"{0}\": \"{1}\"" );
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
  class CidrValidator extends TypedValidator<String> {
    CidrValidator( final Errors errors ) {
      super( errors );
    }

    @Override
    public void validate( final String cidr ) {
      if ( cidr != null && !Cidr.parse( ).apply( cidr ).isPresent( ) ) {
        errors.reject( "property.invalid.cidr", new Object[]{ pathTranslate( errors.getNestedPath( ) ), cidr }, "Invalid CIDR for \"{0}\": \"{1}\"" );
      }
    }
  }

  class RegexValidator extends TypedValidator<String> {
    private final Pattern pattern;
    private final String errorMessage;

    RegexValidator( final Errors errors, final Pattern pattern, final String errorMessage ) {
      super( errors );
      this.pattern = pattern;
      this.errorMessage = errorMessage;
    }

    @Override
    public void validate( final String value ) {
      if ( value != null && !pattern.matcher( value ).matches( ) ) {
        errors.reject( "property.invalid.regex", new Object[]{ pathTranslate( errors.getNestedPath( ) ), value }, errorMessage );
      }
    }
  }

  class InclusiveRangeValidator extends TypedValidator<String> {
    private final Long min;
    private final Long max;
    private final String errorMessage;

    InclusiveRangeValidator( final Errors errors, final Long min, final Long max, final String errorMessage ) {
      super( errors );
      this.min = min;
      this.max = max;
      this.errorMessage = errorMessage;
    }

    @Override
    public void validate( final String value ) {
      Long longValue;
      if ( value != null && ( ( longValue = Longs.tryParse( value ) ) == null || longValue < min || longValue > max ) ) {
        errors.reject( "property.invalid.integer", new Object[]{ pathTranslate( errors.getNestedPath( ) ), value }, errorMessage );
      }
    }
  }

  class NetworkConfigurationTypeResolver extends AbstractTypeResolver {
    @Override
    public JavaType findTypeMapping( final DeserializationConfig config, final JavaType type ) {
      final Class<?> src = type.getRawClass( );
      if ( !src.isInterface( ) || src.getSimpleName( ).startsWith( "Immutable" ) ) {
        return null;
      }
      final Class<?> dst;
      try {
        dst = Class.forName( src.getPackage( ).getName( ) + ".ImmutableNetworkConfigurationApi$" + src.getSimpleName( ) );
      } catch ( ClassNotFoundException ignore ) {
        return null;
      }
      return config.getTypeFactory( ).constructSpecializedType( type, dst );
    }
  }

  class NetworkConfigurationModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings( "WeakerAccess" )
    public NetworkConfigurationModule( final String name ) {
      super( name );
    }

    NetworkConfigurationModule( ) {
      this( "EucalyptusNetworkConfigurationModule" );
    }

    @Override
    public void setupModule( final SetupContext context ) {
      super.setupModule( context );
      context.addAbstractTypeResolver( new NetworkConfigurationTypeResolver() );
      context.setNamingStrategy( PropertyNamingStrategy.UPPER_CAMEL_CASE );
    }
  }

  class Mapping {
    private static CompatSupplier<ObjectMapper> mapperSupplier =
        CompatSupplier.of( Suppliers.memoize( Mapping::buildMapper ) );

    static ObjectMapper mapper( ) {
      return mapperSupplier.get( );
    }

    private static ObjectMapper buildMapper( ) {
      final ObjectMapper mapper = new ObjectMapper( );
      mapper.registerModule( new VavrModule( ) );
      mapper.registerModule( new NetworkConfigurationApi.NetworkConfigurationModule( ) );
      return mapper;
    }
  }
}
