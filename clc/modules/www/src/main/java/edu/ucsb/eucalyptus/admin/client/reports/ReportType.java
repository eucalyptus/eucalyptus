package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.EucaImageButton;

public enum ReportType {
  PDF {
    @Override
    public String getImageName( ) {
      return "application-pdf.png";
    }
  },
  CSV {
    @Override
    public String getImageName( ) {
      return "application-x-applix-spreadsheet.png";
    }
  },
  XLS {
    @Override
    public String getImageName( ) {
      return "application-vnd.ms-excel.png";
    }
  },
  HTML {
    @Override
    public String getImageName( ) {
      return "application-xhtml+xml.png";
    }
  };
  public EucaImageButton button;
  
  public EucaImageButton makeImageButton( final AccountingControl parent ) {
    return new EucaImageButton( this.name( ), "Download this report as a " + this.name( ) + ".", AccountingControl.ACCT_ACTION_BUTTON, this.getImageName( ),
                                new ClickHandler( ) {
                                  @Override
                                  public void onClick( ClickEvent clickevent ) {
                                    Window.Location.replace( parent.getCurrentReport( ).getUrl( ReportType.this ) );
                                    parent.setCurrentReport( parent.getCurrentReport( ) );
                                  }
                                } );
  }
  
  public abstract String getImageName( );
}