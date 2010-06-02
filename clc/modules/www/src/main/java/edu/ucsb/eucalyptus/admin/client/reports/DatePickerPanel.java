package edu.ucsb.eucalyptus.admin.client.reports;

import java.util.Date;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DatePicker;

public class DatePickerPanel extends HorizontalPanel {
  private static final DateTimeFormat DATE_FORMAT = DateTimeFormat.getMediumDateFormat( );
  private final IconButton       calendarButton;
  private final HTML                  labelText;
  private final DateBox               dateBox;
  
  public DatePickerPanel( final String labelText, final Date initialDate, final ValueChangeHandler<Date> handler ) {
    this.ensureDebugId( "dateRange" + labelText );
    this.setHorizontalAlignment( HorizontalPanel.ALIGN_CENTER );
    this.setWidth( "100%" );
    this.labelText = new HTML( labelText ) {{
      setStyleName( "acct-HTML" );
    }};
    this.setCellHorizontalAlignment( this.labelText, HorizontalPanel.ALIGN_LEFT );
    DatePicker datePicker = new DatePicker( ) {
      {
        addValueChangeHandler( handler );
      }
    };
    this.dateBox = new DateBox( datePicker, initialDate, new DateBox.DefaultFormat( DATE_FORMAT ) );
    this.calendarButton = new IconButton( "Select the " + labelText + " date.", Images.SELF.calendar( ), new ClickHandler( ) {
      @Override
      public void onClick( ClickEvent clickevent ) {
        if ( DatePickerPanel.this.dateBox.isDatePickerShowing( ) ) {
          DatePickerPanel.this.dateBox.hideDatePicker( );
        } else {
          DatePickerPanel.this.dateBox.showDatePicker( );
        }
      }
    } );
    
    this.add( this.labelText );
    this.setCellHorizontalAlignment( this.labelText, HorizontalPanel.ALIGN_RIGHT );
    this.setCellVerticalAlignment( this.labelText, HorizontalPanel.ALIGN_MIDDLE );
    this.setCellWidth( this.labelText, "100%" );
    this.add( this.dateBox );
    this.setCellHorizontalAlignment( this.dateBox, HorizontalPanel.ALIGN_RIGHT );
    this.setCellVerticalAlignment( this.dateBox, HorizontalPanel.ALIGN_MIDDLE );
    this.add( this.calendarButton );
    this.setCellHorizontalAlignment( this.calendarButton, HorizontalPanel.ALIGN_RIGHT );
    this.setCellVerticalAlignment( this.calendarButton, HorizontalPanel.ALIGN_MIDDLE );
  }
  
  public void setValue( Date date ) {
    this.dateBox.setValue( date, true );
  }

  public Date getValue( ) {
    return this.dateBox.getValue( );
  }
  
}
