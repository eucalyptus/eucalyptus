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
package com.eucalyptus.portal

import java.text.DateFormat
import java.text.SimpleDateFormat

class UsageReportData {
}

class MonthlyUsageReportData extends UsageReportData {
    public String invoiceID;
    public String payerAccountId;
    public String linkedAccountId;
    public String recordType;
    public String recordID;
    public Date billingPeriodStartDate;
    public Date billingPeriodEndDate;
    public Date invoiceDate;
    public String payerAccountName;
    public String linkedAccountName;
    public String taxationAddress;
    public String payerPONumber;
    public String productCode;
    public String productName
    public String sellerOfRecord
    public String usageType
    public String operation;
    public String rateId;
    public String itemDescription;
    public Date usageStartDate;
    public Date usageEndDate
    public String usageQuantity;
    public String blendedRate
    public String currencyCode;
    public String costBeforeTax;
    public String credits;
    public String taxAmount;
    public String taxType;
    public String totalCost;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        sb.append(invoiceID!=null ? "\""+invoiceID+"\"," : "\"\",");
        sb.append(payerAccountId!=null ? "\""+payerAccountId+"\"," : "\"\",");
        sb.append(linkedAccountId!=null ? "\""+linkedAccountId+"\"," : "\"\",");
        sb.append(recordType!=null ? "\""+recordType+"\"," : "\"\",");
        sb.append(recordID!=null ? "\""+recordID+"\"," : "\"\",");
        sb.append(billingPeriodStartDate!=null ? "\""+df.format(billingPeriodStartDate)+"\"," : "\"\",");
        sb.append(billingPeriodEndDate!=null ? "\""+df.format(billingPeriodEndDate)+"\"," : "\"\",");
        sb.append(invoiceDate!=null ? "\""+df.format(invoiceDate)+"\"," : "\"\",");
        sb.append(payerAccountName!=null ? "\""+payerAccountName+"\"," : "\"\",");
        sb.append(linkedAccountName!=null ? "\""+linkedAccountName+"\"," : "\"\",");
        sb.append(taxationAddress!=null ? "\""+taxationAddress+"\"," : "\"\",");
        sb.append(payerPONumber!=null ? "\""+payerPONumber+"\"," : "\"\",");
        sb.append(productCode!=null ? "\""+productCode+"\"," : "\"\",");
        sb.append(productName!=null ? "\""+productName+"\"," : "\"\",");
        sb.append(sellerOfRecord!=null ? "\""+sellerOfRecord+"\"," : "\"\",");
        sb.append(usageType!=null ? "\""+usageType+"\"," : "\"\",");
        sb.append(operation!=null ? "\""+operation+"\"," : "\"\",");
        sb.append(rateId!=null ? "\""+rateId+"\"," : "\"\",");
        sb.append(itemDescription!=null ? "\""+itemDescription+"\"," : "\"\",");
        sb.append(usageStartDate!=null ? "\""+df.format(usageStartDate)+"\"," : "\"\",");
        sb.append(usageEndDate!=null ? "\""+df.format(usageEndDate)+"\"," : "\"\",");
        sb.append(usageQuantity!=null ? "\""+usageQuantity+"\"," : "\"\",");
        sb.append(blendedRate!=null ? "\""+blendedRate+"\"," : "\"\",");
        sb.append(currencyCode!=null ? "\""+currencyCode+"\"," : "\"\",");
        sb.append(costBeforeTax!=null ? "\""+costBeforeTax+"\"," : "\"\",");
        sb.append(credits!=null ? "\""+credits+"\"," : "\"\",");
        sb.append(taxAmount!=null ? "\""+taxAmount+"\"," : "\"\",");
        sb.append(taxType!=null ? "\""+taxType+"\"," : "\"\",");
        sb.append(totalCost!=null ? "\""+totalCost+"\"," : "\"\",");
        return sb.toString();
    }
}

class AwsUsageReportData extends UsageReportData {
    public String service;
    public String operation;
    public String usageType;
    public String resource;
    public Date startTime;
    public Date endTime;
    public String usageValue;
}


class InstanceUsageReportData extends UsageReportData {
    public String startTime;
    public String endTime;
    public List<Integer> values = new ArrayList<Integer>();
}