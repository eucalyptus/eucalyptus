package edu.ucsb.eucalyptus.admin.client.reports;

import java.util.Date;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.util.Observer;

public class DateRange extends VerticalPanel implements Observer {
  private static final DateTimeFormat DATE_FORMAT = DateTimeFormat.getMediumDateFormat( );
  private final AccountingControl        controller;
  private final DatePickerPanel       startDate;
  private final DatePickerPanel       endDate;
  private int                         days        = 7;
  
  public DateRange( AccountingControl controller ) {
    this( controller, 7 );
  }
  
  public DateRange( AccountingControl controller, Integer days ) {
    this.controller = controller;
    this.days = days;
    Integer absdays = Math.abs( this.days );
    Date now = new Date( );
    Long nowMillis = now.getTime( );
    Date start = new Date( nowMillis - ( 1000l * 60 * 24 * absdays ) );
    this.startDate = new DatePickerPanel( "From", start, new ValueChangeHandler<Date>( ) {
      public void onValueChange( ValueChangeEvent<Date> event ) {
        Long newValue = DateRange.this.controller.changeStartMillis( event.getValue( ).getTime( ) );
        DateRange.this.startDate.setValue( new Date( newValue ) );
      }
    } );
    this.endDate = new DatePickerPanel( "To", now, new ValueChangeHandler<Date>( ) {
      public void onValueChange( ValueChangeEvent<Date> event ) {
        Long newValue = DateRange.this.controller.changeEndMillis( event.getValue( ).getTime( ) );
        DateRange.this.endDate.setValue( new Date( newValue ) );
      }
    } );
  }
  
  @Override
  public void redraw( ) {
    this.clear( );
    //:: update time
    Integer absdays = Math.abs( this.days );
    Date now = new Date( );
    Long nowMillis = now.getTime( );
    this.controller.changeStartMillis( nowMillis - ( 1000l * 60 * 24 * absdays ) );
    this.controller.changeEndMillis( nowMillis );
    this.add( this.startDate );
    this.add( this.endDate );
  }
  
  @Override
  public void update( ) {}
  
}
