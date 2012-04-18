package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.Set;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.IsWidget;

public interface ConfirmationView extends IsWidget {

  void display( String caption, String subject, Set<SearchResultRow> list, ArrayList<Integer> fields );
  
  void display( String caption, String subject, SafeHtml html );
  
  void setPresenter( Presenter presenter );
  
  public interface Presenter {
    
    void confirm( String subject );
    
  }
  
}
