package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.EucaButton;
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
    public String getImageText( AccountingControl parent ) {
      return ""+parent.getCurrentPage( );
    }


    @Override
    public Widget makeImageButton( final AccountingControl parent ) {
      final TextBox currentPageText = new TextBox( ) {
        {
          setText( PAGE_INFO.getImageText( parent ) );
          setStyleName( AccountingControl.RESOURCES.ACCT_REPORT_PAGE_TEXTBOX );
        }
      };
      ValueChangeHandler handler = new ValueChangeHandler<String>( ) {
        @Override
        public void onValueChange( ValueChangeEvent<String> event ) {
          try {
            Integer newPage = Integer.parseInt( event.getValue( ) );
            parent.changePage( -1 * MAX_DELTA );
            parent.changePage( newPage );
            currentPageText.setText( "" + parent.getCurrentPage( ) );
          } catch ( NumberFormatException e ) {}
        }
      };
      currentPageText.setText( this.getImageText( parent) );
      return currentPageText;
    }
  },
  LAST_PAGE {
    @Override
    public Integer apply( AccountingControl parent ) {
      return parent.lastPage( );
    }
    
    @Override
    public String getImageText( AccountingControl parent ) {
      return parent.lastPage( ) + 1 + "";
    }

    @Override
    public Widget makeImageButton( AccountingControl parent ) {
      return new EucaButton( this.getImageText( parent ), "Last Page", Buttons.STYLE_NOOP, Events.DO_NOTHING );
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
  
  public String getImageText( final AccountingControl parent ) {
    return this.name( );
  }

  public Widget makeImageButton( final AccountingControl parent ) {
    return new ReportButton( this, parent );
  }
  
  public String getImageName( ) {
    return "go-" + this.name( ).toLowerCase( ) + ".png";
  }
  
  public abstract Integer apply( AccountingControl parent );
}
