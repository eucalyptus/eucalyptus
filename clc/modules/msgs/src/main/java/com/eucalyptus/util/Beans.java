/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.util;

import java.beans.PropertyDescriptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.util.ReflectionUtils;
import groovy.lang.GroovyObject;

/**
 *
 */
public class Beans {
  public static void setObjectProperty( final Object target, final String property, final Object value ) {
    if ( target instanceof GroovyObject ) {
      ((GroovyObject)target).setProperty( property, value );
    } else {
      final PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor( target.getClass( ), property );
      if ( propertyDescriptor != null ) {
        ReflectionUtils.invokeMethod( propertyDescriptor.getWriteMethod( ), target, value );
      } else {
        throw new InvalidPropertyException( target.getClass( ), property, "set failed" );
      }
    }
  }

  public static Object getObjectProperty( final Object target, final String property ) {
    if ( target instanceof GroovyObject ) {
      return ((GroovyObject)target).getProperty( property );
    } else {
      final PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor( target.getClass( ), property );
      if ( propertyDescriptor != null ) {
        return ReflectionUtils.invokeMethod(
            propertyDescriptor.getReadMethod( ),
            target );
      } else {
        throw new InvalidPropertyException( target.getClass( ), property, "get failed" );
      }
    }
  }
}
