package edu.ucsb.eucalyptus.admin.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public static final String ACCT_REPORT_PAGE_TEXTBOX   = "euca-noop";
    
    @NotStrict
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/accounting.css" )
    public CssResource css( );
    
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/reports/system-logs.png" )
    public ImageResource systemReports( );
    
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/reports/user-logs.png" )
    public ImageResource resourceReports( );
    
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/reports/service-logs.png" )
    public ImageResource serviceReports( );
    
  }
  
  private final String                     sessionId;
  private final AccountingPanel            accountingPanel;
  private Integer                          currentPage     = 0;
  private Boolean                          ready           = Boolean.FALSE;
  private Boolean                          forceFlush      = Boolean.FALSE;
  private ReportInfo                       currentReport   = null;
  private EucaButton                       errorButton     = null;
  private Long                             startMillis;
  private Long                             endMillis;
  private Integer                          criterionInd    = new Integer(0);
  private Integer                          groupByInd      = new Integer(2);
  private final Map<String, List<ReportInfo>> groupMap;
  
  public AccountingControl( String sessionId ) {
    this.ensureDebugId( "AccountingControl" );
    RESOURCES.css( ).ensureInjected( );
    this.groupMap = new HashMap<String, List<ReportInfo>>( );
    Date now = new Date( );
    this.endMillis = now.getTime( );
    this.startMillis = ( this.endMillis - ( 1000l * 60 * 60 * 24 * 7 ) );
    GWT.setUncaughtExceptionHandler( new GWT.UncaughtExceptionHandler( ) {
      @Override
      public void onUncaughtException( Throwable arg0 ) {
        AccountingControl.this.displayError( arg0 );
      }
    } );
    this.sessionId = sessionId;
    this.accountingPanel = new AccountingPanel( this );
    this.errorButton = Buttons.HIDDEN;
    this.currentReport = ReportInfo.BOGUS;
    this.currentReport.setParent( this );
    EucalyptusWebBackend.App.getInstance( ).getReports( this.getSessionid( ), new AsyncCallback<List<ReportInfo>>( ) {
      @Override
      public void onSuccess( List<ReportInfo> result ) {
        AccountingControl.this.setReports( result );
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
  
  protected void setReports( List<ReportInfo> result ) {
    this.groupMap.clear( );
    for( ReportInfo r : result ) {
      if( !this.groupMap.containsKey( r.getGroup( ) ) ) {
        this.groupMap.put( r.getGroup( ), new ArrayList<ReportInfo>() );
      }
      this.groupMap.get( r.getGroup( ) ).add( r );
      r.setParent( this );
    }
//    if ( !this.groupMap.isEmpty( ) ) {
//      this.setCurrentReport( this.groupMap.get( "system" ).get( 0 ) );
//    } else {
//    }
    this.setCurrentReport( ReportInfo.BOGUS );
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
    return this.groupMap.get( "system" );
  }
  
  public List<ReportInfo> getZoneReports( ) {
    return this.groupMap.get( "service" );
  }
  
  public Map<String, List<ReportInfo>> getGroupMap( ) {
    return this.groupMap;
  }

  public List<ReportInfo> getResourceReports( ) {
    return this.groupMap.get( "user" );
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
    return this.currentReport.getUrl( html );
  }
  
  public String getCurrentFileName( ) {
    return this.currentReport.getFileName( );
  }
  
  public Long getStartMillis( ) {
    return this.startMillis;
  }
  
  public Long getEndMillis( ) {
    return this.endMillis;
  }
  
  public Long changeStartMillis( Long millis ) {
    Date now = new Date( );
    Long currentTime = now.getTime( );
    if ( ( millis > currentTime ) ) {
      this.startMillis = currentTime;
    } else if ( millis < 0 ) {
      this.startMillis = currentTime;
    } else {
      this.startMillis = millis;
    }
    this.update( );
    return this.startMillis;
  }
  
  public Long changeEndMillis( Long millis ) {
    Date now = new Date( );
    Long currentTime = now.getTime( );
    if ( ( millis > currentTime ) ) {
      this.endMillis = currentTime;
    } else if ( millis < 0 ) {
      this.endMillis = currentTime;
    } else {
      this.endMillis = millis;
    }
    this.update( );
    return this.endMillis;
  }
  
  public Integer getCriterionInd()
  {
	  return criterionInd;
  }
  
  public void setCriterionInd(Integer criterionInd)
  {
	  this.criterionInd = criterionInd;
  }
  
  public Integer getGroupByInd()
  {
	  return groupByInd;
  }
  
  public void setGroupByInd(Integer groupByInd)
  {
	  this.groupByInd = groupByInd;
  }
  
  public Date getStartTime( ) {
    return new Date( this.startMillis );
  }
  
  public Date getEndTime( ) {
    return new Date( this.endMillis );
  }
  
}
