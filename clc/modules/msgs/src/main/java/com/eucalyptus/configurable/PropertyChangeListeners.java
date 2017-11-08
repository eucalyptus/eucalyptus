/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.configurable;

import com.eucalyptus.configurable.PropertyDirectory.NoopEventListener;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.primitives.Ints;
import org.apache.log4j.Logger;


public class PropertyChangeListeners {

  public static final Logger LOG = Logger.getLogger(PropertyChangeListener.class);

  public enum IsPositiveInteger implements PropertyChangeListener {
    INSTANCE;

    @SuppressWarnings( "unchecked" )
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      Object numberValue = newValue;
      if ( numberValue instanceof String ) {
        numberValue = Ints.tryParse( String.valueOf( numberValue ) );
      }
      if ( numberValue != null && Number.class.isAssignableFrom( numberValue.getClass( ) ) ) {
        final Number numElem = ( Number ) numberValue;
        if ( numElem.longValue( ) > 0 ) {
          return;
        }
      }
      throw new ConfigurablePropertyException( "Value must be an integer greater than zero" );
    }
  }

  public enum IsNonNegativeInteger implements PropertyChangeListener {
    INSTANCE;

    @SuppressWarnings( "unchecked" )
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      Object numberValue = newValue;
      if ( numberValue instanceof String ) {
        numberValue = Ints.tryParse( String.valueOf( numberValue ) );
      }
      if ( numberValue != null && Number.class.isAssignableFrom( numberValue.getClass( ) ) ) {
        final Number numElem = ( Number ) numberValue;
        if ( numElem.longValue( ) >= 0 ) {
          return;
        }
      }
      throw new ConfigurablePropertyException( "Value must be an integer greater than or equal to zero" );
    }
  }

    public enum IsBoolean implements PropertyChangeListener {
        INSTANCE;

        @SuppressWarnings( "unchecked" )
        @Override
        public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
            String lowerValue = newValue.toString().toLowerCase();
            if ( !"true".equals(lowerValue) && !"false".equals(lowerValue)) {
                throw new ConfigurablePropertyException( "Value must be 'true' or 'false'" );
            }
        }
    }

  public static class CacheSpecListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      try {
        CacheBuilderSpec.parse( String.valueOf( newValue ) );
      } catch ( Exception e ) {
        throw new ConfigurablePropertyException( e.getMessage( ) );
      }
    }
  }

  public static PropertyChangeListener getListenerFromClass( Class<? extends PropertyChangeListener> changeListenerClass ) {
    PropertyChangeListener changeListener;
    if ( !changeListenerClass.equals( NoopEventListener.class ) ) {
      if ( changeListenerClass.isEnum( ) ) {
        changeListener = changeListenerClass.getEnumConstants( )[0];
        } else {
      try {
          changeListener = changeListenerClass.newInstance( );
        } catch ( Throwable e ) {
          LOG.error("Can't set listener to " + changeListenerClass);
          LOG.error(e, e);
          changeListener = NoopEventListener.NOOP;
        }
      }
    } else {
      changeListener = NoopEventListener.NOOP;
    }
    LOG.debug("Property change listener set to: " + changeListener.getClass());
    return changeListener;
  }

}
