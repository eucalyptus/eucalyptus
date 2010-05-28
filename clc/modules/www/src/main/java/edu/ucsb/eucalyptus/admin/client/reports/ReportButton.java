package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.EucaImageButton;
import edu.ucsb.eucalyptus.admin.client.util.Observer;

public class ReportButton extends EucaImageButton implements Observer {
  private final AccountingControl controller;
  private final ReportAction      action;
  
  public ReportButton( final ReportAction action, final AccountingControl controller ) {
    super( action.name( ), "Go to page " + action.name( ).toLowerCase( ) + ".", AccountingControl.RESOURCES.BUTTON_STYLE, action.getImageName( ),
           new ClickHandler( ) {
             @Override
             public void onClick( ClickEvent clickevent ) {
               action.apply( controller );
             }
           } );
    this.action = action;
    this.controller = controller;
  }
  
  @Override
  public void redraw( ) {}
  
  @Override
  public void update( ) {}
  
}
