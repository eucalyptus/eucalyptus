package edu.ucsb.eucalyptus.admin.client;

import java.util.ArrayList;
import java.util.List;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class AccountingControl implements ContentControl {
  private String sessionId;
  private ReportInfo currentReport = null;
  //static list of report types
  public enum ReportType {
    PDF, CSV, XLS,HTML;
    public EucaButton button;
  }
  //FIXME: load this list dynamicly
  private final List<ReportInfo> reports = new ArrayList<ReportInfo>( ) {
    {
      add( new ReportInfo( ) {
        {
          name = "System Log";
          fileName = "system-log";
        }
      } );
      add( new ReportInfo( ) {
        {
          name = "Message Log";
          fileName = "msg-log";
        }
      } );
      add( new ReportInfo( ) {
        {
          name = "Test Log";
          fileName = "report2";
        }
      } );
    }
  };
  //FIXME: yawn at this. see above.
  class ReportInfo {
    public EucaButton button;
    public String name;
    public String fileName;
    public String getUrl( ReportType type ) {
      return "/reports?name="+AccountingControl.this.getCurrentReport().fileName+"&type="+type.name().toLowerCase( );
    }
  }
  private VerticalPanel   root;
  private HorizontalPanel reportBar;
  private HorizontalPanel       buttonBar;
  private Frame           report;
  public AccountingControl( String sessionId ) {
    this.sessionId = sessionId;
    this.currentReport = reports.get( 0 );
    this.root = new VerticalPanel( ) {
      {
        addStyleName( "FIXME1" );
        add( AccountingControl.this.reportBar = new HorizontalPanel( ) {
          {
            for ( final ReportInfo ent : AccountingControl.this.reports ) {
              add( ent.button = new EucaButton( ent.name, "View " + ent.name + " Report.", new ClickHandler( ) {
                @Override
                public void onClick( ClickEvent arg0 ) {
                  AccountingControl.this.setCurrentReport( ent );
                }
              } ) );
            }
          }
        });
        add( AccountingControl.this.buttonBar = new HorizontalPanel( ) {
          {
            for ( final ReportType r : ReportType.values( ) ) {
              add( r.button = new EucaButton( r.name( ), "Download this report as a " + r.name( ) + ".", new ClickHandler( ) {
                @Override
                public void onClick( ClickEvent clickevent ) {
                  Window.Location.replace( AccountingControl.this.currentReport.getUrl( r ) );
                  AccountingControl.this.setCurrentReport( AccountingControl.this.currentReport );
                }
              } ) );
            }
          }
        } );
        add( AccountingControl.this.report = new Frame() );
      }
    };
  }
  
  @Override
  public Widget getRootWidget( ) {
    return this.root;
  }
  
  @Override
  public void display( ) {
    this.report.setUrl( this.currentReport.getUrl( ReportType.HTML ) );
  }

  public void setCurrentReport( ReportInfo currentReport ) {
    this.currentReport = currentReport;
    this.display( );
  }

  public ReportInfo getCurrentReport( ) {
    return currentReport;
  }

}
