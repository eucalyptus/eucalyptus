/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
