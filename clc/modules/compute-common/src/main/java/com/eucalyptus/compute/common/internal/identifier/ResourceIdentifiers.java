/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.common.internal.identifier;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.util.FUtils;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 *
 */
@SuppressWarnings( "Guava" )
@ConfigurableClass(
    root = "cloud",
    description = "Properties for compute."
)
public class ResourceIdentifiers {

  private static final Pattern resourcePattern = Pattern.compile( "[0-9a-fA-F]{8}|[0-9a-fA-F]{17}" );
  private static final Set<String> configurableLongIdentifierResourcePrefixes =
      ImmutableSet.of( "i", "r", "snap", "vol" );
  private static final Function<String,Set<String>> shortIdentifierPrefixes =
      FUtils.memoizeLast( ResourceIdentifiers::prefixes );
  private static final Function<String,Set<String>> longIdentifierPrefixes =
      FUtils.memoizeLast( ResourceIdentifiers::prefixes );
  private static final ConcurrentMap<String,ResourceIdentifierCanonicalizer> canonicalizers = Maps.newConcurrentMap();
  private static final AtomicReference<ResourceIdentifierCanonicalizer> defaultCanonicalizer =
      new AtomicReference<>( new LowerResourceIdentifierCanonicalizer( ) );
  @ConfigurableField(
      description = "Name of the canonicalizer for resource identifiers.",
      initial = LowerResourceIdentifierCanonicalizer.NAME,
      displayName = "identifier_canonicalizer",
      changeListener = ResourceIdentifierCanonicalizerChangeListener.class
  )
  public static volatile String IDENTIFIER_CANONICALIZER = LowerResourceIdentifierCanonicalizer.NAME;

  @ConfigurableField(
      description = "List of resource identifier prefixes for short identifiers (i|r|snap|vol|*)",
      displayName = "short_identifier_prefixes",
      changeListener = ResourceIdentifierPrefixListChangeListener.class
  )
  public static volatile String SHORT_IDENTIFIER_PREFIXES = "";

  @ConfigurableField(
      description = "List of resource identifier prefixes for long identifiers (i|r|snap|vol|*)",
      displayName = "long_identifier_prefixes",
      changeListener = ResourceIdentifierPrefixListChangeListener.class
  )
  public static volatile String LONG_IDENTIFIER_PREFIXES = "";

  static void register( final ResourceIdentifierCanonicalizer canonicalizer ) {
    canonicalizers.put( canonicalizer.getName( ).toLowerCase( ), canonicalizer );
  }

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

  public static ResourceIdentifier generateShort( final String prefix ) {
    return parse( Crypto.generateId( prefix ) );
  }

  public static ResourceIdentifier generateLong( final String prefix ) {
    return parse( Crypto.generateLongId( prefix ) );
  }

  public static String generateString( final String prefix ) {
    return generate( prefix ).getIdentifier( );
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

  public static Function<String,String> normalize( ) throws InvalidResourceIdentifier {
    return ResourceIdentifierNormalizeTransform.ENFORCE;
  }

  public static Function<String,String> tryNormalize( ) throws InvalidResourceIdentifier {
    return ResourceIdentifierNormalizeTransform.ATTEMPT;
  }

  public static Function<String,String> normalize( final String expectedPrefix ) {
    return new Function<String,String>( ){
      @Override
      public String apply( final String identifier ) {
        return parse( expectedPrefix, identifier ).getIdentifier( );
      }
    };
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

  private static boolean useLongIdentifierForPrefix( final String prefix ) {
    //noinspection ConstantConditions
    return !shortIdentifierPrefixes.apply( SHORT_IDENTIFIER_PREFIXES ).contains( prefix ) &&
        longIdentifierPrefixes.apply( LONG_IDENTIFIER_PREFIXES ).contains( prefix );
  }

  private static Set<String> prefixValues( final String prefixList ) {
    final Splitter propertySplitter = Splitter.on( CharMatcher.anyOf( " ,|;" ) ).omitEmptyStrings( ).trimResults( );
    return Sets.newHashSet( propertySplitter.split( prefixList ) );
  }

  private static Set<String> prefixes( final String prefixList ) {
    if ( "*".equals( prefixList ) ) {
      return configurableLongIdentifierResourcePrefixes;
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
}
