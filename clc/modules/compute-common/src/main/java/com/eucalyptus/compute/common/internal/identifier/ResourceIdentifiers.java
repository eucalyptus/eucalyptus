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
  private static final Pattern resourcePattern = Pattern.compile( "[0-9a-fA-F]{8}|[0-9a-fA-F]{17}" );
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
