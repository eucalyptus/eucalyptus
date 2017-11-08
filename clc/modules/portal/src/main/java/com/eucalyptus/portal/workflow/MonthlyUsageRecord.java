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
package com.eucalyptus.portal.workflow;

import java.util.Date;

public interface MonthlyUsageRecord {
  String getInvoiceID();
  void setInvoiceID(String invoiceId);
  String getPayerAccountId();
  void setPayerAccountId(String accountId);
  String getLinkedAccountId();
  void setLinkedAccountId(String accountId);
  String getRecordType();
  void setRecordType(String type);
  String getRecordId();
  void setRecordId(String recordId);
  Date getBillingPeriodStartDate();
  void setBillingPeriodStartDate(Date startDate);
  Date getBillingPeriodEndDate();
  void setBillingPeriodEndDate(Date endDate);
  Date getInvoiceDate();
  void setInvoiceDate(Date invoiceDate);
  String getPayerAccountName();
  void setPayerAccountName(String name);
  String getLinkedAccountName();
  void setLinkedAccountName(String name);
  String getTaxationAddress();
  void setTaxationAddress(String address);
  String getPayerPONumber();
  void setPayerPONumber(String number);
  String getProductCode();
  void setProductCode(String code);
  String getProductName();
  void setProductName(String name);
  String getSellerOfRecord();
  void setSellerOfRecord(String seller);
  String getUsageType();
  void setUsageType(String usageType);
  String getOperation();
  void setOperation(String operation);
  String getRateId();
  void setRateId(String id);
  String getItemDescription();
  void setItemDescription(String description);
  Date getUsageStartDate();
  void setUsageStartDate(Date startDate);
  Date getUsageEndDate();
  void setUsageEndDate(Date endDate);
  Double getUsageQuantity();
  void setUsageQuantity(Double quantity);
  String getBlendedRate();
  void setBlendedRate(String rate);
  String getCurrencyCode();
  void setCurrencyCode(String code);
  Double getCostBeforeTax();
  void setCostBeforeTax(Double cost);
  Double getCredits();
  void setCredits(Double credits);
  Double getTaxAmount();
  void setTaxAmount(Double amount);
  String getTaxType();
  void setTaxType(String type);
  Double getTotalCost();
  void setTotalCost(Double cost);
}
