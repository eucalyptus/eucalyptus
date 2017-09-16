/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.binding;

import static org.hamcrest.Matchers.notNullValue;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Parameters;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class BindingManager {

  private static Map<BindingKey, Binding> bindingMap            = Maps.newConcurrentMap( );
  private static Map<BindingKey,Future<Boolean>> bindingSeedMap = Maps.newConcurrentMap( );

  public static String sanitizeNamespace( String namespace ) {
    return namespace.replaceAll( "(https?://)|(/$)", "" ).replaceAll( "[./-]", "_" );
  }

  public static boolean seedBinding( final String bindingName,
                                     final Class seedClass ) {
    boolean foundComponent = false;
    boolean seeded = false;

    if ( BaseMessage.class.isAssignableFrom( seedClass ) ) {
      Class<?> messageClass = seedClass;
      while ( messageClass != BaseMessage.class ) {
        final ComponentMessage componentMessage = messageClass.getAnnotation( ComponentMessage.class );
        if ( componentMessage != null ) {
          foundComponent = true;
          final BindingKey key = key( Optional.<Class<? extends ComponentId>>fromNullable( componentMessage.value() ), bindingName );
          seeded = trySeed( key , seedClass );
          if ( seeded ) {
            BindingManager.bindingMap.put( key( bindingName ), getBinding( key ) );
          }
          break;
        }
        messageClass = messageClass.getSuperclass( );
      }
    }

    if ( !seeded && !foundComponent ) {
      seeded = trySeed( key( bindingName ), seedClass );
    }

    return seeded;
  }

  public static void waitForSeeding( ) {
    for ( Future<Boolean> seedFuture : bindingSeedMap.values( ) ) {
      try {
        seedFuture.get( );
      } catch ( InterruptedException e ) {
        Thread.currentThread( ).interrupt( );
        break;
      } catch ( ExecutionException e ) {
        final Throwable cause = e.getCause( );
        throw BootstrapException.error( cause.getMessage( ), cause );
      }
    }
  }

  public static boolean isRegisteredBinding( final String bindingName ) {
    final BindingKey key = key( bindingName );
    return BindingManager.bindingMap.containsKey( key );
  }

  public static Binding getBinding( final String bindingName,
                                    final Class<? extends ComponentId> component ) {
    return getBinding( key( Optional.<Class<? extends ComponentId>>fromNullable( component ), bindingName ) );
  }

  public static Binding getBinding( final String bindingName ) {
    return getBinding( key( bindingName ) );
  }

  private static BindingKey key( final String bindingName ) {
    return key( Optional.<Class<? extends ComponentId>>absent( ), bindingName );
  }

  private static BindingKey key( final Optional<Class<? extends ComponentId>> component,
                                 final String bindingName ) {
    return new BindingKey( bindingName, component );
  }

  private static Binding getBinding( final BindingKey key ) {
    if ( BindingManager.bindingMap.containsKey( key ) ) {
      return BindingManager.bindingMap.get( key );
    } else {
      final BindingKey sanitizedKey = key.sanitized( );
      if ( BindingManager.bindingMap.containsKey( sanitizedKey ) ) {
        return BindingManager.bindingMap.get( sanitizedKey );
      } else {
        final Binding newBinding = new Binding( key.getName( ) );
        BindingManager.bindingMap.put( key, newBinding );
        return newBinding;
      }
    }
  }

  private static boolean trySeed( final BindingKey key, final Class seedClass ) {
    boolean seeded = false;
    if ( !BindingManager.bindingMap.containsKey( key ) ) {
      final Binding binding = BindingManager.getBinding( key );
      seeded = true;
      bindingSeedMap.put( key, Threads.enqueue( Empyrean.class, BindingManager.class, new Callable<Boolean>( ) {
        @Override
        public Boolean call( ) throws Exception {
          try {
            binding.seed( seedClass );
            Logs.exhaust( ).trace( "Seeding binding " + key.getName( ) + " for class " + seedClass.getCanonicalName( ) );
            EventRecord.here( BindingManager.class, EventType.BINDING_SEEDED, key.getName( ) + " " + key.component, seedClass.getName() ).debug( );
          } catch ( BindingException e ) {
            throw new BindingException( "Failed to seed binding " + key.getName( ) + " with class " + seedClass, e );
          }
          return true;
        }
      } ) );
    }
    return seeded;
  }

  private static final class BindingKey {
    private final String name;
    private final Optional<Class<? extends ComponentId>> component;

    private BindingKey( final String name,
                        final Optional<Class<? extends ComponentId>> component ) {
      Parameters.checkParam( "name", name, notNullValue( ) );
      Parameters.checkParam( "component", component, notNullValue( ) );
      this.name = name;
      this.component = component;
    }

    public String getName() {
      return name;
    }

    public BindingKey sanitized( ) {
      return new BindingKey( BindingManager.sanitizeNamespace( name ), component );
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final BindingKey that = (BindingKey) o;

      if ( !component.equals( that.component ) ) return false;
      if ( !name.equals( that.name ) ) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + component.hashCode();
      return result;
    }
  }


}
