package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;

import java.util.List;
import java.util.Map;

public class UserGroupTabPanel extends DockPanel {
	
	private static final String MAIN_STYLE_NAME = "euca-UserGroupTabPanel";
	
	private Button addGroupButton;	
	private Button addUserButton;
	private UserGroupEntityPanel groupPanel;
	private UserGroupEntityPanel userPanel;
	private UserGroupPropertyPanel propertyPanel;
	
	private UserGroupControl control;
	
	UserGroupTabPanel(UserGroupControl control,
			List<String> groupColumns, List<String> userColumns) {
		super();
		
		this.control = control;
		
		this.setSpacing(8);
		this.setHorizontalAlignment(DockPanel.ALIGN_CENTER);
		this.setVerticalAlignment(DockPanel.ALIGN_TOP);
		this.addStyleName(MAIN_STYLE_NAME);
		
		// Group panel
		this.groupPanel = new UserGroupEntityPanel("group", control, groupColumns,
				new ClickHandler() {
					public void onClick(ClickEvent event) {
						UserGroupTabPanel.this.control.displayAddGroupUI();
					}
				});
		this.add(groupPanel, DockPanel.WEST);
		this.setCellWidth(groupPanel, "20%");
		// User panel
		this.userPanel = new UserGroupEntityPanel("user", control, userColumns,
				new ClickHandler() {
					public void onClick(ClickEvent event) {
						UserGroupTabPanel.this.control.displayAddUserUI();
					}
				});
		this.add(userPanel, DockPanel.WEST);
		this.setCellWidth(userPanel, "30%");
		// Property panel
		this.propertyPanel = new UserGroupPropertyPanel(control);
		this.add(propertyPanel, DockPanel.CENTER);
		this.setCellWidth(propertyPanel, "50%");
	}
	
	public UserGroupEntityPanel getGroupPanel() {
		return this.groupPanel;
	}
	
	public UserGroupEntityPanel getUserPanel() {
		return this.userPanel;
	}
	
	public UserGroupPropertyPanel getPropertyPanel() {
		return this.propertyPanel;
	}
}
