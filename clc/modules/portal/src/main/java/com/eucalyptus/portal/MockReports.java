/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal;

import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.portal.common.model.ViewInstanceUsageReportType;
import com.eucalyptus.portal.common.model.ViewInstanceUsageResult;
import com.eucalyptus.portal.common.model.ViewMonthlyUsageResult;
import com.eucalyptus.portal.common.model.ViewMonthlyUsageType;
import com.eucalyptus.portal.common.model.ViewUsageResponseType;
import com.eucalyptus.portal.common.model.ViewUsageType;
import com.eucalyptus.system.BaseDirectory;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MockReports {
  private static final Logger LOG = Logger.getLogger( MockReports.class );

  private static MockReports instance = new MockReports();
  private MockReports() { }
  public static MockReports getInstance() {
    return instance;
  }

  private static final String dir = String.format("%s/billing-sample-reports", BaseDirectory.CONF);
  private static final String MonthlyReportFile = String.format("%s/monthly_report.csv", dir);

  public ViewMonthlyUsageResult generateMonthlyReport(ViewMonthlyUsageType request) {
    final ViewMonthlyUsageResult result = new ViewMonthlyUsageResult();
    final SampleReportReader reader = new SampleReportReader(MonthlyReportFile);
    final StringBuilder sb = new StringBuilder();
    try {
      sb.append(reader.getHeaderLine()+"\n");
      final MonthlyUsageReports reports = new MonthlyUsageReports(request);
      sb.append(reader.readLines().stream()
              .map(reports.fromHashableLine)
              .map(reports.substitutor)
              .map(data -> data.toString())
              .reduce( (l1, l2) -> String.format("%s\n%s", l1, l2) )
              .get()
      );
      result.setData(sb.toString());
    } catch (final Exception ex) {
      LOG.error("Failed to generate sample monthly report", ex);
    }
    return result;
  }

  public ViewUsageResponseType generateAwsUsageReport(ViewUsageType request) {

    return null;
  }

  public ViewInstanceUsageResult generateInstanceUsageReport(ViewInstanceUsageReportType request) {


    return null;
  }


  public static class SampleReportReader {
    private Map<String, Integer> header = Maps.newHashMap();
    private String sampleFilePath = null;
    public SampleReportReader(final String sampleFilePath) {
      this.sampleFilePath = sampleFilePath;
    }

    public List<HashableLine> readLines() throws Exception {
      if (header == null || header.size() <= 0) {
        buildHeader();
        if (header == null || header.size() <= 0) {
          new Exception("Failed to parse header from the file");
        }
      }
      final BufferedReader reader = new BufferedReader(new FileReader(this.sampleFilePath));
      return reader.lines().skip(1)
              .map(s -> new HashableLine(header, s))
              .collect(Collectors.toList());
    }

    private void buildHeader() throws Exception {
      final String header = getHeaderLine();
      final String[] headerTokens = header.split(",");
      for (int i = 0; i< headerTokens.length; i++ ) {
        final String unquoted = headerTokens[i].replaceAll("\"", "");
        this.header.put(unquoted, i);
      }
    }

    public String getHeaderLine() throws Exception {
      final BufferedReader reader = new BufferedReader(new FileReader(this.sampleFilePath));

      final Optional<String> header = reader.lines().findFirst();
      if (!header.isPresent())
        throw new Exception("Header line is not found");
      return header.get();
    }
  }

  public static class HashableLine {
    private String line = null;
    private Map<String, Integer> header = null;
    private String[] tokens = null;
    public HashableLine(final Map<String, Integer> header, final String line) {
      this.header = header;
      this.line = line;
      this.tokens = this.line.split("\",\"");
    }

    public String getValue(final String fieldName) throws ParseException {
      if (this.tokens.length != this.header.keySet().size()) {
        throw new ParseException("Number of tokens doesn't match the header", -1);
      }
      if (!this.header.keySet().contains(fieldName)) {
        throw new ParseException("No such field is found in header", -1);
      }
      if (this.header.get(fieldName) < 0 || this.header.get(fieldName) >= this.tokens.length) {
        throw new ParseException("Invalid field index in header", this.header.get(fieldName));
      }

      final String token = this.tokens[this.header.get(fieldName)];
      final String unquoted = token.replaceAll("\"", "");
      return unquoted;
    }

    public Set<String> getHeaders() {
      return this.header.keySet();
    }

    @Override
    public String toString() {
      return this.line;
    }
  }

  public static class MonthlyUsageReports {
    private String accountId = null;
    private String accountName = null;
    private int year = 2016;
    private int month = 12;
    public MonthlyUsageReports(final ViewMonthlyUsageType request) {
      try {
        final Context ctx = Contexts.lookup(request.getCorrelationId());
        this.accountId = ctx.getAccountNumber();
        this.accountName = ctx.getAccountAlias();
      }catch(final Exception ex) {
        this.accountId = "0000000000";
        this.accountName = "Eucalyptus";
      }
      this.year = Integer.parseInt(request.getYear());
      this.month = Integer.parseInt(request.getMonth());
    }

    public Function<HashableLine, MonthlyUsageReportData> fromHashableLine = (line) -> {
      final MonthlyUsageReportData data = new MonthlyUsageReportData();
      try {
        for( final Field field : data.getClass().getFields() ) {
          String fieldName = field.getName();
          if (fieldName == null) {
            continue;
          } else if (fieldName.length() > 1) {
            fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
          } else if (fieldName.length() == 1) {
            fieldName = fieldName.toUpperCase();
          }

          if (line.getHeaders().contains(fieldName)) {
            final String strValue = line.getValue(fieldName);
            if (String.class.equals(field.getType())) {
              field.set(data, strValue);
            } else if (Date.class.equals(field.getType()) && strValue.length() > 0) {
              // 2016/12/03 10:34:18
              final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
              final Date dt = df.parse(strValue);
              field.set(data, dt);
            } else if (Double.class.equals(field.getType()) && strValue.length() > 0) {
              Double dv = Double.parseDouble(strValue);
              field.set(data, dv);
            }
          }
        }
      } catch (final ParseException ex) {
        LOG.error("Failed to parse mock data", ex);
        return null;
      } catch (final IllegalAccessException ex) {
        LOG.error("Failed to parse mock data", ex);
        return null;
      }
      return data;
    };

    public Function<MonthlyUsageReportData, MonthlyUsageReportData> substitutor = (data) -> {
      data.payerAccountId = this.accountId;
      data.payerAccountName = this.accountName;
      data.taxationAddress = "6750 Navigator Way Suite 200, Goleta, CA 93117";

      if (data.billingPeriodStartDate != null) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(data.billingPeriodStartDate);
        cal.set(Calendar.YEAR, this.year);
        cal.set(Calendar.MONTH, this.month - 1 );
        data.billingPeriodStartDate = cal.getTime();
      }
      if (data.billingPeriodEndDate != null) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(data.billingPeriodEndDate);
        cal.set(Calendar.YEAR, this.year);
        cal.set(Calendar.MONTH, this.month - 1 );
        data.billingPeriodEndDate = cal.getTime();
      }

      if (data.invoiceDate != null) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(data.invoiceDate);
        cal.set(Calendar.YEAR, this.year);
        cal.set(Calendar.MONTH, this.month - 1 );
        data.invoiceDate = cal.getTime();
      }

      if (data.usageStartDate != null) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(data.usageStartDate);
        cal.set(Calendar.YEAR, this.year);
        cal.set(Calendar.MONTH, this.month - 1 );
        data.usageStartDate = cal.getTime();
      }

      if (data.usageEndDate != null) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(data.usageEndDate);
        cal.set(Calendar.YEAR, this.year);
        cal.set(Calendar.MONTH, this.month - 1);
        data.usageEndDate = cal.getTime();
      }
      return data;
    };
  }
}
