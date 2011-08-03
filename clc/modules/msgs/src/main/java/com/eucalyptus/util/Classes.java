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
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Arrays;

public class Classes {
  private static Logger LOG = Logger.getLogger( Classes.class );
  
  public static Class findAncestor( Object o, Predicate<Class> condition ) {
    return Iterables.find( ancestry( o ), condition );
  }
  
  public static <T> List<T> newInstance( Class<T> type ) {
    if ( !Modifier.isPublic( type.getModifiers( ) ) ) {
      throw new InstantiationError( "Attempt to instantiate a class which is not public: " + type.getCanonicalName( ) );
    } else if ( type.isEnum( ) ) {
      return Lists.newArrayList( type.getEnumConstants( ) );
    } else {
      try {
        return Lists.newArrayList( type.newInstance( ) );
      } catch ( InstantiationException ex ) {
        throw new InstantiationError( "Attempt to instantiate a class which is not public: " + type.getCanonicalName( ) + " because of: " + ex.getMessage( ) );
      } catch ( IllegalAccessException ex ) {
        throw new InstantiationError( "Attempt to instantiate a class which is not public: " + type.getCanonicalName( ) + " because of: " + ex.getMessage( ) );
      }
    }
  }
  
  enum WhateverAsClass implements Function<Object, Class> {
    INSTANCE;
    @Override
    public Class apply( Object o ) {
      return ( o instanceof Class
          ? ( Class ) o
          : o.getClass( ) );
    }
  }
  
  enum ParentClass implements Function<Class, Class> {
    INSTANCE;
    @Override
    public Class apply( Class type ) {
      return type.getSuperclass( );
    }
  }
  
  enum TransitiveClosureImplementedInterfaces implements Function<Class[], List<Class>> {
    INSTANCE;
    @Override
    public List<Class> apply( Class... types ) {
      List<Class> ret = Lists.newArrayList( );
      if ( types.length == 0 ) {
        return ret;
      } else {
        for ( Class t : types ) {
          if ( t.getInterfaces( ).length == 0 ) {
            continue;
//          } else if ( !t.isInterface( ) ) {
//            ret.addAll( Arrays.asList( t.getInterfaces( ) ) );
          } else {
            ret.addAll( Arrays.asList( t.getInterfaces( ) ) );
          }
        }
        List<Class> next = TransitiveClosureImplementedInterfaces.INSTANCE.apply( ret.toArray( new Class[] {} ) );
        ret.addAll( next );
        return ret;
      }
    }
  }
  
  private static final Function<Object, Class> toParentClass( ) {
    return Functions.compose( ParentClass.INSTANCE, WhateverAsClass.INSTANCE );
  }
  
  enum BreadthFirstTransitiveClosure implements Function<Object, List<Class>> {
    INSTANCE;
    
    @Override
    public List<Class> apply( Object input ) {
      List<Class> ret = Lists.newArrayList( );
      if ( input != Object.class ) {
        Class type = WhateverAsClass.INSTANCE.apply( ret );
        ret.add( type );
        List<Class> superInterfaces = TransitiveClosureImplementedInterfaces.INSTANCE.apply( type );
        ret.addAll( superInterfaces );
        List<Class> next = BreadthFirstTransitiveClosure.INSTANCE.apply( type.getSuperclass( ) );
        ret.addAll( next );
      }
      return ret;
    }
    
  }
  
  public static List<Class> ancestry( Object o ) {
    return BreadthFirstTransitiveClosure.INSTANCE.apply( o );
  }
  
  public static List<Class> genericsToClasses( Object o ) {
    List<Class> ret = Lists.newArrayList( );
    if ( !o.getClass( ).isEnum( ) ) {
      ret.addAll( processTypeForGenerics( o.getClass( ).getGenericSuperclass( ) ) );
    }
    ret.addAll( processTypeForGenerics( o.getClass( ).getGenericInterfaces( ) ) );
    return ret;
  }
  
  private static List<Class> processTypeForGenerics( Type... types ) {
    List<Class> ret = Lists.newArrayList( );
    for ( Type t : types ) {
      if ( t instanceof ParameterizedType ) {
        ParameterizedType pt = ( ParameterizedType ) t;
        for ( Type ptType : pt.getActualTypeArguments( ) ) {
          if ( ptType instanceof Class ) {
            ret.add( ( Class ) ptType );
          }
        }
      }
      if ( t instanceof Class ) {
        ret.addAll( processTypeForGenerics( ( ( Class ) t ).getGenericSuperclass( ) ) );
        ret.addAll( processTypeForGenerics( ( ( Class ) t ).getGenericInterfaces( ) ) );
      }
    }
    return ret;
  }
}
