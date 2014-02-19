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
package com.eucalyptus.compute.identifier;

import java.util.List;
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
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 */
@ConfigurableClass(
    root = "cloud",
    description = "Properties for compute."
)
public class ResourceIdentifiers {

  private static final ConcurrentMap<String,ResourceIdentifierCanonicalizer> canonicalizers = Maps.newConcurrentMap();
  private static final Pattern resourcePattern = Pattern.compile( "[0-9a-fA-F]{8}" );
  private static final AtomicReference<ResourceIdentifierCanonicalizer> defaultCanonicalizer =
      new AtomicReference<ResourceIdentifierCanonicalizer>( new LowerResourceIdentifierCanonicalizer( ) );
  @ConfigurableField(
      description = "Name of the canonicalizer for resource identifiers.",
      initial = LowerResourceIdentifierCanonicalizer.NAME,
      displayName = "identifier_canonicalizer",
      changeListener = ResourceIdentifierCanonicalizerChangeListener.class
  )
  public static volatile String IDENTIFIER_CANONICALIZER = LowerResourceIdentifierCanonicalizer.NAME;

  static void register( final ResourceIdentifierCanonicalizer canonicalizer ) {
    canonicalizers.put( canonicalizer.getName( ).toLowerCase( ), canonicalizer );
  }

  public static Optional<ResourceIdentifierCanonicalizer> getCanonicalizer( final String name ) {
    return Optional.fromNullable( canonicalizers.get( name.toLowerCase( ) ) );
  }

  public static ResourceIdentifier generate( final String prefix ) {
    return parse( Crypto.generateId( prefix ) );
  }

  public static String generateString( final String prefix ) {
    return generate( prefix ).getIdentifier( );
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

  enum ResourceIdentifierCanonicalizerToName implements Function<ResourceIdentifierCanonicalizer,String> {
    INSTANCE;

    @Nullable
    @Override
    public String apply( @Nullable final ResourceIdentifierCanonicalizer canonicalizer ) {
      return canonicalizer==null ? null : canonicalizer.getName( );
    }
  }
}
