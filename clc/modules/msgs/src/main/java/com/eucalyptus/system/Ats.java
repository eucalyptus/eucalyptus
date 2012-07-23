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

package com.eucalyptus.system;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Map;
import com.eucalyptus.util.Classes;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

/**
 * A builder-like utility for interrogating the {@link Annotation}s that may be present on instances
 * of {@link AnnotatedElement}s.
 * TODO:GRZE: wrong package: should be .util
 */
public class Ats implements Predicate<Class> {
  private final List<AnnotatedElement> ancestry = Lists.newArrayList( );
  
  private Ats( AnnotatedElement... ancestry ) {
    for ( AnnotatedElement c : ancestry ) {
      if ( c instanceof AnnotatedElement ) {
        this.ancestry.add( c );
      }
    }
  }
  
  public <A extends Annotation> boolean has( Class<A> annotation ) {
    for ( AnnotatedElement a : this.ancestry ) {
      if ( a.isAnnotationPresent( annotation ) ) {
        return true;
      } else if ( a instanceof Class ) {
        for ( Class<?> inter : ( ( Class<?> ) a ).getInterfaces( ) ) {
          if ( inter.isAnnotationPresent( annotation ) ) {
            return true;
          }
        }
      }
    }
    return false;
  }
  
  public <A extends Annotation> A get( Class<A> annotation ) {
    AnnotatedElement decl = find( annotation );
    return decl == null
                       ? null
                       : decl.getAnnotation( annotation );
  }
  
  public <A extends Annotation> AnnotatedElement find( final Class<A> annotation ) {
    for ( final AnnotatedElement a : this.ancestry ) {
      if ( a.isAnnotationPresent( annotation ) ) {
        return a;
      } else if ( a instanceof Class ) {
        for ( Class<?> inter : ( ( Class<?> ) a ).getInterfaces( ) ) {
          if ( inter.isAnnotationPresent( annotation ) ) {
            return inter;
          }
        }
      }
    }
    return this.ancestry.get( 0 );
  }
  
  enum AtsBuilder implements Function<Object, Ats> {
    INSTANCE;
    
    @Override
    public Ats apply( Object input ) {
      if ( input instanceof Class ) {
        return new Ats( ( AnnotatedElement ) input );
      } else if ( input instanceof AnnotatedElement ) {
        return new Ats( ( AnnotatedElement ) input );
      } else {
        return new Ats( ( AnnotatedElement ) input.getClass( ) );
      }
    }
    
  }
  
  enum AtsHierarchyBuilder implements Function<Object, Ats> {
    INSTANCE;
    
    @Override
    public Ats apply( Object input ) {
      if ( input instanceof AnnotatedElement ) {
        return new Ats( Classes.ancestors( input ).toArray( new Class[] {} ) );
      } else {
        return new Ats( Classes.ancestors( input ).toArray( new Class[] {} ) );
      }
    }
    
  }
  
  private static final Map<Object, Ats> atsCache          = new MapMaker( ).makeComputingMap( AtsBuilder.INSTANCE );
  private static final Map<Object, Ats> atsHierarchyCache = new MapMaker( ).makeComputingMap( AtsHierarchyBuilder.INSTANCE );
  
  public static Ats from( Object o ) {
    return atsCache.get( o );
  }
  
  public static Ats inClassHierarchy( Object o ) {
    return atsHierarchyCache.get( o );
  }
  
  List<AnnotatedElement> getAncestry( ) {
    return this.ancestry;
  }
  
  enum AnnotatedElementToString implements Function<AnnotatedElement, String> {
    INSTANCE;
    
    @Override
    public String apply( AnnotatedElement input ) {
      return input.toString( ) + ":" + "[" + Joiner.on( "," ).join( input.getAnnotations( ) ) + "]";
    }
  }
  
  @Override
  public String toString( ) {
    return String.format( "Ats:class=%s\nAts:ancestor=%s", this.ancestry.get( 0 ),
                          Joiner.on( "Ats:ancestor=" ).join( Lists.transform( this.ancestry, AnnotatedElementToString.INSTANCE ) ) );
  }

  @Override
  public boolean apply( Class input ) {
    return this.has( input );
  }
}
