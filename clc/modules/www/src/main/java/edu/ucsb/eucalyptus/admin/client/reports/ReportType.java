package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;

public enum ReportType {
  PDF( "pdf" ), CSV( "x-applix-spreadsheet" ), XLS( "vnd.ms-excel" ), HTML( "xhtml+xml" );
  public IconButton button;
  private final String   mimeType;
  private final String   imageName;
  
  private ReportType( String mimeType ) {
    this.mimeType = mimeType;
    this.imageName = "mimetypes/application-" + this.mimeType + ".png";
  }
  
  public IconButton makeImageButton( final AccountingControl controller ) {
    return new IconButton( "Download this report as a " + this.name( ) + ".", AccountingControl.RESOURCES.BUTTON_STYLE, this.getImageName( ),
                                new ClickHandler( ) {
                                  @Override
                                  public void onClick( ClickEvent clickevent ) {
                                    Window.Location.replace( controller.getCurrentUrl( ReportType.this ) );
                                    controller.update( );
                                  }
                                } );
  }
  
  public String getImageName( ) {
    return this.imageName;
  }
}