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

package com.eucalyptus.configurable;

import com.eucalyptus.configurable.PropertyDirectory.NoopEventListener;
import com.google.common.collect.Constraint;

public class PropertyChangeListeners {
  
  public static void applyConstraint( final Object newValue, final Constraint<Object>... constraints ) throws ConfigurablePropertyException {
    for ( final Constraint<Object> testNewValue : constraints ) {
      try {
        final Object constraintedValue = testNewValue.checkElement( newValue );
      } catch ( final Exception ex ) {
        throw new ConfigurablePropertyException( "Failed to evaluate constraint " + testNewValue + " for new value: " + newValue + " because of: "
                                                 + ex.getMessage( ) );
      }
    }
  }
  
  public static PropertyChangeListener withConstraint( final Constraint<Object> testNewValue ) {
    return new PropertyChangeListener( ) {
      
      @Override
      public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {}
      
    };
  }
  
  public enum IsPositiveInteger implements PropertyChangeListener {
    INSTANCE;
    
    @SuppressWarnings( "unchecked" )
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      applyConstraint( newValue, new Constraint<Object>( ) {
        @Override
        public Object checkElement( final Object element ) {
          if ( Number.class.isAssignableFrom( element.getClass( ) ) ) {
            final Number numElem = ( Number ) element;
            if ( numElem.doubleValue( ) < 0d ) {
              throw new IllegalArgumentException( "Value must be greater than zero" );
            }
          }
          return element;
        }
      } );
    }
    
  }
  
  public static PropertyChangeListener getListenerFromClass( Class<? extends PropertyChangeListener> changeListenerClass ) {
    PropertyChangeListener changeListener;
    if ( !changeListenerClass.equals( NoopEventListener.class ) ) {
      if ( changeListenerClass.isEnum( ) ) {
        changeListener = changeListenerClass.getEnumConstants( )[0];
      }
      try {
        changeListener = changeListenerClass.newInstance( );
      } catch ( Throwable e ) {
        changeListener = NoopEventListener.NOOP;
      }
    } else {
      changeListener = NoopEventListener.NOOP;
    }
    return changeListener;
  }
  
}
