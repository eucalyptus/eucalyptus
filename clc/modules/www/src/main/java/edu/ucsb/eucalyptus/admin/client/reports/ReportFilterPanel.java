package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;

public class ReportFilterPanel extends VerticalPanel {
  public static final String  DEFAULT_HEADER                  = "Property";
  
  private static final String HEADER_STYLE_NAME               = "euca-AccountingTabPanel-header";
  private static final String SCROLL_STYLE_NAME               = "euca-AccountingPropertyPanel-scroll";
  private static final String CONTENT_STYLE_NAME              = "euca-AccountingPropertyPanel-content";
  private static final String CONTENT_TITLE_STYLE_NAME        = "euca-AccountingPropertyPanel-content-title";
  private static final String CONTENT_SUBTITLE_STYLE_NAME     = "euca-AccountingPropertyPanel-content-subtitle";
  private static final String CONTENT_DETAIL_STYLE_NAME       = "euca-AccountingPropertyPanel-content-detail";
  private static final String CONTENT_STATUS_STYLE_NAME       = "euca-AccountingPropertyPanel-status";
  private static final String CONTENT_STATUS_ERROR_STYLE_NAME = "euca-AccountingPropertyPanel-status-error";
  private static final String ACTION_BAR_STYLE_NAME           = "euca-AccountingPropertyPanel-action";
  private static final String DATA_STYLE_NAME                 = "euca-AccountingPropertyPanel-data";
  private static final String DATA_NAME_STYLE_NAME            = "euca-AccountingPropertyPanel-data-name";
  private static final String DATA_ALT_BG_STYLE_NAME          = "euca-AccountingPropertyPanel-data-altbg";
  private static final String DATA_VALUE_STYLE_NAME           = "euca-AccountingPropertyPanel-data-value";
  private static final String DATA_TEXT_STYLE_NAME            = "euca-AccountingPropertyPanel-data-text";
  private static final String DATA_LIST_STYLE_NAME            = "euca-AccountingPropertyPanel-data-list";
  private static final String MAIN_STYLE_NAME                 = "euca-AccountingPropertyPanel";
  
  private static final int    MAX_GROUP_DISPLAY_SIZE          = 64;
  private static final int    MAX_USER_DISPLAY_SIZE           = 64;
  // Time to show status bar
  private static final int    STATUS_DELAY_IN_MILLIS          = 10000;
  private Label header;
  private VerticalPanel content;

  private Label status;
  private Timer statusTimer;

  private HTML subtitle;
  private AccountingControl control;

  ReportFilterPanel(AccountingControl control) {
    super();

    this.control = control;

    header = new Label();
    header.addStyleName(HEADER_STYLE_NAME);
    this.add(header);
    // Tightening the header space
    this.setCellHeight(header, "20px");

    this.status = new Label();
    this.status.addStyleName(CONTENT_STATUS_STYLE_NAME);
    this.statusTimer = new Timer() {
      public void run() {
        content.remove(status);
      }
    };

    ScrollPanel scroll = new ScrollPanel();
    scroll.addStyleName(SCROLL_STYLE_NAME);
    content = new VerticalPanel();
    //content.setBorderWidth(2);
    content.addStyleName(CONTENT_STYLE_NAME);
    scroll.add(content);
    this.add(scroll);

    this.setSpacing(0);
    //this.setBorderWidth(2);
    this.addStyleName(MAIN_STYLE_NAME);
  }
  
}
