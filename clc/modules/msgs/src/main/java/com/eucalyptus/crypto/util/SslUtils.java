/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.crypto.util;

import static java.util.Collections.singleton;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;
import static com.eucalyptus.crypto.util.SslUtils.SslCipherBuilder.ciphers;
import static com.eucalyptus.crypto.util.SslUtils.SslCipherSuiteBuilderParams.params;
import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.contains;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;

/**
 *
 */
public class SslUtils {

  private static final Map<SslCipherSuiteBuilderParams,String[]> SSL_CIPHER_LOOKUP =
      new MapMaker().maximumSize(32).makeComputingMap( SslCipherSuiteBuilder.INSTANCE );

  public static String[] getEnabledCipherSuites( final String cipherStrings, final String[] supportedCipherSuites ) {
    return SSL_CIPHER_LOOKUP.get( params(cipherStrings, supportedCipherSuites) );
  }

  static final class SslCipherSuiteBuilderParams {
    private final String cipherStrings;
    private final String[] supportedCipherSuites;

    private SslCipherSuiteBuilderParams( final String cipherStrings,
                                         final String[] supportedCipherSuites ) {
      this.cipherStrings = cipherStrings;
      this.supportedCipherSuites = supportedCipherSuites.clone();
    }

    public String getCipherStrings() {
      return cipherStrings;
    }

    public String[] getSupportedCipherSuites() {
      return supportedCipherSuites;
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final SslCipherSuiteBuilderParams that = (SslCipherSuiteBuilderParams) o;

      if (!cipherStrings.equals(that.cipherStrings)) return false;
      if (!Arrays.equals( supportedCipherSuites, that.supportedCipherSuites )) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = cipherStrings.hashCode();
      result = 31 * result + Arrays.hashCode(supportedCipherSuites);
      return result;
    }

    static SslCipherSuiteBuilderParams params( final String cipherStrings,
                                               final String[] supportedCipherSuites  ) {
      return new SslCipherSuiteBuilderParams( cipherStrings, supportedCipherSuites );
    }
  }

  private enum SslCipherSuiteBuilder implements Function<SslCipherSuiteBuilderParams,String[]> {
    INSTANCE;

    @Override
    public String[] apply( final SslCipherSuiteBuilderParams params ) {
      return ciphers()
          .with( params.getCipherStrings() )
          .enabledCipherSuites( params.getSupportedCipherSuites() );
    }
  }

  /**
   * Cipher suite builder that allows the OpenSSL syntax for cipher
   * exclusions (! prefix) and supports the ALL, NULL, and EXPORT lists.
   *
   * This also supports + to combine algorithms (e.g. "RSA+AES") and to
   * move ciphers to the end of the list (e.g. "+RC4")
   */
  static final class SslCipherBuilder {
    private final Set<String> cipherStringsSteps = Sets.newLinkedHashSet();
    private final Set<String> excludedCipherStrings = Sets.newHashSet();

    static SslCipherBuilder ciphers() {
      return new SslCipherBuilder();
    }

    SslCipherBuilder with( final String cipherStrings ) {
      return with( Splitter.on( anyOf( ": ," ) ).omitEmptyStrings().trimResults().split( cipherStrings ) );
    }

    SslCipherBuilder with( final Iterable<String> cipherStrings ) {
      addAll(cipherStringsSteps, filter(cipherStrings, not(CipherStringPrefixes.NOT)));
      addAll(excludedCipherStrings, transform(filter(cipherStrings, CipherStringPrefixes.NOT), CipherStringPrefixes.NOT.cleaner()));
      return this;
    }

    String[] enabledCipherSuites( final String[] supportedCipherSuiteArray ) {
      final ImmutableList<String> supportedCipherSuites = copyOf(supportedCipherSuiteArray);
      final ImmutableList<String> excludedCipherSuites = explodeCipherStrings(excludedCipherStrings, supportedCipherSuites);
      final List<String> cipherSuites = newArrayList();
      for ( final String cipherString : cipherStringsSteps ) {
        if ( CipherStringPrefixes.PLUS.apply(cipherString) ) {
          final String cipherStringToShift = CipherStringPrefixes.PLUS.cleaner().apply(cipherString);
          shift(cipherSuites, explodeCipherStrings(singleton(cipherStringToShift), supportedCipherSuites));
        } else {
          cipherSuites.addAll(explodeCipherStrings(singleton(cipherString), supportedCipherSuites));
        }
      }
      return toArray(filter(cipherSuites, and(in(supportedCipherSuites), not(in(excludedCipherSuites)))), String.class);
    }

    void shift( final List<String> cipherSuites,
                final List<String> ciphersSuitesToShift ) {
      // Shift ciphers to the end of the list
      for ( final String cipherSuite : ciphersSuitesToShift ) {
        if ( cipherSuites.remove( cipherSuite ) ) {
          cipherSuites.add( cipherSuite );
        }
      }
    }


    private ImmutableList<String> explodeCipherStrings( final Set<String> cipherStrings,
                                                        final ImmutableList<String> supportedCipherSuites) {
      return copyOf(concat(transform(cipherStrings, cipherStringExploder(supportedCipherSuites))));
    }

    private Function<String,Iterable<String>> cipherStringExploder( final ImmutableList<String> supportedCipherSuites ) {
      return new Function<String,Iterable<String>>() {
        @Override
        public Iterable<String> apply( final String cipherString ) {
          if ( "ALL".equals( cipherString ) ) {
            return supportedCipherSuites;
          } else if ( cipherString.startsWith("TLS_") || cipherString.startsWith("SSL_") ) {
            return singleton(cipherString);
          } else {
            return filter( supportedCipherSuites, toPredicate(cipherString));
          }
        }
      };
    }

    private Predicate<CharSequence> toPredicate( final String cipherString ) {
      final List<Predicate<CharSequence>> predicates = newArrayList();
      for ( final String cipherStringPart : Splitter.on("+").split(cipherString) ) {
        predicates.add( contains(compile("_" + quote(cipherStringPart)  + "(_|$)")) );
      }
      return and(predicates);
    }

    private enum CipherStringPrefixes implements Predicate<String> {
      NOT("!"),
      PLUS("+");

      private final String prefix;

      private CipherStringPrefixes( final String prefix ) {
        this.prefix = prefix;
      }

      @Override
      public boolean apply( final String value ) {
        return value.startsWith( prefix );
      }

      public Function<String,String> cleaner() {
        return new Function<String,String>(){
          @Override
          public String apply( final String value ) {
            return value.substring(1);
          }
        };
      }
    }
  }
}
