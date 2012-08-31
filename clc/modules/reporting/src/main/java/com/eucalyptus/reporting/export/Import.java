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
package com.eucalyptus.reporting.export;

import java.util.List;
import javax.persistence.EntityTransaction;
import org.hibernate.Criteria;
import com.eucalyptus.entities.Entities;

/**
 *
 */
public class Import {

  public static void deleteAll() {
    for ( final Class<?> reportingClass : ExportUtils.getPersistentClasses() ) {
      deleteAll( reportingClass );
    }
  }

  public static void importData( final ReportingExport export ) {
    final EntityTransaction transaction = Entities.get( ExportUtils.getTemplateClass() );
    try {
      for ( final Object item : export ) {
        Entities.mergeDirect(item);
      }
    } finally {
      transaction.commit();
    }
  }

  private static void deleteAll( final Class<?> persistentClass ) {
    final EntityTransaction transaction = Entities.get( ExportUtils.getTemplateClass() );
    try {
      final List<?> entities = Entities.createCriteria(persistentClass)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
          .setCacheable(false)
          .list();
      for ( final Object entity : entities ) {
        Entities.delete( entity );
      }
    } finally {
      transaction.commit();
    }
  }
}
