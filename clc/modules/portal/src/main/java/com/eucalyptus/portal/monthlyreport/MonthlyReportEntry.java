/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.portal.monthlyreport;

import com.eucalyptus.portal.workflow.MonthlyUsageRecord;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Parent;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

  @Override
  public String toString() {
    final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    return String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"," +
                    "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"," +
                    "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
            this.invoiceId != null ? this.invoiceId : "",
            this.payerAccountId != null ? this.payerAccountId : "",
            this.linkedAccountId != null ? this.linkedAccountId : "",
            this.recordType != null ? this.recordType : "",
            this.recordId != null? this.recordId : "",
            this.billingPeriodStartDate != null ? df.format(this.billingPeriodStartDate) : "",
            this.billingPeriodEndDate != null ? df.format(this.billingPeriodEndDate) : "",
            this.invoiceDate != null ? df.format(this.invoiceDate) : "",
            this.payerAccountName != null ? this.payerAccountName : "",
            this.linkedAccountName != null ? this.linkedAccountName : "",
            this.taxationAddress != null ? this.taxationAddress : "",
            this.payerPONumber != null ? this.payerPONumber : "",
            this.productCode != null ? this.productCode : "",
            this.productName != null ? this.productName : "",
            this.sellerOfRecord != null ? this.sellerOfRecord : "",
            this.usageType != null ? this.usageType : "",
            this.operation != null ? this.operation : "",
            this.rateId != null ? this.rateId : "",
            this.itemDescription != null ? this.itemDescription : "",
            this.usageStartDate != null ? df.format(this.usageStartDate) : "",
            this.usageEndDate != null ? df.format(this.usageEndDate) : "",
            this.usageQuantity != null ? this.usageQuantity : "",
            this.blendedRate != null ? this.blendedRate : "",
            this.currencyCode != null ? this.currencyCode : "",
            this.costBeforeTax != null ? this.costBeforeTax : "",
            this.credits != null ? this.credits : "",
            this.taxAmount != null ? this.taxAmount : "",
            this.taxType != null ? this.taxType : "",
            this.totalCost != null ? this.totalCost : ""
            );
  }
}
