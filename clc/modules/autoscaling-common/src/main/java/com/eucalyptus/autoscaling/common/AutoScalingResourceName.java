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
package com.eucalyptus.autoscaling.common;

import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Represents an Auto Scaling specific Amazon Resource Name (ARN)
 * 
 * arn:aws:<service>:<region>:<namespace>:<relative-id>
 */
public class AutoScalingResourceName {
  
  private static final String prefix = "arn:aws:";
  private static final Splitter nameSpliter = Splitter.on(':');
  
  private static final int PART_SERVICE          = 2;
  private static final int PART_NAMESPACE        = 4;
  private static final int PART_RELATIVE_ID_TYPE = 5;
  private static final int PART_RELATIVE_ID_UUID = 6;
  private static final int PART_RELATIVE_NAME_1  = 7;
  private static final int PART_RELATIVE_NAME_2  = 8;
  
  private final String resourceName;
  private final String service;  
  private final String namespace; // account number if you don't speak ARN
  private final String type;
  private final String uuid;
  private final String name1;
  @Nullable
  private final String name2;

  public enum Type { 
    launchConfiguration( "launch configuration", false ), 
    autoScalingGroup( "auto scaling group", false ), 
    scalingPolicy( "scaling policy", true );
    
    private final String description;
    private final boolean scoped;

    private Type( final String description,
                  final boolean scoped ) {
      this.description = description;  
      this.scoped = scoped;
    }
    
    public String describe() {
      return description;
    }

    public boolean isScoped() {
      return scoped;
    }
  }
  
  public AutoScalingResourceName( final String resourceName, 
                                  final String service, 
                                  final String namespace, 
                                  final String type, 
                                  final String uuid,
                                  final String name1,
                                  @Nullable final String name2 ) {
    this.resourceName = resourceName;
    this.service = service;
    this.namespace = namespace;
    this.type = type;
    this.uuid = uuid;
    this.name1 = toValue( name1 );
    this.name2 = toValue( name2 );
  }

  public static AutoScalingResourceName parse( final String resourceName ) throws InvalidResourceNameException {
    return parse( resourceName, null ); 
  }
  
  public static AutoScalingResourceName parse( final String resourceName,
                                               @Nullable final Type type ) throws InvalidResourceNameException {
    if( !resourceName.startsWith( prefix ) ) {
      throw new InvalidResourceNameException( resourceName );
    }
    
    final Iterable<String> nameParts = nameSpliter.split( resourceName );
    final int namePartCount = Iterables.size( nameParts );
    if ( namePartCount < 8 || namePartCount > 9 ) {
      throw new InvalidResourceNameException( resourceName );
    }

    if ( !"autoscaling".equals( Iterables.get( nameParts, PART_SERVICE ) ) ) {
      throw new InvalidResourceNameException( resourceName );
    }

    if ( type != null && !type.name().equals( Iterables.get( nameParts, PART_RELATIVE_ID_TYPE ) ) ) {
      throw new InvalidResourceNameException( resourceName );
    }
    
    return new AutoScalingResourceName(
      resourceName,
      Iterables.get( nameParts, PART_SERVICE ),
      Iterables.get( nameParts, PART_NAMESPACE ),
      Iterables.get( nameParts, PART_RELATIVE_ID_TYPE ),
      Iterables.get( nameParts, PART_RELATIVE_ID_UUID ),
      Iterables.get( nameParts, PART_RELATIVE_NAME_1 ),
      namePartCount > PART_RELATIVE_NAME_2 ? 
          Iterables.get( nameParts, PART_RELATIVE_NAME_2 ) : 
          null
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

  public String getUuid() {
    return uuid;
  }

  public String getScope( final Type type ) {
    if ( !type.isScoped() ) throw new IllegalStateException( "Type not scoped" );
    return name1;
  }

  public String getName( final Type type ) {
    if ( type.isScoped() ) {
      if ( name2 == null ) throw new InvalidResourceNameException( resourceName );
      return name2;
    } else {
      return name1;      
    }
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
  
  public static Predicate<String> isResourceName() {
    return ResourceNamePredicate.INSTANCE;
  } 
  
  private String toValue( final String name ) {
    String value = null;
    if ( name != null ) {
      int slashIndex = name.indexOf( '/' );
      if ( slashIndex <= 0 || slashIndex == name.length() - 1 ) {
        throw new InvalidResourceNameException( resourceName );        
      }
      value = name.substring( slashIndex + 1 );
    }
    return value;
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
