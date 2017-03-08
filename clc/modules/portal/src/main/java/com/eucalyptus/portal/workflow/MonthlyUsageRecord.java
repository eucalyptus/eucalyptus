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
