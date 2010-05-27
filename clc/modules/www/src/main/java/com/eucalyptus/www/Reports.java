package com.eucalyptus.www;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.system.Threads;
import com.google.gwt.user.client.rpc.SerializableException;
import edu.ucsb.eucalyptus.admin.server.EucalyptusManagement;
import edu.ucsb.eucalyptus.admin.server.EucalyptusWebBackendImpl;
import edu.ucsb.eucalyptus.admin.server.SessionInfo;

@ConfigurableClass( root = "reporting", description = "Parameters controlling the generation of system reports." )
public class Reports extends HttpServlet {
  @ConfigurableField( description = "The number of seconds which a generated report should be cached.", initial = "60" )
  public static Integer REPORT_CACHE_SECS = 500;
  private static Logger LOG               = Logger.getLogger( Reports.class );
  
  enum Param {
    name, type, session, page, flush, width, height;
    public String get( HttpServletRequest req ) throws IllegalArgumentException {
      if ( req.getParameter( this.name( ) ) == null ) {
        throw new IllegalArgumentException( "'" + this.name( ) + "' is a required argument." );
      } else {
        String res = req.getParameter( this.name( ) );
        LOG.debug( "Found parameter: " + this.name( ) + "=" + res );
        return res;
      }
    }
  }
  
  enum Type {
    pdf {
      @Override
      public JRExporter setup( HttpServletRequest request, HttpServletResponse res, String name ) throws IOException {
        res.setContentType( "application/pdf" );
        res.setHeader( "Content-Disposition", "file; filename=" + name + ".pdf" );
        JRExporter exporter = new JRPdfExporter( );
        exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
        return exporter;
      }
    },
    csv {
      @Override
      public JRExporter setup( HttpServletRequest request, HttpServletResponse res, String name ) throws IOException {
        res.setContentType( "text/plain" );
        res.setHeader( "Content-Disposition", "file; filename=" + name + ".csv" );
        JRExporter exporter = new JRCsvExporter( );
        exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
        return exporter;
      }
    },
    html {
      @Override
      public JRExporter setup( HttpServletRequest request, HttpServletResponse res, String name ) throws IOException {
        PrintWriter out = res.getWriter( );
        res.setContentType( "text/html" );
        JRExporter exporter = new JRHtmlExporter( );
        exporter.setParameter( new JRExporterParameter( "EUCA_WWW_DIR" ) {}, "/" );
        String pageStr = Param.page.get( request );
        if ( pageStr != null ) {
          try {
            Integer currentPage = new Integer( pageStr );
            exporter.setParameter( JRExporterParameter.PAGE_INDEX, currentPage );
          } catch ( NumberFormatException e ) {}
        }
        exporter.setParameter( JRExporterParameter.OUTPUT_WRITER, res.getWriter( ) );
        exporter.setParameter( JRHtmlExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE );
        exporter.setParameter( JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, Boolean.FALSE );
        return exporter;
      }
      
      @Override
      public void close( HttpServletResponse res ) throws IOException {
        res.getWriter( ).close( );
      }
    },
    xls {
      @Override
      public JRExporter setup( HttpServletRequest request, HttpServletResponse res, String name ) throws IOException {
        res.setContentType( "application/vnd.ms-excel" );
        res.setHeader( "Content-Disposition", "file; filename=" + name + ".xls" );
        JRExporter exporter = new JRXlsExporter( );
        exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
        return exporter;
      }
    };
    public abstract JRExporter setup( HttpServletRequest request, HttpServletResponse res, String name ) throws IOException;
    
    public void close( HttpServletResponse res ) throws IOException {
      res.getOutputStream( ).close( );
    }
  }
  
  @Override
  protected void doGet( HttpServletRequest req, HttpServletResponse res ) throws ServletException, IOException {
    String sessionId = Param.session.get( req );
    String name = Param.name.get( req );
    String type = Param.type.get( req );
    String pageStr = Param.page.get( req );
    SessionInfo session;
    try {
      session = EucalyptusWebBackendImpl.verifySession( sessionId );
      User user = null;
      try {
        user = Users.lookupUser( session.getUserId( ) );
      } catch ( Exception e ) {
        hasError( "User does not exist", res );
        return;
      }
      if ( !user.isAdministrator( ) ) {
        hasError( "Only administrators can view reports.", res );
        return;
      }
    } catch ( SerializableException e1 ) {
      LOG.debug( e1, e1 );
      hasError( "Error obtaining session info.", res );
      return;
    }
    LOG.debug( "Got request for " + name + " page number " + pageStr + " of type " + type );
    Type reportType = Type.valueOf( type );
    final JRExporter exporter = reportType.setup( req, res, name );
    try {
      ReportCache reportCache = getReportManager( name );
      JasperPrint jasperPrint = reportCache.getPendingPrint( );
      exporter.setParameter( JRExporterParameter.JASPER_PRINT, jasperPrint );
      exporter.exportReport( );
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
      res.setContentType( "text/plain" );
      LOG.error( "Could not create the report stream " + ex.getMessage( ) + " " + ex.getLocalizedMessage( ) );
      ex.printStackTrace( res.getWriter( ) );
    } finally {
      reportType.close( res );
    }
  }
  
  public static class ReportCache {
    private final long                timestamp;
    private final String              name;
    private final String              reportName;
    private Integer                   length;
    private final Future<JasperPrint> pendingPrint;
    private JasperPrint jasperPrint = null;
    
    public ReportCache( String name, String reportName, Future<JasperPrint> pendingPrint ) {
      this.timestamp = System.currentTimeMillis( ) / 1000;
      this.name = name;
      this.reportName = reportName;
      this.pendingPrint = pendingPrint;
      this.length = 1;
      if ( this.pendingPrint.isDone( ) ) {
        try {
          this.jasperPrint = this.pendingPrint.get( );
        } catch ( ExecutionException e ) {
          LOG.error( e, e );
        } catch ( Exception e ) {
          LOG.debug( e, e );
        }
      }
    }
    
    @Override
    public int hashCode( ) {
      final int prime = 31;
      int result = 1;
      result = prime * result + ( ( this.name == null ) ? 0 : this.name.hashCode( ) );
      return result;
    }
    
    @Override
    public boolean equals( Object obj ) {
      if ( this == obj ) return true;
      if ( obj == null ) return false;
      if ( getClass( ) != obj.getClass( ) ) return false;
      ReportCache other = ( ReportCache ) obj;
      if ( this.name == null ) {
        if ( other.name != null ) return false;
      } else if ( !this.name.equals( other.name ) ) return false;
      return true;
    }
    
    public String getReportName( ) {
      return this.reportName;
    }
    
    public long getTimestamp( ) {
      return this.timestamp;
    }
    
    public String getName( ) {
      return this.name;
    }
    
    public Integer getLength( ) {
      if( this.jasperPrint != null ) {
        this.length = this.jasperPrint.getPages( ).size( ) - 1;
      }
      return this.length;
    }
    
    public JasperPrint getPendingPrint( ) throws InterruptedException, ExecutionException {
      try {
        return this.pendingPrint.get( );
      } catch ( ExecutionException e ) {
        LOG.error( e, e );
        this.length = 1;
        throw e;
      } catch ( InterruptedException e ) {
        this.length = 1;
        throw e;
      }        
    }
    public JasperPrint getJasperPrint( ) {
      if( this.pendingPrint.isDone( ) ) {
        try {
          this.jasperPrint  = this.getPendingPrint( );
        } catch ( InterruptedException e ) {
        } catch ( ExecutionException e ) {
        }
      }
      return this.jasperPrint;
    }
    
    public boolean isExpired( ) {
      return ( System.currentTimeMillis( ) / 1000l ) - this.timestamp > REPORT_CACHE_SECS;
    }
  }
  
  private static Map<String, ReportCache> reportCache = new ConcurrentHashMap<String, ReportCache>( );
  
  public static ReportCache getReportManager( String name ) throws JRException, SQLException {
    try {
      if ( reportCache.containsKey( name ) && !reportCache.get( name ).isExpired( ) ) {
        return reportCache.get( name );
      } else {
        reportCache.remove( name );
        final JasperDesign jasperDesign = JRXmlLoader.load( SubDirectory.REPORTS.toString( ) + File.separator + name + ".jrxml" );
        Future<JasperPrint> pendingPrint = Threads.getThreadPool( "reporting" ).submit( new Callable<JasperPrint>( ) {
          
          @Override
          public JasperPrint call( ) throws Exception {
            String url = String.format( "jdbc:%s_%s", Component.db.getUri( ).toString( ), "records" );
            JasperReport jasperReport = JasperCompileManager.compileReport( jasperDesign );
            Connection jdbcConnection = DriverManager.getConnection( url, "eucalyptus", Hmacs.generateSystemSignature( ) );
            JasperPrint jasperPrint = JasperFillManager.fillReport( jasperReport, null, jdbcConnection );
            return jasperPrint;
          }
        } );
        reportCache.put( name, new ReportCache( name, jasperDesign.getName( ), pendingPrint ) );
      }
      return reportCache.get( name );
    } catch ( Throwable t ) {
      LOG.error( t, t );
      throw new JRException( t );
    }
  }
  
  public static void hasError( String message, HttpServletResponse response ) {
    try {
      response.getWriter( ).print( EucalyptusManagement.getError( message ) );
      response.getWriter( ).flush( );
    } catch ( IOException e ) {
      e.printStackTrace( );
    }
  }
  
}
