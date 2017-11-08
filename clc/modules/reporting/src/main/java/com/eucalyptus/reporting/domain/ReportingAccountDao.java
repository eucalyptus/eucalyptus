/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
