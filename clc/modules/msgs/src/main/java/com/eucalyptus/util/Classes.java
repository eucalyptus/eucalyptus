/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.util;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.apache.log4j.Logger;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class Classes {
  
  public static Class<?> findAncestor( final Object o, final Predicate<Class<?>> condition ) {
    return Iterables.find( ancestors( o ), condition );
  }
  
  @SuppressWarnings( "unchecked" )
  public static <T> T newInstance( final Class<T> type ) {
    if ( !Modifier.isPublic( type.getModifiers( ) ) ) {
      throw new InstantiationError( "Attempt to instantiate a class which is not public: " + type.getCanonicalName( ) );
    } else if ( type.isEnum( ) ) {
      return type.getEnumConstants( )[0];
    } else {
      try {
        T t = type.newInstance( );
        return t;
      } catch ( final InstantiationException ex ) {
        throw new InstantiationError( "Attempt to instantiate a class which is not public: " + type.getCanonicalName( ) + " because of: " + ex.getMessage( ) );
      } catch ( final IllegalAccessException ex ) {
        throw new InstantiationError( "Attempt to instantiate a class which is not public: " + type.getCanonicalName( ) + " because of: " + ex.getMessage( ) );
      }
    }
  }
  
  enum WhateverAsClass implements Function<Object, Class<?>> {
    INSTANCE;
    @Override
    public Class<?> apply( final Object o ) {
      return ( o instanceof Class
          ? ( Class<?> ) o
          : o.getClass( ) );
    }
  }
  
  enum ParentClass implements Function<Class<?>, Class<?>> {
    INSTANCE;
    @Override
    public Class<?> apply( final Class<?> type ) {
      return type.getSuperclass( );
    }
  }
  
  enum TransitiveClosureImplementedInterfaces implements Function<Class<?>[], List<Class<?>>> {
    INSTANCE;
    @Override
    public List<Class<?>> apply( final Class<?>[] types ) {
      final List<Class<?>> ret = Lists.newArrayList( );
      if ( types.length == 0 ) {
        return ret;
      } else {
        for ( final Class<?> t : types ) {
          if ( t.isInterface( ) ) {
            ret.add( t );
          }
          if ( t.getInterfaces( ).length == 0 ) {
            continue;
//          } else if ( !t.isInterface( ) ) {
//            ret.addAll( Arrays.asList( t.getInterfaces( ) ) );
          } else {
            final List<Class<?>> next = TransitiveClosureImplementedInterfaces.INSTANCE.apply( t.getInterfaces( ) );
            ret.addAll( next );
          }
        }
        return ret;
      }
    }
  }
  
  enum BreadthFirstTransitiveClosure implements Function<Object, List<Class<?>>> {
    INSTANCE;
    
    @Override
    public List<Class<?>> apply( final Object input ) {
      final List<Class<?>> ret = Lists.newArrayList( );
      final Class<?> type = WhateverAsClass.INSTANCE.apply( input );
      if ( type == Object.class ) {
        return ret;
      } else if ( type.isInterface( ) ) {
        return ret;
      } else {
        ret.add( type );
        final List<Class<?>> superInterfaces = TransitiveClosureImplementedInterfaces.INSTANCE.apply( new Class[] { type } );
        ret.addAll( superInterfaces );
        final List<Class<?>> next = this.apply( type.getSuperclass( ) );
        ret.addAll( next );
        return ret;
      }
    }
    
  }
  
  /**
   * Function for geting a linearized breadth-first list of classes which belong to the
   * transitive-closure of classes and interfaces implemented by {@code Object o}.
   * 
   * @param o
   * @return
   */
  public static Function<Object, List<Class<?>>> ancestors( ) {
    return BreadthFirstTransitiveClosure.INSTANCE;
  }
  
  /**
   * Get a linearized breadth-first list of classes which belong to the transitive-closure of
   * classes and interfaces implemented by {@code Object o}.
   * 
   * @param o
   * @return
   */
  public static List<Class<?>> ancestors( final Object o ) {
    return ancestors( ).apply( o );
  }
  
  enum ClassBreadthFirstTransitiveClosure implements Function<Object, List<Class<?>>> {
    INSTANCE;
    
    @Override
    public List<Class<?>> apply( final Object input ) {
      final List<Class<?>> ret = Lists.newArrayList( );
      final Class<?> type = WhateverAsClass.INSTANCE.apply( input );
      if ( type == Object.class ) {
        return ret;
      } else if ( type.isInterface( ) ) {
        return ret;
      } else {
        ret.add( type );
        final List<Class<?>> next = this.apply( type.getSuperclass( ) );
        ret.addAll( next );
        return ret;
      }
    }
    
  }
  
  /**
   * Function for geting a linearized breadth-first list of classes which belong to the
   * transitive-closure of
   * classes implemented by {@code Object o}.
   * 
   * @param o
   * @return
   */
  public static Function<Object, List<Class<?>>> classAncestors( ) {
    return ClassBreadthFirstTransitiveClosure.INSTANCE;
  }
  
  /**
   * Get a linearized breadth-first list of classes which belong to the transitive-closure of
   * classes implemented by {@code Object o}.
   * 
   * @param o
   * @return
   */
  public static List<Class<?>> classAncestors( final Object o ) {
    return ClassBreadthFirstTransitiveClosure.INSTANCE.apply( o );
  }
  
  enum InterfaceBreadthFirstTransitiveClosure implements Function<Object, List<Class<?>>> {
    INSTANCE;
    
    @Override
    public List<Class<?>> apply( final Object input ) {
      final List<Class<?>> ret = Lists.newArrayList( );
      final Class<?> type = WhateverAsClass.INSTANCE.apply( input );
      if ( type == Object.class ) {
        return ret;
      } else {
        final List<Class<?>> superInterfaces = TransitiveClosureImplementedInterfaces.INSTANCE.apply( new Class[] { type } );
        ret.addAll( superInterfaces );
        final List<Class<?>> next = this.apply( type.getSuperclass( ) );
        ret.addAll( next );
        return ret;
      }
    }
    
  }
  
  @SuppressWarnings( "unchecked" )
  public static <T> Class<T> typeOf( Object obj ) {
    return ( Class<T> ) WhateverAsClass.INSTANCE.apply( obj );
  }
  
  /**
   * Function for getting a linearized breadth-first list of classes which belong to the
   * transitive-closure of
   * interfaces implemented by {@code Object o}.
   * 
   * @param o
   * @return
   */
  public static Function<Object, List<Class<?>>> interfaceAncestors( ) {
    return InterfaceBreadthFirstTransitiveClosure.INSTANCE;
  }
  
  /**
   * Get a linearized breadth-first list of classes which belong to the transitive-closure of
   * interfaces implemented by {@code Object o}.
   * 
   * @param o
   * @return
   */
  public static List<Class<?>> interfaceAncestors( final Object o ) {
    return interfaceAncestors( ).apply( o );
  }
  
  enum GenericsBreadthFirstTransitiveClosure implements Function<Object, List<Class<?>>> {
    INSTANCE;
    
    @Override
    public List<Class<?>> apply( final Object input ) {
      final List<Class<?>> ret = Lists.newArrayList( );
      if ( !input.getClass( ).isEnum( ) ) {
        ret.addAll( processTypeForGenerics( input.getClass( ).getGenericSuperclass( ) ) );
      }
      ret.addAll( processTypeForGenerics( input.getClass( ).getGenericInterfaces( ) ) );
      return ret;
    }
    
    private static List<Class<?>> processTypeForGenerics( final Type... types ) {
      final List<Class<?>> ret = Lists.newArrayList( );
      for ( final Type t : types ) {
        if ( t instanceof ParameterizedType ) {
          final ParameterizedType pt = ( ParameterizedType ) t;
          for ( final Type ptType : pt.getActualTypeArguments( ) ) {
            if ( ptType instanceof Class ) {
              ret.add( ( Class<?> ) ptType );
            }
          }
        }
        if ( t instanceof Class ) {
          ret.addAll( processTypeForGenerics( ( ( Class<?> ) t ).getGenericSuperclass( ) ) );
          ret.addAll( processTypeForGenerics( ( ( Class<?> ) t ).getGenericInterfaces( ) ) );
        }
      }
      return ret;
    }
    
  }
  
  /**
   * Get a list of the classes corresponding to the actual generic parameters for the given
   * {@code Object o}.
   * 
   * @param o
   * @return
   */
  public static List<Class<?>> genericsToClasses( final Object o ) {
    return genericsToClasses( ).apply( o );
  }
  
  /**
   * Function for getting a list of the classes corresponding to the actual generic parameters for
   * the given {@code Object o}.
   * 
   * @param o
   * @return
   */
  private static Function<Object, List<Class<?>>> genericsToClasses( ) {
    return GenericsBreadthFirstTransitiveClosure.INSTANCE;
  }
  
}
