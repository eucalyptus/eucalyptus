package com.eucalyptus.reporting;

import java.io.*;

import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.export.*;

public enum ReportFormat
{
	pdf {
		@Override
		public JRExporter getExporter()
			throws IOException
		{
			return new JRPdfExporter();
		}
	},
	csv {
		@Override
		public JRExporter getExporter() throws IOException
		{
			return new JRCsvExporter();
		}
	},
	html {
		@Override
		public JRExporter getExporter()
			throws IOException
		{
			JRExporter exporter = new JRHtmlExporter();
			exporter.setParameter(new JRExporterParameter("EUCA_WWW_DIR")
			{
			}, "/");
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
	},
	xls {
		@Override
		public JRExporter getExporter() throws IOException
		{
			return new JRXlsExporter();
		}
	};
	
	public abstract JRExporter getExporter()
		throws IOException;

}
