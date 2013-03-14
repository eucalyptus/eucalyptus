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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.system;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
  
  /**
   * Find the nearest conformant Class to the root of the Ats hierarcy that has the argument annotation.
   * @param annotation
   * @return Class which is annotated with {@link annotation} and is closest in terms of sub-typing to {@link #getRootClass()}
   * @throws NoSuchElementException if no such class is found
   */
  public <A extends Annotation,T> Class<T> findAncestor( final Class<A> annotation ) {
    for ( final AnnotatedElement a : this.ancestry ) {
      if ( a instanceof Class && a.isAnnotationPresent( annotation ) ) {
        return ( Class<T> ) a;
      }    
    }
    throw new NoSuchElementException( "Failed to find ancestor with @" + annotation.getSimpleName( ) + " for root class " + getRootClass( ).getSimpleName( ) );
  }
  
  /**
   * Find the nearest conformant AnnotatedElement to the root of the Ats hierarcy that has the argument annotation.
   * @param annotation
   * @return AnnotatedElement which is annotated with {@link annotation} and is closest in terms of sub-typing to {@link #getRootClass()}
   * @throws NoSuchElementException if no such AnnotatedElement is found
   */
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
    return getRootClass( );
  }

  /**
   * @return the root of this annotation hierarchy
   */
  private Class getRootClass( ) {
    return ( Class ) this.ancestry.get( 0 );
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
    if ( o instanceof AccessibleObject ) {
      return atsCache.get( o );
    } else {
      return atsCache.get( Classes.typeOf( o ) );
    }
  }
  
  public static Ats inClassHierarchy( Object o ) {
    if ( o instanceof AccessibleObject ) {
      return atsHierarchyCache.get( o );
    } else {
      return atsHierarchyCache.get( Classes.typeOf( o ) );
    }
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
    return String.format( "Ats:class=%s\nAts:ancestor=%s", getRootClass( ),
                          Joiner.on( "Ats:ancestor=" ).join( Lists.transform( this.ancestry, AnnotatedElementToString.INSTANCE ) ) );
  }

  @Override
  public boolean apply( Class input ) {
    return this.has( input );
  }
}
