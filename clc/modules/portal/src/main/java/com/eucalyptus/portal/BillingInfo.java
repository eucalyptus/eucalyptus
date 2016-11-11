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

import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.portal.common.PortalMetadata.BillingInfoMetadata;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_billing" )
@Table( name = "billing_info" )
public class BillingInfo extends AbstractOwnedPersistent implements BillingInfoMetadata {
  private static final long serialVersionUID = 1L;

  @Column( name = "billing_reports_bucket" )
  private String billingReportsBucket;

  @Column( name = "billing_detailed_enabled" )
  private Boolean detailedBillingEnabled;

  @ElementCollection
  @CollectionTable( name = "billing_info_tags" )
  @Column( name = "billing_tag" )
  @JoinColumn( name = "billing_info_id" )
  @OrderColumn( name = "billing_tag_index")
  private List<String> activeCostAllocationTags;

  @Override
  protected String createUniqueName() {
    return getAccountNumber( );
  }

  public String getAccountNumber( ) {
    return getDisplayName( );
  }

  @Nullable
  public String getBillingReportsBucket( ) {
    return billingReportsBucket;
  }

  public void setBillingReportsBucket( @Nullable final String billingReportsBucket ) {
    this.billingReportsBucket = billingReportsBucket;
  }

  public Boolean getDetailedBillingEnabled( ) {
    return detailedBillingEnabled;
  }

  public void setDetailedBillingEnabled( final Boolean detailedBillingEnabled ) {
    this.detailedBillingEnabled = detailedBillingEnabled;
  }

  public List<String> getActiveCostAllocationTags( ) {
    return activeCostAllocationTags;
  }

  public void setActiveCostAllocationTags( final List<String> activeCostAllocationTags ) {
    this.activeCostAllocationTags = activeCostAllocationTags;
  }
}
