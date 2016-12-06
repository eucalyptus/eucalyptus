/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.portal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.portal.common.PortalMetadata.BillingAccountMetadata;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_billing" )
@Table( name = "billing_account" )
public class BillingAccount extends AbstractOwnedPersistent implements BillingAccountMetadata {
  private static final long serialVersionUID = 1L;

  @Column( name = "billing_user_access_enabled" )
  private Boolean userAccessEnabled;

  @Override
  protected String createUniqueName() {
    return getAccountNumber( );
  }

  public String getAccountNumber( ) {
    return getDisplayName( );
  }

  public Boolean getUserAccessEnabled( ) {
    return userAccessEnabled;
  }

  public void setUserAccessEnabled( final Boolean userAccessEnabled ) {
    this.userAccessEnabled = userAccessEnabled;
  }
}
