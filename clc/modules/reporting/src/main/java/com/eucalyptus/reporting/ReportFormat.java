/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.reporting;

import java.io.*;

import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.export.*;

public enum ReportFormat
{
	PDF {
		@Override
		public JRExporter getExporter()
			throws IOException
		{
			return new JRPdfExporter();
		}
	},
	CSV {
		@Override
		public JRExporter getExporter() throws IOException
		{
			return new JRCsvExporter();
		}
	},
	HTML {
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
	XLS {
		@Override
		public JRExporter getExporter() throws IOException
		{
			return new JRXlsExporter();
		}
	};
	
	public abstract JRExporter getExporter()
		throws IOException;

}
