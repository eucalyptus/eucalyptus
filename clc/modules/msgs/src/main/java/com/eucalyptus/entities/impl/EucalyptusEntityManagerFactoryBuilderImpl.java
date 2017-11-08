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
package com.eucalyptus.entities.impl;

import java.util.HashSet;
import java.util.Map;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.mapping.SimpleAuxiliaryDatabaseObject;
import org.hibernate.service.ServiceRegistry;
import com.eucalyptus.entities.AuxiliaryDatabaseObject;
import com.eucalyptus.entities.PersistenceContextConfiguration;
import com.eucalyptus.entities.PersistenceContexts;
import com.google.common.collect.Sets;

/**
 *
 */
public class EucalyptusEntityManagerFactoryBuilderImpl extends EntityManagerFactoryBuilderImpl {

  private final String name;

  public EucalyptusEntityManagerFactoryBuilderImpl(
      final PersistenceUnitDescriptor persistenceUnit,
      final Map integrationSettings,
      final ClassLoader providedClassLoader
  ) {
    super( persistenceUnit, integrationSettings, providedClassLoader );
    name = persistenceUnit.getName( );
  }

  @Override
  public Configuration buildHibernateConfiguration( final ServiceRegistry serviceRegistry ) {
    final Configuration configuration = super.buildHibernateConfiguration( serviceRegistry );
    final PersistenceContextConfiguration persistenceContextConfiguration =
        PersistenceContexts.getConfiguration( name );
    if ( persistenceContextConfiguration != null ) {
      for ( final AuxiliaryDatabaseObject ado : persistenceContextConfiguration.getAuxiliaryDatabaseObjects( ) ) {
        configuration.addAuxiliaryDatabaseObject(
            new SimpleAuxiliaryDatabaseObject( ado.create( ) , ado.drop( ), Sets.newHashSet( ado.dialect( ) ) )
        );
      }
    }
    return configuration;
  }
}
