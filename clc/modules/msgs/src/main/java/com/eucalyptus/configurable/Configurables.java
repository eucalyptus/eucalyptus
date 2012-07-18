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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.configurable.PropertyDirectory.NoopEventListener;
import com.eucalyptus.empyrean.Empyrean;
import com.google.common.base.Function;

/**
 * Low-effort annotations for exposing values as being @Configurable.<br/>
 * 
 * To use it simply mark a {@link Class}, {@link Field}, or {@link Method}:
 * 
 * {@code @Configurable("Human readable name")}
 * 
 * The purpose of this annotation is to be the shortest path to exposing
 * something for modification at runtime. To that end, it only contains (and will only ever contain)
 * a single <i>optional</i> argument ({@link Configurable#value()} which can be provided to give a
 * human readable name for the property.
 * 
 * @note {@link ConfigurableIdentifier} is replaced by {@link Configurables.Identifier}
 * @note {@link ConfigurableField#readonly()} is replaced by checking whether
 *       the field is {@link Modifier#FINAL} or is enforced by a {@link Configurables.Constraint}.
 * @note {@link ConfigurableFieldType} is replaced by {@link Configurables.Restricted}.
 * @note {@link ConfigurableClass#root()} is replaced by {@link Configurables.Namespace}
 * @note {@link ConfigurableClass#singleton()} is replaced by {@link Configurables.Singleton}
 * @note {@link ConfigurableClass#deferred()} is no longer applicable.
 * @see {@link Configurable}
 * @see ConfigurableIdentifier
 * @see ChangeListener
 * @see Constraint
 */
public class Configurables {
  
  @Target( { ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface DefaultValue {
    String value( ) default "";
  }
  
  @Target( { ElementType.FIELD, ElementType.METHOD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface Identifier {}
  
  @Target( { ElementType.TYPE, ElementType.FIELD, ElementType.METHOD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface Description {
    String value( );
  }
  
  @Target( { ElementType.TYPE } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface Namespace {
    String value( );
  }
  
  @Target( { ElementType.TYPE } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface Singleton {
    Class<? extends ComponentId> value( ) default Empyrean.class;
  }
  
  @Target( { ElementType.TYPE } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface Restricted {
    boolean internal( ) default false;
    
    boolean hidden( ) default false;
  }
  
  @Target( { ElementType.TYPE } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface Constraint {
    Class<? extends com.google.common.collect.Constraint<?>> value( );
  }
  
  @Target( { ElementType.TYPE } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface ChangeListener {
    Class<? extends PropertyChangeListener> value( ) default NoopEventListener.class;
  }
  
  @Target( { ElementType.FIELD, ElementType.METHOD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface TypeParser {
    Class<?> type( );
    Class<? extends Function<String, ?>> parser( );
  }
  
}
