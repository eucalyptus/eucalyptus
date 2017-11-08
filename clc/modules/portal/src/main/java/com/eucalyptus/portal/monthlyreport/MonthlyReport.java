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

import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.portal.common.PortalMetadata;
import com.google.common.collect.Lists;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import java.util.Collection;
import java.util.List;

@Entity
@PersistenceContext( name = "eucalyptus_billing" )
@Table( name = "billing_monthly_report" )
public class MonthlyReport  extends AbstractOwnedPersistent implements PortalMetadata.BillingReportMetadata {
  private static final long serialVersionUID = 1L;
  public MonthlyReport() { }

  public MonthlyReport(final String year, final String month) {
    this.year = year;
    this.month = month;
  }

  public MonthlyReport(final String year, final String month, final List<MonthlyReportEntry> entries) {
    this.year = year;
    this.month = month;
    this.entries = entries;
  }

  @Column( name = "year" )
  private String year;

  public void setYear(final String year) { this.year = year; }
  public String getYear() { return this.year; }

  @Column( name = "month" )
  private String month;

  public void setMonth(final String month) { this.month = month; }
  public String getMonth() { return this.month; }

  @ElementCollection (fetch = FetchType.LAZY)
  @CollectionTable( name = "billing_monthly_report_entry",
          joinColumns = @JoinColumn( name = "monthlyreport_id", referencedColumnName = "id"))
  private Collection<MonthlyReportEntry> entries = null;

  public List<MonthlyReportEntry> getEntries() {
    return Lists.newArrayList(this.entries);
  }

  void addEntry(final MonthlyReportEntry entry) {
    if(this.entries==null)
      this.entries = Lists.newArrayList();
    this.removeEntry(entry);
    this.entries.add(entry);
  }

  void removeEntry(final MonthlyReportEntry entry) {
    if(this.entries==null)
      return;
    this.entries.remove(entry);
  }
}
