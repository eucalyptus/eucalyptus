package com.eucalyptus.reporting.generator;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import com.eucalyptus.reporting.*;
import com.eucalyptus.reporting.instance.*;
import com.eucalyptus.reporting.storage.*;

public class ReportGenerator
{
	private static Logger log = Logger.getLogger( ReportGenerator.class );

	public static String STORAGE_REPORT_FILENAME         = "storage.jrxml";
	public static String NESTED_STORAGE_REPORT_FILENAME  = "nested_storage.jrxml";
	public static String INSTANCE_REPORT_FILENAME        = "instance.jrxml";
	public static String NESTED_INSTANCE_REPORT_FILENAME = "nested_instance.jrxml";

	public static enum Format {
		PDF, HTML, XLS, CSV;
	}
	
	private final File baseDir;
	
	public ReportGenerator(File baseDir)
	{
		this.baseDir = baseDir;
	}

	public void generateStorageReport(Period period, Format format,
			GroupByCriterion criterion, OutputStream dest)
	{
		StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();
		Map<String, StorageUsageSummary> summaryMap = usageLog.scanSummarize(period, criterion);
		List items = new ArrayList(summaryMap.size());
		for (String key: summaryMap.keySet()) {
			items.add(new StorageReportLine(key, null, summaryMap.get(key)));
		}
		generateReport(new File(baseDir, INSTANCE_REPORT_FILENAME), format,
				null, criterion.toString(), items, dest);
	}
	
	public void generateNestedStorageReport(Period period, Format format,
			GroupByCriterion groupBy, GroupByCriterion criterion,
			OutputStream dest)
	{
		StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();
		Map<String, Map<String, StorageUsageSummary>> summaryMap =
			usageLog.scanSummarize(period, groupBy, criterion);
		List items = new ArrayList(summaryMap.size());
		for (String outerKey: summaryMap.keySet()) {
			Map<String, StorageUsageSummary> innerMap =
				summaryMap.get(outerKey);
			for (String innerKey: innerMap.keySet()) {
				items.add(new StorageReportLine(innerKey, outerKey,
						innerMap.get(innerKey)));
			}
		}
		generateReport(new File(baseDir, INSTANCE_REPORT_FILENAME), format,
				groupBy.toString(), criterion.toString(), items, dest);
	}
	
	public void generateInstanceReport(Period period, Format format,
			GroupByCriterion criterion, OutputStream dest)
	{
		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		Map<String, InstanceUsageSummary> summaryMap = usageLog.scanSummarize(period, criterion);
		List items = new ArrayList(summaryMap.size());
		for (String key: summaryMap.keySet()) {
			items.add(new InstanceReportLine(key, null, summaryMap.get(key)));
		}
		generateReport(new File(baseDir, INSTANCE_REPORT_FILENAME), format,
				null, criterion.toString(), items, dest);
	}
	
	public void generateNestedInstanceReport(Period period, Format format,
			GroupByCriterion groupBy, GroupByCriterion criterion,
			OutputStream dest)
	{
		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		Map<String, Map<String, InstanceUsageSummary>> summaryMap =
			usageLog.scanSummarize(period, groupBy, criterion);
		List items = new ArrayList(summaryMap.size());
		for (String outerKey: summaryMap.keySet()) {
			Map<String, InstanceUsageSummary> innerMap =
				summaryMap.get(outerKey);
			for (String innerKey: innerMap.keySet()) {
				items.add(new InstanceReportLine(innerKey, outerKey,
						innerMap.get(innerKey)));
			}
		}
		generateReport(new File(baseDir, INSTANCE_REPORT_FILENAME), format,
				groupBy.toString(), criterion.toString(), items, dest);
	}

	private static void generateReport(File jrxmlFile, Format format,
			String groupBy, String criterion, Collection items,
			OutputStream dest)
	{
		Map params = new HashMap();
		params.put("criterion", criterion.toString());
		if (groupBy != null) {
			params.put("groupBy", groupBy.toString());
		}
		JRDataSource dataSource = new JRBeanCollectionDataSource(items);
		try {
			JasperReport report =
				JasperCompileManager.compileReport(jrxmlFile.getAbsolutePath());
			JasperFillManager.fillReportToStream(report, dest, params, dataSource);	
		} catch (JRException e) {
			log.error(e.getMessage(), e);
		}
		
	}

}
