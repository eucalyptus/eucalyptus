package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DockPanel;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.UserGroupEntityPanel;
import edu.ucsb.eucalyptus.admin.client.UserGroupPropertyPanel;

public class AccountingPanel extends DockPanel {

  private ReportControlPanel controlPanel;
  private ReportFilterPanel filterPanel;
  private ReportDisplayPanel reportPanel;
  private AccountingControl parent;
  private static final String MAIN_STYLE_NAME = "euca-AccountingPanel";
  public AccountingPanel( AccountingControl parent ) {
    super( );
    this.parent = parent;
    this.setSpacing(8);
    this.setHorizontalAlignment(DockPanel.ALIGN_CENTER);
    this.setVerticalAlignment(DockPanel.ALIGN_TOP);
    this.addStyleName(MAIN_STYLE_NAME);
    
    this.controlPanel = new ReportControlPanel(parent);
    this.add(controlPanel, DockPanel.WEST);
    this.setCellWidth(controlPanel, "20%");
    // User panel
    this.filterPanel = new ReportFilterPanel(parent);
    this.add(filterPanel, DockPanel.WEST);
    this.setCellWidth(filterPanel, "30%");
    // Property panel
    this.reportPanel = new ReportDisplayPanel(parent);
    this.add(reportPanel, DockPanel.CENTER);
    this.setCellWidth(reportPanel, "50%");

  }
  
  

}
