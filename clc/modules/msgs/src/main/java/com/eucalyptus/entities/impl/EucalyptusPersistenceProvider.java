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

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import com.eucalyptus.entities.PersistenceContextConfiguration;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.util.Classes;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 *
 */
public class EucalyptusPersistenceProvider extends HibernatePersistenceProvider {

  /**
   * Build a Hibernate configuration for the given persistence unit.
   *
   * This assumes that the persistence unit metadata is available from
   * PersistenceContexts.
   */
  public Configuration getConfiguration(
      final String persistenceUnitName,
      final Map<String,String> properties) {
    try {
      final EucalyptusEntityManagerFactoryBuilderImpl e = (EucalyptusEntityManagerFactoryBuilderImpl)
          this.getEntityManagerFactoryBuilderOrNull( persistenceUnitName, properties );
      if( e == null ) {
        return null;
      } else {
        return e.buildHibernateConfiguration( e.buildServiceRegistry( ) );
      }
    } catch (PersistenceException var4) {
      throw var4;
    } catch (Exception var5) {
      throw new PersistenceException("Unable to build entity manager factory", var5);
    }
  }

  @Override
  protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(
      final String persistenceUnitName,
      final Map properties,
      final ClassLoader providedClassLoader
  ) {
    final PersistenceContextConfiguration persistenceContextConfiguration =
        PersistenceContexts.getConfiguration( persistenceUnitName );
    if ( persistenceContextConfiguration != null ) {
      final Map integration = wrap( properties );

      final PersistenceUnitInfoDescriptor persistenceUnit =
          new PersistenceUnitInfoDescriptor( info( persistenceContextConfiguration ) );

      return this.getEntityManagerFactoryBuilder( persistenceUnit, integration, providedClassLoader );
    }

    return null;
  }

  @Override
  protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(
      final PersistenceUnitDescriptor persistenceUnitDescriptor,
      final Map integration,
      final ClassLoader providedClassLoader
  ) {
    return new EucalyptusEntityManagerFactoryBuilderImpl( persistenceUnitDescriptor, integration, providedClassLoader );
  }

  private PersistenceUnitInfo info( final PersistenceContextConfiguration configuration  ) {
    final Properties properties = new Properties( );
    properties.putAll( configuration.getProperties( ) );
    return new PersistenceUnitInfo( ) {
      @Override
      public String getPersistenceUnitName( ) {
        return configuration.getName( );
      }

      @Override
      public String getPersistenceProviderClassName( ) {
        return null;
      }

      @Override
      public PersistenceUnitTransactionType getTransactionType( ) {
        return PersistenceUnitTransactionType.RESOURCE_LOCAL;
      }

      @Override
      public DataSource getJtaDataSource( ) {
        return null;
      }

      @Override
      public DataSource getNonJtaDataSource( ) {
        return null;
      }

      @Override
      public List<String> getMappingFileNames( ) {
        return Collections.emptyList( );
      }

      @Override
      public List<URL> getJarFileUrls( ) {
        return Collections.emptyList( );
      }

      @Override
      public URL getPersistenceUnitRootUrl( ) {
        return null;
      }

      @Override
      public List<String> getManagedClassNames( ) {
        return ImmutableList.copyOf( Iterables.transform(
            configuration.getEntityClasses( ),
            Classes.nameFunction( )
        ) );
      }

      @Override
      public boolean excludeUnlistedClasses( ) {
        return true;
      }

      @Override
      public SharedCacheMode getSharedCacheMode( ) {
        return SharedCacheMode.NONE;
      }

      @Override
      public ValidationMode getValidationMode( ) {
        return ValidationMode.NONE;
      }

      @Override
      public Properties getProperties( ) {
        return properties;
      }

      @Override
      public String getPersistenceXMLSchemaVersion( ) {
        return null;
      }

      @Override
      public ClassLoader getClassLoader( ) {
        return EucalyptusPersistenceProvider.class.getClassLoader( );
      }

      @Override
      public void addTransformer( final ClassTransformer classTransformer ) {
      }

      @Override
      public ClassLoader getNewTempClassLoader( ) {
        return EucalyptusPersistenceProvider.class.getClassLoader( );
      }
    };
  }
}
