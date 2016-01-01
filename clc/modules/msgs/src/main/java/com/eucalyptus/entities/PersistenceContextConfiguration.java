/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.entities;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 *
 */
public class PersistenceContextConfiguration {

  private final String name;
  private final List<Class<?>> entityClasses;
  private final List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects;
  private final Map<String,String> properties;

  public PersistenceContextConfiguration(
      final String name,
      final List<Class<?>> entityClasses,
      final Map<String, String> properties ) {
    this( name, entityClasses, Collections.<AuxiliaryDatabaseObject>emptyList( ), properties );
  }

  public PersistenceContextConfiguration(
      final String name,
      final List<Class<?>> entityClasses,
      final List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects,
      final Map<String, String> properties ) {
    this.name = name;
    this.entityClasses = ImmutableList.copyOf( entityClasses );
    this.auxiliaryDatabaseObjects = ImmutableList.copyOf( auxiliaryDatabaseObjects );
    this.properties = ImmutableMap.copyOf( properties );
  }

  public String getName( ) {
    return name;
  }

  public List<Class<?>> getEntityClasses( ) {
    return entityClasses;
  }

  public List<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjects( ) {
    return auxiliaryDatabaseObjects;
  }

  public Map<String, String> getProperties( ) {
    return properties;
  }
}
