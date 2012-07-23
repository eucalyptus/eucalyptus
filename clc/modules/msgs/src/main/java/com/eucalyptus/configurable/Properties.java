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

package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.util.NoSuchElementException;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Fields;

public class Properties {
  
  public static ConfigurableProperty lookup( String fullyQualifiedName ) throws IllegalAccessException {
    return PropertyDirectory.getPropertyEntry( fullyQualifiedName );
  }
  
  public static ConfigurableProperty lookup( Class<?> declaringClass, String fieldName ) throws IllegalAccessException, NoSuchFieldException {
    Field f = Fields.get( declaringClass, fieldName );
    return lookup( propertyName( f ) );
    
  }
  
  public static String propertyName( Field f ) {
    Class c = f.getDeclaringClass( );
    if ( c.isAnnotationPresent( ConfigurableClass.class ) && f.isAnnotationPresent( ConfigurableField.class ) ) {
      ConfigurableClass classAnnote = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
      return classAnnote.root( ) + "." + f.getName( ).toLowerCase( );
    } else {
      throw new NoSuchElementException( Ats.from( f ).toString( ) );
    }
  }
  
}
