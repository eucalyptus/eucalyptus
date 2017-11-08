/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
