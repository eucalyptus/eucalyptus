package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.EucaButton;
import edu.ucsb.eucalyptus.admin.client.EucaImageButton;
import edu.ucsb.eucalyptus.admin.client.util.Buttons;
import edu.ucsb.eucalyptus.admin.client.util.Events;

public enum ReportAction {
  FIRST {
    @Override
    public Integer apply( AccountingControl parent ) {
      return parent.changePage( -1 * MAX_DELTA );
    }
  },
  PREVIOUS {
    @Override
    public Integer apply( AccountingControl parent ) {
      return parent.changePage( -1 );
    }
  },
  FIRST_PAGE {
    @Override
    public Integer apply( AccountingControl parent ) {
      return 1;
    }
    
    @Override
    public Widget makeImageButton( AccountingControl parent ) {
      return new EucaButton( "1", "First Page", Buttons.STYLE_NOOP, Events.DO_NOTHING );
    }
  },
  PAGE_INFO {
    @Override
    public Integer apply( AccountingControl parent ) {
      return parent.getCurrentPage( );
    }
    
    @Override
    public Widget makeImageButton( final AccountingControl parent ) {
      TextBox currentPageText = new TextBox( ) {
        {
          setText( "" + parent.getCurrentPage( ) );
          addValueChangeHandler( new ValueChangeHandler<String>( ) {
            @Override
            public void onValueChange( ValueChangeEvent<String> event ) {
              try {
                Integer newPage = Integer.parseInt( event.getValue( ) );
                parent.changePage( -1 * MAX_DELTA );
                parent.changePage( newPage );
              } catch ( NumberFormatException e ) {}
            }
          } );
        }
      };
      return currentPageText;
    }
  },
  LAST_PAGE {
    @Override
    public Integer apply( AccountingControl parent ) {
      return parent.lastPage( );
    }
    
    @Override
    public Widget makeImageButton( AccountingControl parent ) {
      return new EucaButton( parent.lastPage( ) + 1 + "", "Last Page", Buttons.STYLE_NOOP, Events.DO_NOTHING );
    }
  },
  NEXT {
    @Override
    public Integer apply( AccountingControl parent ) {
      return parent.changePage( 1 );
    }
  },
  LAST {
    @Override
    public Integer apply( AccountingControl parent ) {
      return parent.changePage( MAX_DELTA );
    }
  };
  private static final int MAX_DELTA = 100000000;
  
  public Widget makeImageButton( final AccountingControl parent ) {
    return new ReportButton( this, parent );
  }
  
  public String getImageName( ) {
    return "go-" + this.name( ).toLowerCase( ) + ".png";
  }
  
  public abstract Integer apply( AccountingControl parent );
}
