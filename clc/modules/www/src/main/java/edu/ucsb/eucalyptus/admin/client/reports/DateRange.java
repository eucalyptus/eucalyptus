package edu.ucsb.eucalyptus.admin.client.reports;

import java.util.Date;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DatePicker;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.util.Observer;

public class DateRange extends HorizontalPanel implements Observer {
  private final HTML                  END_DATE;
  private final HTML                  START_DATE;
  private static final DateTimeFormat DATE_FORMAT = DateTimeFormat.getMediumDateFormat( );
  private final AccountingControl     controller;
//  private final Label                 startDateText;
//  private final Label                 endDateText;
//  private final Integer               days;
  
  public DateRange( AccountingControl controller ) {
    this( controller, 7 );
  }
  
  public DateRange( AccountingControl controller, Integer days ) {
    this.setHorizontalAlignment( ALIGN_LEFT );
    this.START_DATE = new HTML( "Start Date: " );
    this.END_DATE = new HTML( "End Date: " );
    this.controller = controller;
//    this.days = Math.abs( days );
//    Date now = new Date( );
//    Long nowMillis = now.getTime( );
//    this.controller.changeStartMillis( nowMillis - ( 1000l * 60 * 24 * days ) );
//    this.controller.changeEndMillis( nowMillis );
//    this.startDateText = new Label( ) {
//      {
//        setText( "HI" /*DATE_FORMAT.format( DateRange.this.controller.getStartTime( ) )*/ );
//      }
//    };
//    this.endDateText = new Label( ) {
//      {
//        setText( "BYE" /*DATE_FORMAT.format( DateRange.this.controller.getEndTime( ) )*/ );
//      }
//    };
  }
  
//  private Date setStart( Date start ) {
//    Long millis = this.controller.changeStartMillis( start.getTime( ) );
//    this.startDateText.setText( DATE_FORMAT.format( new Date( millis ) ) );
//    return this.controller.getStartTime( );
//  }
//  
//  private Date setEnd( Date end ) {
//    Long millis = this.controller.changeEndMillis( end.getTime( ) );
//    this.endDateText.setText( DATE_FORMAT.format( new Date( millis ) ) );
//    return this.controller.getEndTime( );
//  }
  
  @Override
  public void redraw( ) {
    this.clear( );
//    DatePicker fromDatePicker = new DatePicker( ) {
//      {
//        addValueChangeHandler( new ValueChangeHandler<Date>( ) {
//          public void onValueChange( ValueChangeEvent<Date> event ) {
//            DateRange.this.setStart( event.getValue( ) );
//          }
//        } );
//        setValue( DateRange.this.controller.getStartTime( ), true );
//      }
//    };
//    DateBox fromDateBox = new DateBox( ) {
//      {
//        DateTimeFormat dateFormat = DateTimeFormat.getLongDateFormat( );
//        setFormat( new DateBox.DefaultFormat( dateFormat ) );
//      }
//    };
//    
//    DatePicker toDatePicker = new DatePicker( ) {
//      {
//        addValueChangeHandler( new ValueChangeHandler<Date>( ) {
//          public void onValueChange( ValueChangeEvent<Date> event ) {
//            DateRange.this.setEnd( event.getValue( ) );
//          }
//        } );
//        setValue( DateRange.this.controller.getEndTime( ), true );
//      }
//    };
//    DateBox toDateBox = new DateBox( ) {
//      {
//        DateTimeFormat dateFormat = DateTimeFormat.getLongDateFormat( );
//        setFormat( new DateBox.DefaultFormat( dateFormat ) );
//      }
//    };
    this.add( START_DATE );
//    this.add( fromDateBox );
    this.add( END_DATE );
//    this.add( toDateBox );    
  }
  
  @Override
  public void update( ) {
//    this.redraw( );
  }
  
}
