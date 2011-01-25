package com.eucalyptus.reporting.generator;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.*;

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
		
		PDF  (new JRPdfExporter(), null, null),
		CSV  (new JRCsvExporter(), null, null),
		HTML (new JRHtmlExporter(),
				JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN,
				new Boolean(false));
		
		private final JRAbstractExporter exporter;
		private Format(JRAbstractExporter exporter, JRExporterParameter param,
				Object val)
		{
			this.exporter = exporter;
			if (param != null) exporter.setParameter(param, val);
		}
		
		public JRAbstractExporter getExporter()
		{
			return this.exporter;
		}

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
		System.out.println("--> Items size:" + items.size());
		generateReport(new File(baseDir, STORAGE_REPORT_FILENAME), format,
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
		generateReport(new File(baseDir, NESTED_STORAGE_REPORT_FILENAME), format,
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
		generateReport(new File(baseDir, NESTED_INSTANCE_REPORT_FILENAME), format,
				groupBy.toString(), criterion.toString(), items, dest);
	}

	private static void generateReport(File jrxmlFile, Format format,
			String groupBy, String criterion, Collection items,
			OutputStream dest)
	{
		Map params = new HashMap();
		params.put("criterion", criterion.toString());
		if (groupBy != null) {
			params.put("groupByCriterion", groupBy.toString());
		}
		JRDataSource dataSource = new JRBeanCollectionDataSource(items);
		try {
			JasperReport report =
				JasperCompileManager.compileReport(jrxmlFile.getAbsolutePath());
			JasperPrint print =
				JasperFillManager.fillReport(report, params, dataSource);
			JRAbstractExporter exporter = format.getExporter();
			exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
			exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, dest);
			exporter.exportReport();
		} catch (JRException e) {
			log.error(e.getMessage(), e);
			e.printStackTrace();
		}
		
	}

}
