package com.eucalyptus.reporting;

import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.log4j.Logger;

import com.eucalyptus.reporting.instance.InstanceReportLineGenerator;
import com.eucalyptus.reporting.s3.S3ReportLineGenerator;
import com.eucalyptus.reporting.storage.StorageReportLineGenerator;
import com.eucalyptus.reporting.units.Units;
import com.eucalyptus.system.SubDirectory;

/**
 * <p>ReportGenerator is the main class by which the reporting system is
 * accessed by outside modules. It acts as a facade for the various reporting
 * sub-packages (instance, storage, and s3 sub-packages).
 * 
 * <p>ReportGenerator contains all the jasper-related stuff.
 * 
 * @author tom.werges
 */
public class ReportGenerator
{
	private static Logger log = Logger.getLogger( ReportGenerator.class );

	private static final int DEFAULT_CACHE_SIZE = 5;
	private final Map<ReportKey, List<ReportLine>> lineListMap;
	
	private static ReportGenerator instance;

	private ReportGenerator()
	{
		this.lineListMap = new HashMap<ReportKey, List<ReportLine>>();
	}
	
	public static ReportGenerator getInstance()
	{
		if (instance == null) {
			instance = new ReportGenerator();
		}
		return instance;
	}
	
	/**
	 * <p>Generates a report and sends it to an OutputStream.
	 * 
	 * @param groupByCriterion Can be null if none selected
	 */
	public void generateReport(ReportType reportType, ReportFormat format,
			Period period, ReportingCriterion criterion,
			ReportingCriterion groupByCriterion, Units displayUnits,
			OutputStream out)
	{
		if (reportType == null)
			throw new IllegalArgumentException("ReportType can't be null");
		if (criterion == null)
			throw new IllegalArgumentException("Criterion can't be null");
		if (displayUnits == null)
			displayUnits = Units.DEFAULT_DISPLAY_UNITS;
		
		
		final Map<String, String> params = new HashMap<String, String>();
		params.put("criterion", criterion.toString());
		params.put("timeUnit", displayUnits.getTimeUnit().toString());
		params.put("sizeUnit", displayUnits.getSizeUnit().toString());
		params.put("sizeTimeTimeUnit",
				displayUnits.getSizeTimeTimeUnit().toString());
		params.put("sizeTimeSizeUnit",
				displayUnits.getSizeTimeSizeUnit().toString());
		if (groupByCriterion != null) {
			params.put("groupByCriterion", groupByCriterion.toString());			
		}


		final String filename = (groupByCriterion==null)
								? reportType.getJrxmlFilename()
								: reportType.getNestedJrxmlFilename();
		final File jrxmlFile = 	new File(SubDirectory.REPORTS.toString()
				+ File.separator + filename);

			
		@SuppressWarnings("rawtypes")
		ReportLineGenerator generator = null;
		switch (reportType) {
			case INSTANCE:
				generator =	InstanceReportLineGenerator.getInstance();
				break;
			case STORAGE:
				generator =	StorageReportLineGenerator.getInstance();
				break;
			case S3:
				generator =	S3ReportLineGenerator.getInstance();
				break;
		}

		
		/* We maintain a small cache here of very recently-viewed reports. If
		 * not found in cache then get report lines from {s3,storage,instance}
		 * API
		 */
		List<ReportLine> reportLines = null;
		ReportKey key = new ReportKey(reportType, period, criterion,
				groupByCriterion, displayUnits, System.currentTimeMillis());
		if (lineListMap.containsKey(key)) {
			log.info("Gathered report data from cache:" + key);
			reportLines = lineListMap.get(key);
		} else {
			while (lineListMap.size() >= DEFAULT_CACHE_SIZE) {
				ReportKey oldestKey = null;
				for (ReportKey oldKey: lineListMap.keySet()) {
					if (oldestKey==null || oldestKey.getTimestampMs() > oldKey.getTimestampMs()) {
						oldestKey = oldKey;
					}
				}
				if (oldestKey != null) {
					lineListMap.remove(oldestKey);
					log.info("Removed report data from cache:" + oldestKey);
				}
			}
			reportLines = generator.getReportLines(period, groupByCriterion,
					criterion, displayUnits);
			log.info("Generated report data from db:" + key);
			lineListMap.put(key, reportLines);
			
		}
		JRDataSource dataSource = new JRBeanCollectionDataSource(reportLines);

		
		try {
			JasperReport report = JasperCompileManager.compileReport(jrxmlFile
					.getAbsolutePath());
			JasperPrint jasperPrint = JasperFillManager.fillReport(report,
					params, dataSource);

			JRExporter exporter = format.getExporter();
			exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, out);
			exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
			exporter.exportReport();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private class ReportKey
	{
		private final ReportType type;
		private final Period period;
		private final ReportingCriterion groupByCriterion;
		private final ReportingCriterion criterion;
		private final Units units;
		private final long timestampMs;

		public ReportKey(ReportType type, Period period,
				ReportingCriterion criterion,
				ReportingCriterion groupByCriterion, Units units,
				long timestampMs)
		{
			super();
			this.type = type;
			this.period = period;
			this.groupByCriterion = groupByCriterion;
			this.criterion = criterion;
			this.units = units;
			this.timestampMs = timestampMs;
		}

		public long getTimestampMs() {
			return timestampMs;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((criterion == null) ? 0 : criterion.hashCode());
			result = prime
					* result
					+ ((groupByCriterion == null) ? 0 : groupByCriterion
							.hashCode());
			result = prime * result
					+ ((period == null) ? 0 : period.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + ((units == null) ? 0 : units.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReportKey other = (ReportKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (criterion != other.criterion)
				return false;
			if (groupByCriterion != other.groupByCriterion)
				return false;
			if (period == null) {
				if (other.period != null)
					return false;
			} else if (!period.equals(other.period))
				return false;
			if (type != other.type)
				return false;
			if (units == null) {
				if (other.units != null)
					return false;
			} else if (!units.equals(other.units))
				return false;
			return true;
		}

		private ReportGenerator getOuterType() {
			return ReportGenerator.this;
		}

		public String toString()
		{
			return String.format("[type:%s period:%s crit:%s groupBy:%s timestamp:%d]",
					type, period, criterion, groupByCriterion, timestampMs);
		}
		
	}
}
