package com.eucalyptus.www;

import java.io.*;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.reporting.ReportingCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.instance.InstanceReportLine;
import com.eucalyptus.reporting.instance.InstanceReportLineGenerator;
import com.eucalyptus.reporting.s3.S3ReportLine;
import com.eucalyptus.reporting.s3.S3ReportLineGenerator;
import com.eucalyptus.reporting.storage.StorageReportLine;
import com.eucalyptus.reporting.storage.StorageReportLineGenerator;
import com.eucalyptus.reporting.units.Units;
import com.eucalyptus.system.SubDirectory;
import com.google.gwt.user.client.rpc.SerializableException;

import edu.ucsb.eucalyptus.admin.server.EucalyptusWebBackendImpl;
import edu.ucsb.eucalyptus.admin.server.SessionInfo;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.*;

public class ReportServlet
	extends HttpServlet
{
	  private static Logger LOG = Logger.getLogger( Reports.class );
	  
	  private static String STORAGE_REPORT_FILENAME         = "storage.jrxml";
	  private static String NESTED_STORAGE_REPORT_FILENAME  = "nested_storage.jrxml";
	  private static String INSTANCE_REPORT_FILENAME        = "instance.jrxml";
	  private static String NESTED_INSTANCE_REPORT_FILENAME = "nested_instance.jrxml";
	  private static String S3_REPORT_FILENAME              = "s3.jrxml";
	  private static String NESTED_S3_REPORT_FILENAME       = "nested_s3.jrxml";


	  enum Param {
		name, type, session, start(false), end(false),
		criterionId(false),	groupById(false);

		private String value = null;
		private Boolean required = Boolean.TRUE;

		public Boolean isRequired()
		{
			return this.required;
		}

		private Param(String value, Boolean required)
		{
			this.value = value;
			this.required = required;
		}

		private Param()
		{
		}

		private Param(Boolean required)
		{
			this.required = required;
		}

		public String get() throws NoSuchFieldException
		{
			if (this.value == null) {
				throw new NoSuchFieldException();
			} else {
				return this.value;
			}
		}

		public String get(HttpServletRequest req)
				throws IllegalArgumentException
		{
			if (req.getParameter(this.name()) == null) {
				throw new IllegalArgumentException("'" + this.name()
						+ "' is a required argument.");
			} else {
				this.value = req.getParameter(this.name());
				LOG.debug("Found parameter: " + this.name() + "=" + this.value);
				return this.value;
			}
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
	{
		for (Param p : Param.values()) {
			try {
				p.get(req);
			} catch (IllegalArgumentException e) {
				if (p.isRequired()) {
					LOG.debug(e, e);
					throw new RuntimeException(e);
				}
			}
		}

		try {
			this.verifySession(Param.session.get());
		} catch (NoSuchFieldException e) {
			LOG.debug(e, e);
			throw new RuntimeException(e);
		}
		for (Param p : Param.values()) {
			try {
				LOG.debug(String.format("REPORT: %10.10s=%s", p.name(), p.get()));
			} catch (NoSuchFieldException e1) {
				LOG.debug(String.format("REPORT: %10.10s=%s", p.name(),
						e1.getMessage()));
			}
		}
		this.exportReport(req, res);
	  
	  }

	
	private void exportReport(HttpServletRequest req, HttpServletResponse res)
	{
		try {
			Type reportType = Type.valueOf(Param.type.get());
			try {

				final JRExporter exporter = reportType.getReportExporter(req, res,
						Param.name.get(req));
				LOG.info("--> scriptName:" + Param.name.get(req));

				String scriptName = Param.name.get(req);
				JasperPrint jasperPrint = null;

				long start = Long.parseLong(Param.start.get(req));
				long end = Long.parseLong(Param.end.get(req));
				Period period = new Period(start, end);
				int criterionId = Integer.parseInt(Param.criterionId.get(req));
				int groupById = Integer.parseInt(Param.groupById.get(req));
				// TODO: explain magic num
				ReportingCriterion criterion = ReportingCriterion.values()[criterionId + 1];
				Units displayUnits = Units.DEFAULT_DISPLAY_UNITS;

				Map<String, String> params = new HashMap<String, String>();
				params.put("criterion", criterion.toString());
				params.put("timeUnit", displayUnits.getTimeUnit().toString());
				params.put("sizeUnit", displayUnits.getSizeUnit().toString());
				params.put("sizeTimeTimeUnit", displayUnits.getSizeTimeTimeUnit().toString());
				params.put("sizeTimeSizeUnit", displayUnits.getSizeTimeSizeUnit().toString());

				ReportingCriterion groupByCriterion = null;
				if (groupById > 0) {
					groupByCriterion = ReportingCriterion.values()[groupById - 1];
					params.put("groupByCriterion", groupByCriterion.toString());
				}

				if (scriptName.equals("user_vms")) {

					InstanceReportLineGenerator generator = InstanceReportLineGenerator
							.getInstance();
					File jrxmlFile = null;
					JRDataSource dataSource = null;
					if (groupById == 0) {
						List<InstanceReportLine> list = generator
								.getReportLines(period, criterion, displayUnits);
						dataSource = new JRBeanCollectionDataSource(list);
						jrxmlFile = new File(SubDirectory.REPORTS.toString()
								+ File.separator + INSTANCE_REPORT_FILENAME);
					} else {
						List<InstanceReportLine> list = generator
								.getReportLines(period, groupByCriterion,
										criterion, displayUnits);
						dataSource = new JRBeanCollectionDataSource(list);
						jrxmlFile = new File(SubDirectory.REPORTS.toString()
								+ File.separator
								+ NESTED_INSTANCE_REPORT_FILENAME);
					}

					JasperReport report = JasperCompileManager
							.compileReport(jrxmlFile.getAbsolutePath());
					jasperPrint = JasperFillManager.fillReport(report, params,
							dataSource);

				} else if (scriptName.equals("user_storage")) {

					StorageReportLineGenerator generator = StorageReportLineGenerator
							.getInstance();
					File jrxmlFile = null;
					JRDataSource dataSource = null;
					if (groupById == 0) {
						List<StorageReportLine> list = generator
								.getReportLines(period, criterion, displayUnits);
						dataSource = new JRBeanCollectionDataSource(list);
						jrxmlFile = new File(SubDirectory.REPORTS.toString()
								+ File.separator + STORAGE_REPORT_FILENAME);
					} else {
						List<StorageReportLine> list = generator
								.getReportLines(period, groupByCriterion,
										criterion, displayUnits);
						dataSource = new JRBeanCollectionDataSource(list);
						jrxmlFile = new File(SubDirectory.REPORTS.toString()
								+ File.separator
								+ NESTED_STORAGE_REPORT_FILENAME);
					}
					JasperReport report = JasperCompileManager
							.compileReport(jrxmlFile.getAbsolutePath());
					jasperPrint = JasperFillManager.fillReport(report, params,
							dataSource);

				} else if (scriptName.equals("user_s3")) {

					S3ReportLineGenerator generator = S3ReportLineGenerator
							.getInstance();
					File jrxmlFile = null;
					JRDataSource dataSource = null;
					if (groupById == 0) {
						List<S3ReportLine> list = generator.getReportLines(
								period, criterion, displayUnits);
						dataSource = new JRBeanCollectionDataSource(list);
						jrxmlFile = new File(SubDirectory.REPORTS.toString()
								+ File.separator + S3_REPORT_FILENAME);
					} else {
						List<S3ReportLine> list = generator.getReportLines(
								period, groupByCriterion, criterion,
								displayUnits);
						dataSource = new JRBeanCollectionDataSource(list);
						jrxmlFile = new File(SubDirectory.REPORTS.toString()
								+ File.separator + NESTED_S3_REPORT_FILENAME);
					}
					JasperReport report = JasperCompileManager
							.compileReport(jrxmlFile.getAbsolutePath());
					jasperPrint = JasperFillManager.fillReport(report, params,
							dataSource);
				}

				exporter.setParameter(JRExporterParameter.JASPER_PRINT,
						jasperPrint);
				// exporter.setParameter( JRExporterParameter.PAGE_INDEX, new
				// Integer( Param.page.get( ) ) );
				exporter.exportReport();
			} catch (Throwable ex) {
				LOG.error(ex, ex);
				res.setContentType("text/plain");
				LOG.error("Could not create the report stream "
						+ ex.getMessage() + " " + ex.getLocalizedMessage());
				ex.printStackTrace(res.getWriter());
			} finally {
				reportType.close(res);
			}
		} catch (NoSuchFieldException e) {
			LOG.debug(e, e);
		} catch (IOException e) {
			LOG.debug(e, e);
		}
	}
	
	private void verifySession(String sessionId)
	{
		SessionInfo session;
		try {
			session = EucalyptusWebBackendImpl.verifySession(sessionId);
			User user = null;
			try {
				user = Accounts.lookupUserByName(session.getUserId());
			} catch (Exception e) {
				throw new RuntimeException("User does not exist");
			}
			if (!user.isSystemAdmin()) {
				throw new RuntimeException(
						"Only administrators can view reports.");
			}
		} catch (SerializableException e1) {
			throw new RuntimeException("Error obtaining session info.");
		}
		session.setLastAccessed(System.currentTimeMillis());
	}
	
	enum Type
	{
		pdf {
			@Override
			public JRExporter getReportExporter(HttpServletRequest request,
					HttpServletResponse res, String name) throws IOException
			{
				res.setContentType("application/pdf");
				res.setHeader("Content-Disposition", "file; filename=" + name
						+ ".pdf");
				JRExporter exporter = new JRPdfExporter();
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM,
						res.getOutputStream());
				return exporter;
			}
		},
		csv {
			@Override
			public JRExporter getReportExporter(HttpServletRequest request,
					HttpServletResponse res, String name) throws IOException
			{
				res.setContentType("text/plain");
				res.setHeader("Content-Disposition", "file; filename=" + name
						+ ".csv");
				JRExporter exporter = new JRCsvExporter();
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM,
						res.getOutputStream());
				return exporter;
			}
		},
		html {
			@Override
			public JRExporter getReportExporter(HttpServletRequest request,
					HttpServletResponse res, String name) throws IOException
			{
				PrintWriter out = res.getWriter();
				res.setContentType("text/html");
				JRExporter exporter = new JRHtmlExporter();
				exporter.setParameter(new JRExporterParameter("EUCA_WWW_DIR")
				{
				}, "/");
				exporter.setParameter(JRExporterParameter.OUTPUT_WRITER,
						res.getWriter());
				exporter.setParameter(
						JRHtmlExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS,
						Boolean.TRUE);
				exporter.setParameter(
						JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN,
						Boolean.FALSE);
				exporter.setParameter(
						JRHtmlExporterParameter.IGNORE_PAGE_MARGINS,
						Boolean.TRUE);
				return exporter;
			}

			@Override
			public void close(HttpServletResponse res) throws IOException
			{
				res.getWriter().close();
			}
		},
		xls {
			@Override
			public JRExporter getReportExporter(HttpServletRequest request,
					HttpServletResponse res, String name) throws IOException
			{
				res.setContentType("application/vnd.ms-excel");
				res.setHeader("Content-Disposition", "file; filename=" + name
						+ ".xls");
				JRExporter exporter = new JRXlsExporter();
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM,
						res.getOutputStream());
				return exporter;
			}
		};
		public abstract JRExporter getReportExporter(HttpServletRequest request,
				HttpServletResponse res, String name) throws IOException;

		public void close(HttpServletResponse res) throws IOException
		{
			res.getOutputStream().close();
		}
	}
	
}
