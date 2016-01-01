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
