/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudwatch.common;

import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 *
 */
public class CloudWatchResourceName {

  private static final int MIN_PARTS = 7;
  private static final int MAX_PARTS = 7;

  private static final String prefix = "arn:aws:";
  private static final Splitter nameSpliter = Splitter.on(':').limit(MAX_PARTS);

  private static final int PART_SERVICE          = 2;
  private static final int PART_NAMESPACE        = 4;
  private static final int PART_RELATIVE_ID_TYPE = 5;
  private static final int PART_RELATIVE_NAME    = 6;

  private final String resourceName;
  private final String service;
  private final String namespace; // account number if you don't speak ARN
  private final String type;
  private final String name;

  public enum Type {
    alarm( "alarm" ),
    ;

    private final String description;

    private Type( final String description ) {
      this.description = description;
    }

    public String describe() {
      return description;
    }
  }

  public CloudWatchResourceName( final String resourceName,
                                 final String service,
                                 final String namespace,
                                 final String type,
                                 final String name ) {
    this.resourceName = resourceName;
    this.service = service;
    this.namespace = namespace;
    this.type = type;
    this.name = name;
  }

  public static CloudWatchResourceName parse( final String resourceName ) throws InvalidResourceNameException {
    return parse( resourceName, null );
  }

  public static CloudWatchResourceName parse(           final String resourceName,
                                              @Nullable final Type type ) throws InvalidResourceNameException {
    if( !resourceName.startsWith( prefix ) ) {
      throw new InvalidResourceNameException( resourceName );
    }

    final Iterable<String> nameParts = nameSpliter.split( resourceName );
    final int namePartCount = Iterables.size( nameParts );
    if ( namePartCount < MIN_PARTS || namePartCount > MAX_PARTS ) {
      throw new InvalidResourceNameException( resourceName );
    }

    if ( !"cloudwatch".equals( Iterables.get( nameParts, PART_SERVICE ) ) ) {
      throw new InvalidResourceNameException( resourceName );
    }

    if ( type != null && !type.name().equals( Iterables.get( nameParts, PART_RELATIVE_ID_TYPE ) ) ) {
      throw new InvalidResourceNameException( resourceName );
    }

    return new CloudWatchResourceName(
        resourceName,
        Iterables.get( nameParts, PART_SERVICE ),
        Iterables.get( nameParts, PART_NAMESPACE ),
        Iterables.get( nameParts, PART_RELATIVE_ID_TYPE ),
        Iterables.get( nameParts, PART_RELATIVE_NAME )
    );
  }

  public String getResourceName() {
    return resourceName;
  }

  public String getService() {
    return service;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public String toString() {
    return resourceName;
  }

  public static Set<String> simpleNames( final Iterable<String> namesAndArns ) {
    return Sets.newHashSet( Iterables.filter( namesAndArns, Predicates.not( isResourceName() ) ) );
  }

  public static Set<String> arns( final Iterable<String> namesAndArns ) {
    return Sets.newHashSet( Iterables.filter( namesAndArns, isResourceName() ) );
  }

  public static Function<String,Optional<CloudWatchResourceName>> asArnOfType( final Type type ) {
    return new Function<String,Optional<CloudWatchResourceName>>() {
      @Override
      public Optional<CloudWatchResourceName> apply( final String value ) {
        Optional<CloudWatchResourceName> name = Optional.absent( );
        if ( isResourceName().apply( value ) ) try {
          name = Optional.of( parse( value, type ) );
        } catch ( InvalidResourceNameException e ) {
          // absent
        }
        return name;
      }
    };
  }

  public static Predicate<String> isResourceName() {
    return ResourceNamePredicate.INSTANCE;
  }

  public static Function<CloudWatchResourceName,String> toName() {
    return StringProperties.NAME;
  }

  public static Function<CloudWatchResourceName,String> toNamespace() {
    return StringProperties.NAMESPACE;
  }

  private enum StringProperties implements Function<CloudWatchResourceName,String> {
    NAME {
      @Override
      public String apply( @Nullable final CloudWatchResourceName input ) {
        return input == null ? null : input.getName( );
      }
    },
    NAMESPACE {
      @Override
      public String apply( @Nullable final CloudWatchResourceName input ) {
        return input == null ? null : input.getNamespace( );
      }
    }
  }

  private enum ResourceNamePredicate implements Predicate<String> {
    INSTANCE;

    @Override
    public boolean apply( final String value ) {
      return value != null && value.startsWith( prefix );
    }
  }

  public static final class InvalidResourceNameException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidResourceNameException( final String resourceName ) {
      super( "Invalid resource name: " + resourceName );
    }
  }
}
