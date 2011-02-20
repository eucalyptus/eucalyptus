package com.eucalyptus.reporting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import org.apache.log4j.Logger;
import com.eucalyptus.reporting.instance.InstanceDisplayBean;
import com.eucalyptus.reporting.instance.InstanceDisplayDb;
import com.eucalyptus.reporting.storage.StorageDisplayBean;
import com.eucalyptus.reporting.storage.StorageDisplayDb;
import com.eucalyptus.reporting.units.Units;

public class GenerateReport
{
	public static enum Format
	{	
		PDF  (new JRPdfExporter(), "pdf", null, null),
		CSV  (new JRCsvExporter(), "csv", null, null),
		HTML (new JRHtmlExporter(), "html",
				JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN,
				new Boolean(false));
		
		private final JRAbstractExporter exporter;
		private final String extension;

		private Format(JRAbstractExporter exporter, String extension,
				JRExporterParameter param, Object val)
		{
			this.extension = extension;
			this.exporter = exporter;
			if (param != null) exporter.setParameter(param, val);
		}
		
		public JRAbstractExporter getExporter()
		{
			return this.exporter;
		}
		
		public String getFileExtension()
		{
			return this.extension;
		}

	}
	
	private static File baseDir = new File("/tmp/");
	private static File storageReportFile = new File(baseDir, "storage.jrxml");
	private static File nestedStorageReportFile = new File(baseDir, "nested_storage.jrxml");
	private static File instanceReportFile = new File(baseDir, "instance.jrxml");
	private static File nestedInstanceReportFile = new File(baseDir, "nested_instance.jrxml");
	private static Format format = Format.PDF;
	private static File destFile = new File(baseDir,
							"report." + format.getFileExtension());
	private static Period period = new Period(0, Long.MAX_VALUE);
	
	private static Logger log = Logger.getLogger( GenerateReport.class );

	public static void generateInstanceReportOneCriterion(String criterion)
	{
		GroupByCriterion crit = getCriterion(criterion);
		System.out.println(" ----> GENERATING INSTANCE REPORT WITH " + crit);

		InstanceDisplayDb db = InstanceDisplayDb.getInstance();
		OutputStream dest = null;
		try {
			dest = new FileOutputStream(destFile);
			List<InstanceDisplayBean> list =
				db.search(period, crit, Units.DEFAULT_DISPLAY_UNITS);
			generateReport(instanceReportFile, format,
					Units.DEFAULT_DISPLAY_UNITS, null, crit, list, dest);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (dest != null) {
				try {
					dest.close();
				} catch (IOException iox) {
					iox.printStackTrace();
				}
			}
		}
	}

	public static void generateInstanceReportTwoCriteria(String groupBy,
			String criterion)
	{
		GroupByCriterion crit = getCriterion(criterion);
		GroupByCriterion groupCrit = getCriterion(groupBy);
		System.out.println(" ----> GENERATING INSTANCE REPORT WITH " + crit
				+ "," + groupCrit); 

		InstanceDisplayDb db = InstanceDisplayDb.getInstance();
		OutputStream dest = null;
		try {
			dest = new FileOutputStream(destFile);
			List<InstanceDisplayBean> displayItems =
				db.searchGroupBy(period, groupCrit, crit, Units.DEFAULT_DISPLAY_UNITS);
			generateReport(nestedInstanceReportFile, format,
					Units.DEFAULT_DISPLAY_UNITS, groupCrit, crit, displayItems,
					dest);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (dest != null) {
				try {
					dest.close();
				} catch (IOException iox) {
					iox.printStackTrace();
				}
			}
		}
	}

	public static void generateStorageReportOneCriterion(String criterion)
	{
		GroupByCriterion crit = getCriterion(criterion);
		System.out.println(" ----> GENERATING STORAGE REPORT WITH " + crit);

		StorageDisplayDb db = StorageDisplayDb.getInstance();
		OutputStream dest = null;
		try {
			dest = new FileOutputStream(destFile);
			List<StorageDisplayBean> list = db.search(period, crit,
					Units.DEFAULT_DISPLAY_UNITS);
			generateReport(storageReportFile, format,
					Units.DEFAULT_DISPLAY_UNITS, null, crit, list, dest);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (dest != null) {
				try {
					dest.close();
				} catch (IOException iox) {
					iox.printStackTrace();
				}
			}
		}
	}

	public static void generateStorageReportTwoCriteria(String groupBy,
			String criterion)
	{
		GroupByCriterion crit = getCriterion(criterion);
		GroupByCriterion groupCrit = getCriterion(groupBy);
		System.out.println(" ----> GENERATING STORAGE REPORT WITH " + crit
				+ "," + groupCrit); 

		StorageDisplayDb db = StorageDisplayDb.getInstance();
		OutputStream dest = null;
		try {
			dest = new FileOutputStream(destFile);
			List<StorageDisplayBean> displayItems = db.searchGroupBy(period,
					groupCrit, crit, Units.DEFAULT_DISPLAY_UNITS);
			generateReport(nestedStorageReportFile, format,
					Units.DEFAULT_DISPLAY_UNITS, groupCrit, crit, displayItems,
					dest);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (dest != null) {
				try {
					dest.close();
				} catch (IOException iox) {
					iox.printStackTrace();
				}
			}
		}

	}

	private static GroupByCriterion getCriterion(String name)
	{
		/* throws an IllegalArgument which we allow to percolate up
		 */
		return GroupByCriterion.valueOf(name.toUpperCase());
	}
	
	private static void generateReport(File jrxmlFile, Format format,
			Units units, GroupByCriterion groupBy, GroupByCriterion criterion,
			Collection items, OutputStream dest)
	{
		Map params = new HashMap();
		params.put("criterion", criterion.toString());
		if (groupBy != null) {
			params.put("groupByCriterion", groupBy.toString());
		}
		params.put("timeUnit", units.getTimeUnit().toString());
		params.put("sizeUnit", units.getSizeUnit().toString());
		params.put("sizeTimeTimeUnit", units.getSizeTimeTimeUnit().toString());
		params.put("sizeTimeSizeUnit", units.getSizeTimeSizeUnit().toString());
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
