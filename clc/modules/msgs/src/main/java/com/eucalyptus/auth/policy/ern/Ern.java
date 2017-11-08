/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.policy.ern;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.policy.PolicySpec;
import com.google.common.base.Strings;
import net.sf.json.JSONException;

public abstract class Ern {
  
  public static final String ARN_PREFIX = "arn:aws:";
  public static final String ARN_WILDCARD = "*";
  public static final Pattern ARN_PATTERN =
      Pattern.compile( "arn:aws:([a-z][a-z0-9-]*):(\\*|[a-z0-9-]+)?:([0-9]{12}|eucalyptus|[*])?:(.*)" );

  private static List<ServiceErnBuilder> ernBuilders = new CopyOnWriteArrayList<>( );

  // Group index of ARN fields in the ARN pattern
  public static final int ARN_PATTERNGROUP_SERVICE = 1;
  public static final int ARN_PATTERNGROUP_REGION = 2;
  public static final int ARN_PATTERNGROUP_ACCOUNT = 3;
  public static final int ARN_PATTERNGROUP_RESOURCE = 4;

  private final String service;
  private final String region;
  private final String account;

  protected Ern( ) {
    this( null, null, null );
  }

  protected Ern(
      @Nullable  final String service,
      @Nullable final String region,
      @Nullable final String account ) {
    this.service = Strings.emptyToNull( service );
    this.region = Strings.emptyToNull( region );
    this.account = Strings.emptyToNull( account );
  }

  public static Ern parse( String ern ) throws JSONException {
    if ( ARN_WILDCARD.equals( ern ) ) return new WildcardResourceName();
    final Matcher matcher = ARN_PATTERN.matcher( ern );
    if ( matcher.matches() ) {
      final String service = matcher.group( ARN_PATTERNGROUP_SERVICE );
      final String region = matcher.group( ARN_PATTERNGROUP_REGION );
      final String account = matcher.group( ARN_PATTERNGROUP_ACCOUNT );
      final String resource = matcher.group( ARN_PATTERNGROUP_RESOURCE );
      for ( final ServiceErnBuilder builder : ernBuilders ) {
        if ( builder.supports( service ) ) {
          return builder.build(
              ern, service,
              "*".equals( region ) ? null : region,
              "*".equals( account ) ? null : account,
              resource );
        }
      }
    }
    throw new JSONException( "'" + ern + "' is not a valid ARN" );
  }

  @Nullable
  public String getService( ) {
    return service;
  }

  @Nullable
  public String getAccount( ) {
    return account;
  }

  @Nullable
  public String getRegion( ) {
    return region;
  }
  
  public abstract String getResourceType( );
  
  public abstract String getResourceName( );

  /**
   * Explode this Ern to a collection suitable for matching against resources.
   *
   * <p>ARNs used in IAM policy can match multiple resource types. Exploding
   * the Ern results in a service specific collection according to the services
   * resource matching semantics.</p>
   *
   * @return The collection of Erns, which must contain at least this Ern.
   */
  @Nonnull
  public Collection<Ern> explode( ) {
    return Collections.singleton( this );
  }

  protected String qualifiedName( String name ) {
    return PolicySpec.qualifiedName( getService( ), name );
  }

  public static void registerServiceErnBuilder( final ServiceErnBuilder builder ) {
    if ( !ernBuilders.contains( builder ) ) {
      ernBuilders.add( builder );
    }
  }
}
