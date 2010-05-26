package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.EucaImageButton;

public enum ReportAction {
  FIRST {
    @Override
    public Integer apply( AccountingControl parent ) {
      return parent.setCurrentPage( 0 );
    }
  },
  PREVIOUS {
    @Override
    public Integer apply( AccountingControl parent ) {
      Integer current = parent.getCurrentPage( );
      return parent.setCurrentPage( current >= 1 ? current - 1 : 0 );
    }
  },
  NEXT {
    @Override
    public Integer apply( AccountingControl parent ) {
      Integer max = parent.getCurrentReport( ).getLength( );
      Integer current = parent.getCurrentPage( );
      return parent.setCurrentPage( current < max ? current+1 : max );
    }
  },
  LAST {
    @Override
    public Integer apply( AccountingControl parent ) {
      return parent.getCurrentReport( ).getLength( );
    }
  };
  public EucaImageButton button;
  
  public EucaImageButton makeImageButton( final AccountingControl parent ) {
    return new EucaImageButton( this.name( ), "Go to the " + this.name( ).toLowerCase( ) + " page.", AccountingControl.ACCT_ACTION_BUTTON, this.getImageName( ),
                                new ClickHandler( ) {
                                  @Override
                                  public void onClick( ClickEvent clickevent ) {
                                    ReportAction.this.apply( parent );
                                  }
                                } );
  }
  
  public String getImageName( ) {
    return "go-" + this.name( ).toLowerCase( ) + ".png";
  }
  
  public abstract Integer apply( AccountingControl parent );
}
