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
package com.eucalyptus.util;

import java.beans.PropertyDescriptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.util.ReflectionUtils;
import groovy.lang.GroovyObject;
import groovy.lang.MetaProperty;

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

  public static Class<?> getObjectPropertyType( final Object target, final String property ) {
    if ( target instanceof GroovyObject ) {
      final MetaProperty metaProperty = ((GroovyObject)target).getMetaClass( ).getMetaProperty( property );
      if ( metaProperty != null && metaProperty.getType( ) != null ) {
        return metaProperty.getType( );
      } else {
        throw new InvalidPropertyException( target.getClass( ), property, "get failed" );
      }
    } else {
      final PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor( target.getClass( ), property );
      if ( propertyDescriptor != null ) {
        return propertyDescriptor.getPropertyType( );
      } else {
        throw new InvalidPropertyException( target.getClass( ), property, "get failed" );
      }
    }
  }
}
