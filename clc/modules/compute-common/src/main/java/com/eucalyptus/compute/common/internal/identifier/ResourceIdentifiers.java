/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common.internal.identifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.CloudMetadataLongIdentifierConfigurable;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Strings;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 *
 */
@SuppressWarnings( { "Guava", "StaticPseudoFunctionalStyleMethod" } )
@ConfigurableClass(
    root = "cloud",
    description = "Properties for compute."
)
public class ResourceIdentifiers {
  private static final Logger logger = Logger.getLogger( ResourceIdentifiers.class );
  private static final Pattern resourcePattern = Pattern.compile( "[0-9a-fA-F]{8}|[0-9a-fA-F]{17}" );
  private static final Map<String,String> configurableResourcePrefixToResourceMap = Maps.newConcurrentMap( );
  private static final Function<String,Set<String>> shortIdentifierPrefixes =
      FUtils.memoizeLast( ResourceIdentifiers::prefixes );
  private static final Function<String,Set<String>> longIdentifierPrefixes =
      FUtils.memoizeLast( ResourceIdentifiers::prefixes );
  private static final Function<Iterable<String>,Set<String>> longIdentifierResources =
      FUtils.memoizeLast( unordered -> ImmutableSet.copyOf( Ordering.natural( ).sortedCopy( unordered ) ) );
  private static final ConcurrentMap<String,ResourceIdentifierCanonicalizer> canonicalizers = Maps.newConcurrentMap();
  private static final AtomicReference<ResourceIdentifierCanonicalizer> defaultCanonicalizer =
      new AtomicReference<>( new LowerResourceIdentifierCanonicalizer( ) );

  @SuppressWarnings( "unused" )
  @ConfigurableField(
      description = "Name of the canonicalizer for resource identifiers.",
      initial = LowerResourceIdentifierCanonicalizer.NAME,
      displayName = "identifier_canonicalizer",
      changeListener = ResourceIdentifierCanonicalizerChangeListener.class
  )
  public static volatile String IDENTIFIER_CANONICALIZER = LowerResourceIdentifierCanonicalizer.NAME;

  @SuppressWarnings( "WeakerAccess" )
  @ConfigurableField(
      description = "List of resource identifier prefixes for short identifiers or * for all",
      displayName = "short_identifier_prefixes",
      changeListener = ResourceIdentifierPrefixListChangeListener.class
  )
  public static volatile String SHORT_IDENTIFIER_PREFIXES = "";

  @SuppressWarnings( "WeakerAccess" )
  @ConfigurableField(
      description = "List of resource identifier prefixes for long identifiers or * for all",
      displayName = "long_identifier_prefixes",
      changeListener = ResourceIdentifierPrefixListChangeListener.class
  )
  public static volatile String LONG_IDENTIFIER_PREFIXES = "";

  static void register( final ResourceIdentifierCanonicalizer canonicalizer ) {
    canonicalizers.put( canonicalizer.getName( ).toLowerCase( ), canonicalizer );
  }

  @SuppressWarnings( "unused" )
  public static Optional<ResourceIdentifierCanonicalizer> getCanonicalizer( final String name ) {
    return Optional.fromNullable( canonicalizers.get( name.toLowerCase( ) ) );
  }

  /**
   * Generate a long or short identifier based on the prefix and configuration.
   */
  public static ResourceIdentifier generate( final String prefix ) {
    return useLongIdentifierForPrefix( prefix ) ?
        generateLong( prefix ) :
        generateShort( prefix );
  }

  @SuppressWarnings( "WeakerAccess" )
  public static ResourceIdentifier generateShort( final String prefix ) {
    return parse( Crypto.generateId( prefix ) );
  }

  @SuppressWarnings( "WeakerAccess" )
  public static ResourceIdentifier generateLong( final String prefix ) {
    return parse( Crypto.generateLongId( prefix ) );
  }

  public static String generateString( final String prefix ) {
    return generate( prefix ).getIdentifier( );
  }

  public static String generateShortString( final String prefix ) {
    return generateShort( prefix ).getIdentifier( );
  }

  public static String generateLongString( final String prefix ) {
    return generateLong( prefix ).getIdentifier( );
  }

  public static ResourceIdentifier parse( final String expectedPrefix,
                                          final String identifierText ) throws InvalidResourceIdentifier {
    return doParse( expectedPrefix, identifierText );
  }

  public static ResourceIdentifier parse( final String identifierText ) throws InvalidResourceIdentifier {
    return doParse( null, identifierText );
  }

  /**
   * Converts the given identifiers to normal form.
   */
  public static List<String> normalize( final String expectedPrefix,
                                        final Iterable<String> identifiers ) throws InvalidResourceIdentifier {
    return Lists.newArrayList( Iterables.transform( identifiers, normalize( expectedPrefix ) ) );
  }

  public static List<String> normalize( final Iterable<String> identifiers ) throws InvalidResourceIdentifier {
    return Lists.newArrayList( Iterables.transform( identifiers, normalize( ) ) );
  }

  @SuppressWarnings( "WeakerAccess" )
  public static Function<String,String> normalize( ) throws InvalidResourceIdentifier {
    return ResourceIdentifierNormalizeTransform.ENFORCE;
  }

  public static Function<String,String> tryNormalize( ) throws InvalidResourceIdentifier {
    return ResourceIdentifierNormalizeTransform.ATTEMPT;
  }

  @SuppressWarnings( "WeakerAccess" )
  public static Function<String,String> normalize( final String expectedPrefix ) {
    return identifier -> parse( expectedPrefix, identifier ).getIdentifier( );
  }

  @SuppressWarnings( "ConstantConditions" )
  private static ResourceIdentifier doParse( @Nullable final String expectedPrefix,
                                             @Nonnull final String identifierText ) throws InvalidResourceIdentifier {
    if ( identifierText == null ) throw new InvalidResourceIdentifier( identifierText );
    if ( expectedPrefix != null && !identifierText.startsWith( expectedPrefix + '-' ) ) {
      throw new InvalidResourceIdentifier( identifierText );
    }
    final int hexOffset = identifierText.lastIndexOf( '-' ) + 1;
    if ( hexOffset < 2 )  throw new InvalidResourceIdentifier( identifierText );
    if ( !resourcePattern.matcher( identifierText.substring( hexOffset ) ).matches( ) ) {
      throw new InvalidResourceIdentifier( identifierText );
    }
    final ResourceIdentifierCanonicalizer canonicalizer = defaultCanonicalizer.get( );
    return new ResourceIdentifier(
        canonicalizer.canonicalizePrefix( identifierText.substring( 0, hexOffset -1 ) ) +
        "-" +
        canonicalizer.canonicalizeHex( identifierText.substring( hexOffset ) ) );
  }

  public static boolean useLongIdentifierForPrefix( final String prefix ) {
    //noinspection ConstantConditions
    return longIdentifierPrefixes.apply( LONG_IDENTIFIER_PREFIXES ).contains( prefix ) ||
        !shortIdentifierPrefixes.apply( SHORT_IDENTIFIER_PREFIXES ).contains( prefix );
  }

  public static boolean useLongIdentifierForResource( final String resource ) {
    return configurableResourcePrefixToResourceMap.entrySet( ).stream( )
        .filter( entry -> resource.equals( entry.getValue( ) ) )
        .map( Map.Entry::getKey )
        .anyMatch( ResourceIdentifiers::useLongIdentifierForPrefix );
  }

  public static Set<String> getLongIdentifierResources( ) {
    return longIdentifierResources.apply( configurableResourcePrefixToResourceMap.values( ) );
  }

  /**
   * Truncate a long identifier to a short identifier format.
   *
   * @return Null if the given identifier was null else the short identifier
   */
  public static String truncate( final String identifier ) {
    String shortId = identifier;
    if ( identifier != null ) {
        final int index = identifier.lastIndexOf( '-' );
        if ( index == identifier.length() - 18 ) {
          shortId = Strings.truncate( identifier, identifier.length( ) - 9 );
        }
    }
    return shortId;
  }

  private static Set<String> prefixValues( final String prefixList ) {
    final Splitter propertySplitter = Splitter.on( CharMatcher.anyOf( " ,|;" ) ).omitEmptyStrings( ).trimResults( );
    return Sets.newHashSet( propertySplitter.split( prefixList ) );
  }

  private static Set<String> prefixes( final String prefixList ) {
    if ( "*".equals( prefixList ) ) {
      return ImmutableSet.copyOf( configurableResourcePrefixToResourceMap.keySet( ) );
    } else {
      return ImmutableSet.copyOf( prefixValues( prefixList ) );
    }
  }

  private enum ResourceIdentifierNormalizeTransform implements Function<String,String> {
    ATTEMPT{
      @Nonnull
      @Override
      public String apply( final String identifier ) {
        try {
          return ENFORCE.apply( identifier );
        } catch ( InvalidResourceIdentifier e ) {
          return identifier;
        }
      }
    },
    ENFORCE {
      @Nonnull
      @Override
      public String apply( final String identifier ) {
        return parse( identifier ).getIdentifier( );
      }
    }
    ;

    @Nonnull
    @Override
    public abstract String apply( final String identifier );
  }

  @SuppressWarnings( "WeakerAccess" )
  public static final class ResourceIdentifierCanonicalizerChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange( final ConfigurableProperty t, final String newValue ) throws ConfigurablePropertyException {
      final Splitter propertySplitter = Splitter.on( CharMatcher.anyOf( " ,|;" ) ).omitEmptyStrings( ).trimResults( );
      final Iterable<String> canonicalizerValues = propertySplitter.split( newValue );
      for ( final String canonicalizer : canonicalizerValues ) {
        if ( !canonicalizers.containsKey( canonicalizer ) ) {
          throw new ConfigurablePropertyException( "Unknown resource identifier canonicalizer: " + canonicalizer + " in " + newValue );
        }
      }
      defaultCanonicalizer.set( new DispatchingResourceIdentifierCanonicalizer( Iterables.transform(
          canonicalizerValues,
          Functions.forMap( canonicalizers ) ) ) );
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  public static final class ResourceIdentifierPrefixListChangeListener implements PropertyChangeListener<String> {
    private static final Pattern prefixPattern = Pattern.compile( "[a-z](?:[a-z-]{0,30}[a-z])?" );

    @Override
    public void fireChange( final ConfigurableProperty t, final String newValue ) throws ConfigurablePropertyException {
      if ( !"*".equals( newValue ) ) {
        final Set<String> prefixValues = prefixValues( newValue );
        for ( final String prefixValue : prefixValues ) {
          if ( !prefixPattern.matcher( prefixValue ).matches( ) ) {
            throw new ConfigurablePropertyException( "Invalid resource identifier prefix: '" + prefixValue + "' in '" + newValue + "'" );
          }
        }
      }
    }
  }
  enum ResourceIdentifierCanonicalizerToName implements Function<ResourceIdentifierCanonicalizer,String> {
    INSTANCE;

    @Nullable
    @Override
    public String apply( @Nullable final ResourceIdentifierCanonicalizer canonicalizer ) {
      return canonicalizer==null ? null : canonicalizer.getName( );
    }
  }

  public static final class ConfigurableLongResourceIdentifierDiscovery extends ServiceJarDiscovery {
    public ConfigurableLongResourceIdentifierDiscovery( ) {
      configurableResourcePrefixToResourceMap.putAll(
          ImmutableMap.<String, String>builder( )
              .put( "bun", "bundle" )
              .put( "cgw", "customer-gateway" )
              .put( "eipalloc", "elastic-ip-allocation" )
              .put( "eipassoc", "elastic-ip-association" )
              .put( "export-i", "export-task" )
              .put( "fl", "flow-log" )
              .put( "import-i", "import-task" )
              .put( "aclassoc", "network-acl-association" )
              .put( "eni-attach", "network-interface-attachment" )
              .put( "pl", "prefix-list" )
              .put( "r", "reservation" )
              .put( "rtbassoc", "route-table-association" )
              .put( "subnet-cidr-assoc", "subnet-cidr-block-association" )
              .put( "vpc-cidr-assoc", "vpc-cidr-block-association" )
              .put( "vpce", "vpc-endpoint" )
              .put( "pcx", "vpc-peering-connection" )
              .put( "vpn", "vpn-connection" )
              .put( "vgw", "vpn-gateway" )
              .build( )
      );
    }

    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( CloudMetadata.class.isAssignableFrom( candidate ) ) {
        final Ats ats = Ats.from( candidate );
        final CloudMetadataLongIdentifierConfigurable configurable =
            ats.get( CloudMetadataLongIdentifierConfigurable.class );
        if ( configurable != null ) {
          final PolicyResourceType type = ats.get( PolicyResourceType.class );
          if ( type != null ) {
            final String resource = type.value( );
            if ( !configurable.prefix( ).isEmpty( ) ) {
              configurableResourcePrefixToResourceMap.putIfAbsent( configurable.prefix( ), resource );
            }
            for ( final String prefix : configurable.relatedPrefixes( ) ) {
              configurableResourcePrefixToResourceMap.putIfAbsent( prefix, resource );
            }
          } else {
            logger.warn(
                "Ignoring long identifier configuration due to missing @PolicyResourceType annotation for: " +
                configurable.prefix( ) + "/" + Arrays.asList( configurable.relatedPrefixes( ) ) );
          }
          return true;
        }
      }
      return false;
    }

    @Override
    public Double getPriority( ) {
      return 1.0d;
    }
  }
}
