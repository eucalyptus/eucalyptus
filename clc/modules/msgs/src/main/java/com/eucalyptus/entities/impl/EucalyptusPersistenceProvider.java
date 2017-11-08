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
