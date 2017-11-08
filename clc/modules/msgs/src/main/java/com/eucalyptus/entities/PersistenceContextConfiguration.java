/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
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
