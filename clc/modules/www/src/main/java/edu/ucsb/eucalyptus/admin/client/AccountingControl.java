package edu.ucsb.eucalyptus.admin.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.ucsb.eucalyptus.admin.client.reports.AccountingPanel;
import edu.ucsb.eucalyptus.admin.client.reports.ReportInfo;
import edu.ucsb.eucalyptus.admin.client.reports.ReportType;
import edu.ucsb.eucalyptus.admin.client.util.Buttons;
import edu.ucsb.eucalyptus.admin.client.util.Observer;

public class AccountingControl extends VerticalPanel implements ContentControl, Observer {
  public final static ResourceBundle RESOURCES = GWT.create( ResourceBundle.class );
  
  public interface ResourceBundle extends ClientBundle {
    public final static String TAB_ROOT_STYLE             = "acct-root";
    public final static String ROOT_PANEL_STYLE           = "acct-AccountingPanel";
    public final static String DISPLAY_PANEL_STYLE        = "acct-ReportDisplay";
    public final static String LIST_PANEL_STYLE           = "acct-ReportList";
    public final static String BUTTON_STYLE               = "acct-Button-Action";
    public final static String STACK_HEADER_STYLE         = "acct-StackPanelHeader";
    public final static String REPORT_BAR_STYLE           = "acct-report-bar";
    public final static String REPORT_SET_STYLE           = "acct-Report-Set";
    public final static String REPORT_FRAME_STYLE         = "acct-Frame";
    public static final String ACCT_REPORT_BUTTON         = "acct-Button-Report";
    public static final String ACCT_REPORT_CURRENT_BUTTON = "acct-Button-Report-Active";
    public static final String DISPLAY_BAR_STYLE          = STACK_HEADER_STYLE;
    public static final String ACCT_REPORT_PAGE_TEXTBOX   = "acct-ReportPageNum";
    
    @NotStrict
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/accounting.css" )
    public CssResource css( );
    
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/system-logs.png" )
    public ImageResource systemReports( );
    
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/user-logs.png" )
    public ImageResource resourceReports( );
    
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/service-logs.png" )
    public ImageResource serviceReports( );
    
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/go-next.png" )
    public ImageResource test2( );
  }
  
  private final String           sessionId;
  private final List<ReportInfo> systemReports   = new ArrayList<ReportInfo>( );
  private final List<ReportInfo> zoneReports     = new ArrayList<ReportInfo>( );
  private final List<ReportInfo> resourceReports = new ArrayList<ReportInfo>( );
  private final AccountingPanel  accountingPanel;
  private Integer                currentPage     = 0;
  private Boolean                ready           = Boolean.FALSE;
  private Boolean                forceFlush      = Boolean.FALSE;
  private ReportInfo             currentReport   = null;
  private EucaButton             errorButton     = null;
  private Long                   startMillis;
  private Long                   endMillis;
  public final ReportInfo        bogus;
  
  public AccountingControl( String sessionId ) {
    this.ensureDebugId( "AccountingControl" );
    RESOURCES.css( ).ensureInjected( );
    GWT.setUncaughtExceptionHandler( new GWT.UncaughtExceptionHandler( ) {
      @Override
      public void onUncaughtException( Throwable arg0 ) {
        AccountingControl.this.displayError( arg0 );
      }
    } );
    this.sessionId = sessionId;
    this.accountingPanel = new AccountingPanel( this );
    this.errorButton = Buttons.HIDDEN;
    Date now = new Date( );
    this.endMillis = now.getTime( );
    this.startMillis = ( this.endMillis - ( 1000l * 60 * 24 * 7 ) );
    this.bogus = new ReportInfo( "System Log", "system-log", 0 ) {
      {
        setParent( AccountingControl.this );
      }
    };
    this.currentReport = this.bogus;
    EucalyptusWebBackend.App.getInstance( ).getSystemReports( this.getSessionid( ), new AsyncCallback<List<ReportInfo>>( ) {
      @Override
      public void onSuccess( List<ReportInfo> result ) {
        AccountingControl.this.setSystemReports( result );
      }
      
      @Override
      public void onFailure( Throwable arg0 ) {
        AccountingControl.this.displayError( arg0 );
      }
    } );
    EucalyptusWebBackend.App.getInstance( ).getZoneReports( this.getSessionid( ), new AsyncCallback<List<ReportInfo>>( ) {
      @Override
      public void onSuccess( List<ReportInfo> result ) {
        AccountingControl.this.setZoneReports( result );
      }
      
      @Override
      public void onFailure( Throwable arg0 ) {
        AccountingControl.this.displayError( arg0 );
      }
    } );
    EucalyptusWebBackend.App.getInstance( ).getResourceReports( this.getSessionid( ), new AsyncCallback<List<ReportInfo>>( ) {
      @Override
      public void onSuccess( List<ReportInfo> result ) {
        AccountingControl.this.setResourceReports( result );
      }
      
      @Override
      public void onFailure( Throwable arg0 ) {
        AccountingControl.this.displayError( arg0 );
      }
    } );
  }
  
  protected void displayError( Throwable arg0 ) {
    this.errorButton = Buttons.errorButton( arg0 );
    this.redraw( );
  }
  
  protected void setSystemReports( List<ReportInfo> result ) {
    this.systemReports.clear( );
    this.systemReports.addAll( result );
    if ( !this.systemReports.isEmpty( ) ) {
      for ( ReportInfo r : this.systemReports ) {
        r.setParent( this );
      }
      this.setCurrentReport( this.systemReports.get( 0 ) );
    } else {
      this.setCurrentReport( this.bogus );
    }
    this.redraw( );
  }
  
  protected void setZoneReports( List<ReportInfo> result ) {
    this.zoneReports.clear( );
    this.zoneReports.addAll( result );
    if ( !this.zoneReports.isEmpty( ) ) {
      for ( ReportInfo r : this.zoneReports ) {
        r.setParent( this );
      }
    }
    this.redraw( );
  }
  
  protected void setResourceReports( List<ReportInfo> result ) {
    this.resourceReports.clear( );
    this.resourceReports.addAll( result );
    if ( !this.resourceReports.isEmpty( ) ) {
      for ( ReportInfo r : this.resourceReports ) {
        r.setParent( this );
      }
    }
    this.redraw( );
  }
  
  @Override
  public Widget getRootWidget( ) {
    return this;
  }
  
  @Override
  public void update( ) {
    this.accountingPanel.update( );
  }
  
  @Override
  public void display( ) {
    this.update( );
  }
  
  @Override
  public void redraw( ) {
    this.clear( );
    this.addStyleName( RESOURCES.ROOT_PANEL_STYLE );
    this.add( this.errorButton );
    this.add( this.accountingPanel );
    this.accountingPanel.redraw( );
    this.display( );
  }
  
  public Boolean isReady( ) {
    return this.currentReport != null;
  }
  
  public void setCurrentReport( ReportInfo newReport ) {
    if ( newReport != null && !newReport.equals( this.currentReport ) ) {
      this.currentPage = 0;
      this.currentReport = newReport;
      this.display( );
    }
  }
  
  public ReportInfo getCurrentReport( ) {
    return this.currentReport;
  }
  
  public String getSessionid( ) {
    return this.sessionId;
  }
  
  public Integer getCurrentPage( ) {
    return this.currentPage;
  }
  
  public Boolean getForceFlush( ) {
    return forceFlush;
  }
  
  public List<ReportInfo> getSystemReports( ) {
    return this.systemReports;
  }
  
  public List<ReportInfo> getZoneReports( ) {
    return this.zoneReports;
  }
  
  public List<ReportInfo> getResourceReports( ) {
    return this.resourceReports;
  }
  
  public Integer changePage( Integer delta ) {
    if ( this.currentPage + delta < 0 ) {
      this.currentPage = 0;
    } else if ( this.currentReport != null && ( this.currentPage >= this.currentReport.getLength( ) ) ) {
      this.currentPage = this.currentReport.getLength( );
    } else {
      this.currentPage += delta;
    }
    this.update( );
    return this.currentPage;
  }
  
  public Integer lastPage( ) {
    if ( this.currentReport != null ) {
      return this.currentReport.getLength( );
    } else {
      return 0;
    }
  }
  
  public String getCurrentUrl( ReportType html ) {
    if ( this.currentReport != null ) {
      return this.currentReport.getUrl( html );
    } else {
      return "#";
    }
  }
  
  public String getCurrentFileName( ) {
    if ( this.currentReport != null ) {
      return this.currentReport.getFileName( );
    } else {
      return "#";
    }
  }
  
  public Long getStartMillis( ) {
    return this.startMillis;
  }
  
  public Long getEndMillis( ) {
    return this.endMillis;
  }
  
  public Long changeStartMillis( Long millis ) {
    Long currentTime = new Date( ).getTime( );
    if ( millis > this.endMillis ) {
      this.startMillis = this.endMillis;
    } else if ( ( millis > currentTime ) ) {
      this.startMillis = this.endMillis;
    } else if ( millis < 0 ) {
      this.startMillis = this.startMillis;
    } else {
      this.startMillis = millis;
    }
    return this.startMillis;
  }
  
  public Long changeEndMillis( Long millis ) {
    Long currentTime = new Date( ).getTime( );
    if ( millis < this.startMillis ) {
      this.endMillis = this.startMillis;
    } else if ( ( millis > currentTime ) ) {
      this.endMillis = currentTime;
    } else if ( millis < 0 ) {
      this.endMillis = this.startMillis;
    } else {
      this.endMillis = millis;
    }
    return this.endMillis;
  }
  
  public Date getStartTime( ) {
    return new Date( this.startMillis );
  }
  
  public Date getEndTime( ) {
    return new Date( this.endMillis );
  }
  
}
