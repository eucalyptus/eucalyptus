package edu.ucsb.eucalyptus.admin.client.reports;

import java.util.List;
import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.ColumnFormatter;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.EucaButton;

public class ReportControlPanel extends VerticalPanel {
  
  public interface SelectionChangeHandler {
    public void onSelectionChange();
  }
  
  public static final String DEFAULT_GROUP_HEADER = "Groups";
  public static final String DERAULT_USER_HEADER = "Users";
  
  private static final String HEADER_STYLE_NAME = "euca-UserGroupTabPanel-header";
  private static final String ACTION_BAR_STYLE_NAME = "euca-ReportparentPanel-action";
  private static final String FILTER_STYLE_NAME = "euca-ReportparentPanel-filter";
  private static final String SCROLL_STYLE_NAME = "euca-ReportparentPanel-scroll";
  private static final String LIST_HEADER_STYLE_NAME = "euca-ReportparentPanel-list-header";
  private static final String ANCHOR_STYLE_NAME = "euca-ReportparentPanel-anchor";
  private static final String MAIN_STYLE_NAME = "euca-ReportparentPanel";
  
  private String name;
  
  private Label header;
  private TextBox filter;
  
  private List<String> columns;
  
  private String filterString;
  
  private int sortColumn;
  private boolean sortInc;
  
  private AccountingControl parent;
  
  ReportControlPanel(AccountingControl parent) {
    super();
    ClickHandler addHandler = new ClickHandler( ) {
      
      @Override
      public void onClick( ClickEvent clickevent ) {}
    };
    List<String> cols = Lists.newArrayList( "yhalothar", "kkthxbye" );
    this.name = name;
    this.parent = parent;
    this.columns = cols;
    
    ScrollPanel scroll = new ScrollPanel();
    scroll.setStyleName(SCROLL_STYLE_NAME);
    
    header = new Label();
    header.addStyleName(HEADER_STYLE_NAME);
    
    Widget actionBar = createActionBar(addHandler);
    Widget listHeader = createListHeader(cols);
    
    // Order matters
    this.add(header);
    this.add(actionBar);
    this.add(listHeader);
    this.add(scroll);

    // Tightening the header space
    this.setCellHeight(header, "20px");
    this.setCellHeight(actionBar, "32px");
    this.setCellHeight(listHeader, "20px");

    this.setSpacing(0);
    this.addStyleName(MAIN_STYLE_NAME);
  }
  
  public void setHeaderText(String title) {
    this.header.setText(title);
  }
  
  protected Widget createListHeader(List<String> cols) {
    int colSize = cols.size();
    Grid headerGrid = new Grid(1, colSize + 1);
    headerGrid.addStyleName(LIST_HEADER_STYLE_NAME);
    headerGrid.setCellPadding(1);
    headerGrid.setCellSpacing(0);
    ColumnFormatter colFormatter = headerGrid.getColumnFormatter();
    colFormatter.setWidth(0, "10px");
    CheckBox checkbox = new CheckBox();
    headerGrid.setWidget(0, 0, checkbox);
    for (int i = 0; i < colSize; i++) {
      final int col = i;
      Anchor anchor = new Anchor(cols.get(i));
      anchor.addStyleName(ANCHOR_STYLE_NAME);
      anchor.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
        }
      });
      headerGrid.setWidget(0, col + 1, anchor);
    }
    return headerGrid;
  }
  
  protected Widget createActionBar(ClickHandler handler) {
    HorizontalPanel actionBar = new HorizontalPanel();
    actionBar.addStyleName(ACTION_BAR_STYLE_NAME);
    actionBar.setHorizontalAlignment(ALIGN_CENTER);
    actionBar.setVerticalAlignment(ALIGN_MIDDLE);

    filter = new TextBox();
    filter.addKeyPressHandler(new KeyPressHandler() {
      public void onKeyPress(KeyPressEvent event) {
        if (event.getCharCode() == KeyCodes.KEY_ENTER) {
        }
      }
    });
    filter.addStyleName(FILTER_STYLE_NAME);
    
    Button addButton = new EucaButton("Add", "Add new " + this.name, handler);
    Button filterButton = new EucaButton("Filter", "Filter " + this.name, new ClickHandler() {
      public void onClick(ClickEvent event) {}
    });
    
    actionBar.add(filter);
    actionBar.add(filterButton);
    actionBar.add(addButton);
    
    actionBar.setCellWidth(filterButton, "15px");
    actionBar.setCellWidth(addButton, "15px");
    
    return actionBar;
  }
}
