package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface Images extends ClientBundle {
  public final static Images SELF = GWT.create( Images.class );
  
  @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/mimetypes/application-pdf.png" )
  public ImageResource pdf( );
  
  @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/mimetypes/application-vnd.ms-excel.png" )
  public ImageResource xls( );
  
  @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/mimetypes/application-x-applix-spreadsheet.png" )
  public ImageResource csv( );
  
  @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/mimetypes/application-xhtml+xml.png" )
  public ImageResource html( );
  
  @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/actions/calendar.png" )
  public ImageResource calendar( );
  
  @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/actions/go-next.png" )
  public ImageResource next( );
  
  @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/actions/go-previous.png" )
  public ImageResource previous( );
  
  @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/actions/go-first.png" )
  public ImageResource first( );
  
  @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/actions/go-last.png" )
  public ImageResource last( );
}
