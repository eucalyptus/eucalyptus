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
import com.eucalyptus.configurable.PropertyDirectory.NoopEventListener;

@Target( { ElementType.FIELD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface ConfigurableField {
  String description( ) default "None available.";
  
  String initial( ) default "";
  
  boolean readonly( ) default true;
  
  String displayName( ) default "None";
  
  ConfigurableFieldType type( ) default ConfigurableFieldType.KEYVALUE;
  
  Class<? extends PropertyChangeListener> changeListener( ) default NoopEventListener.class;
  
}
