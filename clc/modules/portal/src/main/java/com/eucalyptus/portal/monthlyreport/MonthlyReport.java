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
