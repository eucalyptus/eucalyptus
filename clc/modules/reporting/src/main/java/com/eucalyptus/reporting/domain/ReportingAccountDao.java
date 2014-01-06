/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.reporting.domain;

import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;

/**
 * ReportingAccountDao is an object for reading ReportingAccount objects from
 * the database.
 */
public class ReportingAccountDao {
  private static Logger LOG = Logger.getLogger( ReportingAccountDao.class );

  private static ReportingAccountDao instance = new ReportingAccountDao( );

  public static ReportingAccountDao getInstance( ) {
    return instance;
  }

  @Nullable
  public ReportingAccount getReportingAccount( final String accountId ) {
    final ReportingAccount searchAccount = new ReportingAccount( );
    searchAccount.setId( accountId );
    try ( final TransactionResource db = Entities.transactionFor( ReportingAccount.class ) ) {
      return Entities.uniqueResult( searchAccount );
    } catch ( NoSuchElementException e ) {
      // OK
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
    }
    return null;
  }
}
