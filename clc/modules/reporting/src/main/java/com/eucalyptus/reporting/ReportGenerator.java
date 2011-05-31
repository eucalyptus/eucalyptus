package com.eucalyptus.reporting;

import java.io.File;
import java.io.OutputStream;
import java.util.*;

import org.apache.log4j.Logger;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import com.eucalyptus.reporting.instance.*;
import com.eucalyptus.reporting.s3.S3ReportLine;
import com.eucalyptus.reporting.s3.S3ReportLineGenerator;
import com.eucalyptus.reporting.storage.StorageReportLine;
import com.eucalyptus.reporting.storage.StorageReportLineGenerator;
import com.eucalyptus.reporting.units.Units;
import com.eucalyptus.system.SubDirectory;

public class ReportGenerator
{
	private static Logger log = Logger.getLogger( ReportGenerator.class );
	
	/**
	 * <p>Generates a report and sends it to an OutputStream
	 * 
	 * @param groupByCriterion Can be null if none selected
	 */
	public static void generateReport(ReportType reportType, ReportFormat format,
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

				
		JRDataSource dataSource = null;
		switch (reportType) {
			case INSTANCE:
				List<InstanceReportLine> irl =
					InstanceReportLineGenerator.getInstance()
						.getReportLines(period, groupByCriterion, criterion,
							displayUnits);
				dataSource = new JRBeanCollectionDataSource(irl);
				break;
			case STORAGE:
				List<StorageReportLine> srl =
					StorageReportLineGenerator.getInstance()
						.getReportLines(period, groupByCriterion, criterion,
							displayUnits);
				dataSource = new JRBeanCollectionDataSource(srl);
				break;
			case S3:
				List<S3ReportLine> s3rl =
					S3ReportLineGenerator.getInstance()
						.getReportLines(period, groupByCriterion, criterion,
							displayUnits);
				dataSource = new JRBeanCollectionDataSource(s3rl);
				break;
		}

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
}
