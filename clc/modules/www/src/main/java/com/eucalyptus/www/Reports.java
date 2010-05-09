package com.eucalyptus.www;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.system.BaseDirectory;

public class Reports extends HttpServlet {
  private static Logger LOG = Logger.getLogger( Reports.class );
  
  @Override
  protected void doGet( HttpServletRequest req, HttpServletResponse res ) throws ServletException, IOException {
    
    String temp = req.getParameter( "type" );
    LOG.info( "Report request of type=" + temp );
    String reportType = null;
    if ( temp instanceof String ) {
      reportType = ( String ) temp;
    }
    String baseDir = this.getServletConfig( ).getInitParameter( "PWD" );
    String url = String.format( "%s_%s?createDatabaseIfNotExist=true", Component.db.getUri( ).toString( ), "records" );
    final JRExporter exporter;
    if ( "pdf".equals( reportType ) ) {
      res.setContentType( "application/pdf" );
      res.setHeader( "Content-Disposition", "file; filename=FIXME.pdf" );
      exporter = new JRPdfExporter( );
      exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
    } else if ( "csv".equals( reportType ) ) {
      res.setContentType( "text/plain" );
      res.setHeader( "Content-Disposition", "file; filename=FIXME.csv" );
      exporter = new JRCsvExporter( );
      exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
    } else if ( "xls".equals( reportType ) ) {
      res.setContentType( "application/vnd.ms-excel" );
      res.setHeader( "Content-Disposition", "file; filename=FIXME.xls" );
      exporter = new JRXlsExporter( );
      exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
    } else {
      baseDir = "/";
      PrintWriter out = res.getWriter( );
      res.setContentType( "text/html" );
      exporter = new JRHtmlExporter( );
      exporter.setParameter( new JRExporterParameter( "EUCA_WWW_DIR" ) {}, baseDir );
      exporter.setParameter( JRExporterParameter.OUTPUT_WRITER, res.getWriter( ) );
    }
    
    try {
      JasperDesign jasperDesign = JRXmlLoader.load( BaseDirectory.CONF.toString( ) + File.separator + "report2.jrxml" );
      JasperReport jasperReport = JasperCompileManager.compileReport( jasperDesign );
      Connection jdbcConnection = DriverManager.getConnection( url, "eucalyptus", Hmacs.generateSystemSignature( ) );
      JasperPrint jasperPrint = JasperFillManager.fillReport( jasperReport, null, jdbcConnection );
      exporter.setParameter( JRExporterParameter.JASPER_PRINT, jasperPrint );
      exporter.exportReport( );
    } catch ( Exception ex ) {
      res.setContentType( "text/html" );
      LOG.error( "Could not create the report stream " + ex.getMessage( ) + " " + ex.getLocalizedMessage( ) );
      ex.printStackTrace( res.getWriter( ) );
    } finally {
      if ( exporter instanceof JRHtmlExporter ) {
        res.getWriter( ).close( );
      } else {
        res.getOutputStream( ).close( );
      }
    }
  }
}
