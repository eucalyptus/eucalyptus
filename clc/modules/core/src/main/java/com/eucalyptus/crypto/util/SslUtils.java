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
import java.util.Set;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;


/**
 *
 */
public class SslUtils {
  private static final LoadingCache<SslCipherSuiteBuilderParams,String[]> SSL_CIPHER_LOOKUP =
    CacheBuilder.newBuilder().maximumSize(32).build(
    new CacheLoader<SslCipherSuiteBuilderParams,String[]>() {
      @Override
      public String[] load( final SslCipherSuiteBuilderParams params ) {
        return ciphers()
            .with( params.getCipherStrings() )
            .enabledCipherSuites( params.getSupportedCipherSuites() );
      }
    });

  public static String[] getEnabledCipherSuites( final String cipherStrings, final String[] supportedCipherSuites ) {
      return SSL_CIPHER_LOOKUP.getUnchecked( params(cipherStrings, supportedCipherSuites) );
  }

  public static String[] getEnabledProtocols( final String protocolsList, final String[] supportedProtocols ) {
    final Iterable<String> protocols =
        Splitter.on( anyOf( ": ," ) ).omitEmptyStrings( ).trimResults( ).split( protocolsList );
    return Iterables.toArray(
        Iterables.filter( protocols, Predicates.in( Arrays.asList( supportedProtocols ) ) ),
        String.class );
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
