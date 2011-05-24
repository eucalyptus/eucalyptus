package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class LabelWithAnchor extends Composite implements HasClickHandlers {
  
  private static LabelWithAnchorUiBinder uiBinder = GWT.create( LabelWithAnchorUiBinder.class );
  
  interface LabelWithAnchorUiBinder extends UiBinder<Widget, LabelWithAnchor> {}
  
  @UiField
  Label label;
  
  @UiField
  Anchor anchor;
  
  public LabelWithAnchor( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  public void setContent( String title, String actionTitle ) {
    this.label.setText( title );
    this.anchor.setText( actionTitle );
  }

  @Override
  public HandlerRegistration addClickHandler( ClickHandler handler ) {
    return this.anchor.addClickHandler( handler );
  }
  
}
