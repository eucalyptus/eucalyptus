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
package com.eucalyptus.reporting.upgrade;

import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.system.Ats;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 *
 */
class UpgradeUtils {

  static boolean transactionalForEntity( final Class entityClass,
                                         final Function<EntityManager,Boolean> callback ) {
    final String context = Ats.inClassHierarchy( entityClass ).get( PersistenceContext.class ).name();
    final EntityManagerFactory entityManagerFactory = PersistenceContexts.getEntityManagerFactory( context );
    final EntityManager entityManager = entityManagerFactory.createEntityManager( );
    final EntityTransaction transaction = entityManager.getTransaction( );
    transaction.begin();
    boolean success = false;
    try {
      success = callback.apply( entityManager );
    } finally {
      if ( success && !transaction.getRollbackOnly() ) {
        transaction.commit();
      } else {
        transaction.rollback();
      }
    }
    return success;
  }

  static Function<EntityManager,Boolean> dropTables( final List<String> dropTables ) {
    return new Function<EntityManager,Boolean>() {
      @Override
      public Boolean apply( final EntityManager entityManager ) {
        return Iterables.all( dropTables, new Predicate<String>(){
          @Override
          public boolean apply( final String table ) {
            entityManager.createNativeQuery( "drop table if exists " + table ).executeUpdate();
            return true;
          }
        } );
      }
    };
  }
}
