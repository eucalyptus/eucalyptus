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
package com.eucalyptus.compute.common.internal.account;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.bootstrap.BillOfMaterials;
import com.eucalyptus.entities.AbstractPersistent;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_compute_account" )
public class ComputeAccount extends AbstractPersistent {
  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_account_number", nullable = false, updatable = false, unique = true )
  private String accountNumber;

  @Column( name = "metadata_account_version", nullable = false )
  private String accountVersion;

  protected ComputeAccount( ) {
  }

  protected ComputeAccount(
      final String accountNumber,
      final String accountVersion
  ) {
    this.accountNumber = accountNumber;
    this.accountVersion = accountVersion;
  }

  public static ComputeAccount create( final String accountNumber ) {
    return new ComputeAccount( accountNumber, BillOfMaterials.getVersion( ) );
  }

  public static ComputeAccount exampleWithAccountNumber( final String accountNumber ) {
    return new ComputeAccount( accountNumber, null );
  }

  public String getAccountNumber( ) {
    return accountNumber;
  }

  public void setAccountNumber( final String accountNumber ) {
    this.accountNumber = accountNumber;
  }

  public String getAccountVersion( ) {
    return accountVersion;
  }

  public void setAccountVersion( final String accountVersion ) {
    this.accountVersion = accountVersion;
  }
}
