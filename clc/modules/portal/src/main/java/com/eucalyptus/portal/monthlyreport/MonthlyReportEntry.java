/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.portal.monthlyreport;

import com.eucalyptus.portal.workflow.MonthlyUsageRecord;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Parent;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import java.util.Date;
import java.util.UUID;

@Embeddable
public class MonthlyReportEntry implements MonthlyUsageRecord {
  private static Logger LOG     = Logger.getLogger( MonthlyReportEntry.class );

  @Transient
  private static final long serialVersionUID = 1L;

  public MonthlyReportEntry() {
    updateRecordId();
  }

  @Parent
  private MonthlyReport report;

  public MonthlyReport getReport() { return this.report; }
  public void setReport(final MonthlyReport report) { this.report = report; }

  public MonthlyReport getMonthlyReport() {
    return this.report;
  }

  @Column( name = "invoice_id" )
  private String invoiceId;
  public void setInvoiceID(final String invoiceId) { this.invoiceId = invoiceId; }
  public String getInvoiceID() { return this.invoiceId; }

  @Column( name = "payer_account_id", nullable = false)
  private String payerAccountId;
  public void setPayerAccountId(final String payerAccountId) { this.payerAccountId = payerAccountId; }
  public String getPayerAccountId() { return this.payerAccountId; }

  @Column( name = "linked_account_id" )
  private String linkedAccountId;
  public void setLinkedAccountId(final String linkedAccountId) { this.linkedAccountId = linkedAccountId; }
  public String getLinkedAccountId() { return this.linkedAccountId; }

  @Column( name = "record_type" )
  private String recordType;
  public void setRecordType(final String recordType) { this.recordType = recordType; }
  public String getRecordType() { return this.recordType; }

  @Column( name = "record_id", unique = true, updatable = false, nullable = false )
  private String recordId;
  public void setRecordId(final String recordId) { this.recordId = recordId; }
  public String getRecordId() { return this.recordId; }

  @PreUpdate
  @PrePersist
  public void updateRecordId() {
    if (this.recordId == null) {
      this.recordId = UUID.randomUUID( ).toString( );
    }
  }

  @Column( name = "billing_period_start_date", nullable = false )
  private Date billingPeriodStartDate;
  public void setBillingPeriodStartDate(final Date billingPeriodStartDate) { this.billingPeriodStartDate = billingPeriodStartDate; }
  public Date getBillingPeriodStartDate() { return this.billingPeriodStartDate; }

  @Column( name = "billing_period_end_date", nullable = false )
  private Date billingPeriodEndDate;
  public void setBillingPeriodEndDate(final Date billingPeriodEndDate) { this.billingPeriodEndDate = billingPeriodEndDate; }
  public Date getBillingPeriodEndDate() { return this.billingPeriodEndDate; }

  @Column ( name = "invoice_date" )
  private Date invoiceDate;
  public void setInvoiceDate(final Date invoiceDate) { this.invoiceDate = invoiceDate; }
  public Date getInvoiceDate() { return this.invoiceDate; }

  @Column ( name = "payer_account_name", nullable = false )
  private String payerAccountName;
  public void setPayerAccountName(final String payerAccountName) { this.payerAccountName = payerAccountName; }
  public String getPayerAccountName() { return this.payerAccountName; }

  @Column ( name = "linked_account_name" )
  private String linkedAccountName;
  public void setLinkedAccountName(final String linkedAccountName) { this.linkedAccountName  = linkedAccountName; }
  public String getLinkedAccountName() { return this.linkedAccountName; }

  @Column ( name = "taxation_address" )
  private String taxationAddress;
  public void setTaxationAddress(final String taxationAddress) { this.taxationAddress = taxationAddress; }
  public String getTaxationAddress() { return this.taxationAddress; }

  @Column ( name = "payer_po_number" )
  private String payerPONumber;
  public void setPayerPONumber(final String payerPONumber) { this.payerPONumber = payerPONumber; }
  public String getPayerPONumber() { return this.payerPONumber; }

  @Column ( name = "product_code", nullable = false )
  private String productCode;
  public void setProductCode(final String productCode) { this.productCode = productCode; }
  public String getProductCode() { return this.productCode; }

  @Column ( name = "product_name", nullable = false )
  private String productName;
  public void setProductName(final String productName) { this.productName = productName; }
  public String getProductName() { return this.productName; }

  @Column ( name = "seller_of_record", nullable = false )
  private String sellerOfRecord;
  public void setSellerOfRecord(final String sellerOfRecord) { this.sellerOfRecord = sellerOfRecord; }
  public String getSellerOfRecord() { return this.sellerOfRecord; }

  @Column ( name = "usage_type", nullable = false )
  private String usageType;
  public void setUsageType(final String usageType) { this.usageType = usageType; }
  public String getUsageType() { return this.usageType; }

  @Column ( name = "operation" )
  private String operation;
  public void setOperation(final String operation) { this.operation = operation; }
  public String getOperation() { return this.operation; }

  @Column ( name = "rate_id" )
  private String rateId;
  public void setRateId(final String rateId) { this.rateId = rateId; }
  public String getRateId() { return this.rateId; }

  @Column ( name = "item_description" )
  private String itemDescription;
  public void setItemDescription(final String itemDescription) { this.itemDescription = itemDescription; }
  public String getItemDescription() { return this.itemDescription; }

  @Column ( name = "usage_start_date", nullable = false )
  private Date usageStartDate;
  public void setUsageStartDate (final Date usageStartDate) { this.usageStartDate = usageStartDate; }
  public Date getUsageStartDate() { return this.usageStartDate;}

  @Column ( name = "usage_end_date", nullable = false )
  private Date usageEndDate;
  public void setUsageEndDate(final Date usageEndDate) { this.usageEndDate = usageEndDate; }
  public Date getUsageEndDate() { return this.usageEndDate; }

  @Column ( name = "usage_quantity", nullable = false )
  private Double usageQuantity;
  public void setUsageQuantity(final Double usageQuantity) { this.usageQuantity = usageQuantity; }
  public Double getUsageQuantity() { return this.usageQuantity; }

  @Column ( name = "blended_rate" )
  private String blendedRate;
  public void setBlendedRate(final String blendedRate) { this.blendedRate = blendedRate; }
  public String getBlendedRate() { return this.blendedRate; }

  @Column ( name = "currency_code" )
  private String currencyCode;
  public void setCurrencyCode(final String currencyCode) { this.currencyCode = currencyCode; }
  public String getCurrencyCode() { return this.currencyCode; }

  @Column ( name = "cost_before_tax" )
  private Double costBeforeTax;
  public void setCostBeforeTax(final Double costBeforeTax) { this.costBeforeTax = costBeforeTax; }
  public Double getCostBeforeTax() { return this.costBeforeTax; }

  @Column ( name = "credits" )
  private Double credits;
  public void setCredits(final Double credits) { this.credits = credits; }
  public Double getCredits() { return this.credits; }

  @Column ( name = "tax_amount" )
  private Double taxAmount;
  public void setTaxAmount(final Double taxAmount) { this.taxAmount = taxAmount; }
  public Double getTaxAmount() { return this.taxAmount; }

  @Column ( name = "tax_type" )
  private String taxType;
  public void setTaxType(final String taxType) { this.taxType = taxType; }
  public String getTaxType() { return this.taxType; }

  @Column ( name = "total_cost" )
  private Double totalCost;
  public void setTotalCost(final Double totalCost) { this.totalCost = totalCost; }
  public Double getTotalCost() { return this.totalCost; }

  @Override
  public boolean equals(final Object obj){
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }

    final MonthlyReportEntry other = ( MonthlyReportEntry ) obj;
    if ( this.recordId == null ) {
      if ( other.recordId != null ) {
        return false;
      }
    } else if ( !this.recordId.equals( other.recordId ) ) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( recordId == null ) ? 0 : recordId.hashCode( ) );
    return result;
  }
}
