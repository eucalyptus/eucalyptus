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
  enum Param {
    name, type;
    public String get( HttpServletRequest req ) throws IllegalArgumentException {
      if( req.getParameter( this.name( ) ) == null ) {
        throw new IllegalArgumentException( "'" + this.name() + "' is a required argument." );
      } else {
        String res = req.getParameter( this.name() );
        LOG.debug( "Found parameter: " + this.name() + "=" + res );
        return res;
      }
    }
  }
  enum Type {
    pdf {
      @Override
      public JRExporter setup( HttpServletResponse res, String name ) throws IOException {
        res.setContentType( "application/pdf" );
        res.setHeader( "Content-Disposition", "file; filename="+name+".pdf" );
        JRExporter exporter = new JRPdfExporter( );
        exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
        return exporter;        
      }
    }, csv {
      @Override
      public JRExporter setup( HttpServletResponse res, String name ) throws IOException {
        res.setContentType( "text/plain" );
        res.setHeader( "Content-Disposition", "file; filename="+name+".csv" );
        JRExporter exporter = new JRCsvExporter( );
        exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
        return exporter;
      }
    }, html {
      @Override
      public JRExporter setup( HttpServletResponse res, String name ) throws IOException {
        PrintWriter out = res.getWriter( );
        res.setContentType( "text/html" );
        JRExporter exporter = new JRHtmlExporter( );
        exporter.setParameter( new JRExporterParameter( "EUCA_WWW_DIR" ) {}, "/" );
        exporter.setParameter( JRExporterParameter.OUTPUT_WRITER, res.getWriter( ) );
        return exporter;
      }
      @Override
      public void close( HttpServletResponse res ) throws IOException {
        res.getWriter( ).close( );
      }
    }, xls {
      @Override
      public JRExporter setup( HttpServletResponse res, String name ) throws IOException {
        res.setContentType( "application/vnd.ms-excel" );
        res.setHeader( "Content-Disposition", "file; filename="+name+".xls" );
        JRExporter exporter = new JRXlsExporter( );
        exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
        return exporter;
      }
    };
    public abstract JRExporter setup( HttpServletResponse res, String name ) throws IOException;
    public void close( HttpServletResponse res ) throws IOException {
      res.getOutputStream( ).close( );
    }
  }
  @Override
  protected void doGet( HttpServletRequest req, HttpServletResponse res ) throws ServletException, IOException {
    //FIXME: RELEASE: verify the session id somewhere?
    String name = Param.name.get( req );
    String type = Param.type.get( req );
    String url = String.format( "%s_%s?createDatabaseIfNotExist=true", Component.db.getUri( ).toString( ), "records" );
    Type reportType = Type.valueOf( type );
    final JRExporter exporter = reportType.setup( res, name );    
    try {
      JasperDesign jasperDesign = JRXmlLoader.load( BaseDirectory.CONF.toString( ) + File.separator + name +".jrxml" );
      JasperReport jasperReport = JasperCompileManager.compileReport( jasperDesign );
      Connection jdbcConnection = DriverManager.getConnection( url, "eucalyptus", Hmacs.generateSystemSignature( ) );
      JasperPrint jasperPrint = JasperFillManager.fillReport( jasperReport, null, jdbcConnection );
      exporter.setParameter( JRExporterParameter.JASPER_PRINT, jasperPrint );
      exporter.exportReport( );
    } catch ( Throwable ex ) {
      res.setContentType( "text/plain" );
      LOG.error( "Could not create the report stream " + ex.getMessage( ) + " " + ex.getLocalizedMessage( ) );
      ex.printStackTrace( res.getWriter( ) );
    } finally {
      reportType.close( res );
    }
  }
}
