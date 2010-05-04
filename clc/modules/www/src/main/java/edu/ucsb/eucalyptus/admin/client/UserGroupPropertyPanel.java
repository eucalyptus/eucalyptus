package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HTMLTable.ColumnFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserGroupPropertyPanel extends VerticalPanel {
	
	public static final String DEFAULT_HEADER = "Property";
	
	private static final String HEADER_STYLE_NAME = "euca-UserGroupTabPanel-header";
	private static final String SCROLL_STYLE_NAME = "euca-UserGroupPropertyPanel-scroll";
	private static final String CONTENT_STYLE_NAME = "euca-UserGroupPropertyPanel-content";
	private static final String CONTENT_TITLE_STYLE_NAME = "euca-UserGroupPropertyPanel-content-title";
	private static final String CONTENT_SUBTITLE_STYLE_NAME = "euca-UserGroupPropertyPanel-content-subtitle";
	private static final String CONTENT_DETAIL_STYLE_NAME = "euca-UserGroupPropertyPanel-content-detail";
	private static final String CONTENT_STATUS_STYLE_NAME = "euca-UserGroupPropertyPanel-status";
	private static final String CONTENT_STATUS_ERROR_STYLE_NAME = "euca-UserGroupPropertyPanel-status-error";
	private static final String ACTION_BAR_STYLE_NAME = "euca-UserGroupPropertyPanel-action";
	private static final String DATA_STYLE_NAME = "euca-UserGroupPropertyPanel-data";
	private static final String DATA_NAME_STYLE_NAME = "euca-UserGroupPropertyPanel-data-name";
	private static final String DATA_REQUIRED_STYLE_NAME = "euca-UserGroupPropertyPanel-data-required";
	private static final String DATA_VALUE_STYLE_NAME = "euca-UserGroupPropertyPanel-data-value";
	private static final String DATA_TEXT_STYLE_NAME = "euca-UserGroupPropertyPanel-data-text";
	private static final String DATA_LIST_STYLE_NAME = "euca-UserGroupPropertyPanel-data-list";
	private static final String MAIN_STYLE_NAME = "euca-UserGroupPropertyPanel";
	
	private static final int MAX_GROUP_DISPLAY_SIZE = 64;
	private static final int MAX_USER_DISPLAY_SIZE = 64;
	// Time to show status bar
	private static final int STATUS_DELAY_IN_MILLIS = 10000;
	
	private Label header;
	private VerticalPanel content;
	
	private Label status;
	private Timer statusTimer;
	
	private HTML subtitle;
	
	private UserGroupControl control;
	
	UserGroupPropertyPanel(UserGroupControl control) {
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
				remove(status);
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
	
	public void setHeaderText(String title) {
		this.header.setText(title);
	}
	
	public void showStatus(String text, boolean isError) {
		if (isError) {
			this.status.addStyleName(CONTENT_STATUS_ERROR_STYLE_NAME);
		} else {
			this.status.removeStyleName(CONTENT_STATUS_ERROR_STYLE_NAME);
		}
		this.status.setText(text);
		if (!this.getChildren().contains(status)) {
			this.insert(status, 1);
			// Tightening the header space
			this.setCellHeight(status, "20px");
		}
		this.statusTimer.cancel();
		this.statusTimer.schedule(STATUS_DELAY_IN_MILLIS);
	}
	
	private HTML addHtml(String html, String style) {
		HTML w = new HTML(html);
		if (style != null) {
			w.addStyleName(style);
		}
		this.content.add(w);
		return w;
	}
	
	private void cleanup() {
		if (this.status != null) {
			this.remove(this.status);
		}
		this.content.clear();
	}
	
	private void addTitle(String title) {
		addHtml(title, CONTENT_TITLE_STYLE_NAME);
	}
	
	private void addSubtitle(String sub) {
		this.subtitle = addHtml(sub, CONTENT_SUBTITLE_STYLE_NAME);
	}
	
	private void addSeparator() {
		addHtml("<br>", CONTENT_DETAIL_STYLE_NAME);
	}
	
	private void addHtmlContent(String content) {
		addHtml(content, CONTENT_DETAIL_STYLE_NAME);
	}
	
	private HorizontalPanel addActionBar() {
		HorizontalPanel actionBar = new HorizontalPanel();
		actionBar.setHorizontalAlignment(ALIGN_CENTER);
		actionBar.addStyleName(ACTION_BAR_STYLE_NAME);
		this.content.add(actionBar);
		return actionBar;
	}
	
	public void setSubtitle(String sub) {
		this.subtitle.setHTML(sub);
	}
	
	private void addDataRow(Grid grid, int row, String name, String value) {
		CellFormatter cellFormatter = grid.getCellFormatter();
		if (name != null) {
			grid.setText(row, 0, name);
			cellFormatter.addStyleName(row, 0, DATA_NAME_STYLE_NAME);
		}
		if (value != null) {
			grid.setText(row, 1, value);
			cellFormatter.addStyleName(row, 1, DATA_VALUE_STYLE_NAME);
		}
	}
	
	private Grid addDataGrid(int rows) {
		Grid grid = new Grid(rows, 2);
		grid.addStyleName(DATA_STYLE_NAME);
		ColumnFormatter colFormatter = grid.getColumnFormatter();
		colFormatter.setWidth(0, "40%");
		colFormatter.setWidth(1, "60%");
		this.content.add(grid);
		return grid;
	}
	
	public void showGroup(GroupInfoWeb group) {
		this.cleanup();
		setHeaderText("Group: " + group.name);		
		HorizontalPanel action = addActionBar();
		action.add(new Button("Edit", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displayEditGroupUI();
			}
		}));
		action.add(new Button("Delete", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displayDeleteGroupUI();
			}
		}));
		addTitle("\"" + group.name + "\" group");
		addSubtitle(" ");
		addSeparator();		
		Grid grid = addDataGrid(1);
		addDataRow(grid, 0, "Availability Zone", group.zone);
	}
	
	public void showGroups(List<GroupInfoWeb> groups) {
		this.cleanup();
		setHeaderText("Groups: " + groups.size());		
		HorizontalPanel action = addActionBar();
		action.add(new Button("Delete", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displayDeleteGroupsUI();
			}
		}));
		addTitle(groups.size() + " groups");
		addSubtitle(" ");
		addSeparator();
		Grid grid = addDataGrid(1);
		addDataRow(grid, 0, "Selected Groups", 
				UserGroupUtils.getListString(
						UserGroupUtils.getGroupNamesFromGroups(groups), MAX_GROUP_DISPLAY_SIZE));
	}
	
	private String getBooleanString(Boolean value) {
		if (value == null) {
			return "no";
		}
		return value ? "yes" : "no";
	}
	
	public Grid showUser(UserInfoWeb user) {
		this.cleanup();
		setHeaderText("User: " + user.getRealName());		
		HorizontalPanel action = addActionBar();
		action.add(new Button("Edit", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displayEditUserUI();
			}
		}));
		// admin can not delete another admin
		if (!user.isAdministrator()) {
			action.add(new Button("Delete", new ClickHandler() {
				public void onClick(ClickEvent event) {
					control.displayDeleteUserUI();
				}
			}));
		}
		addTitle(user.getRealName());
		addSeparator();		
		Grid grid = addDataGrid(11);
		int i = 0;
		addDataRow(grid, i++, "User ID", user.getUserName());
		addDataRow(grid, i++, "Administrator", getBooleanString(user.isAdministrator()));
		addDataRow(grid, i++, "Email", user.getEmail());
		addDataRow(grid, i++, "Confirmed", getBooleanString(user.isConfirmed()));
		addDataRow(grid, i++, "Phone", user.getTelephoneNumber());
		addDataRow(grid, i++, "Affiliation", user.getAffiliation());
		addDataRow(grid, i++, "Project PI", user.getProjectPIName());
		addDataRow(grid, i++, "Project Description", user.getProjectDescription());
		addDataRow(grid, i++, "Approved", getBooleanString(user.isApproved()));
		addDataRow(grid, i++, "Enabled", getBooleanString(user.isEnabled()));
		addDataRow(grid, i++, "Groups", "");
		
		return grid;
	}
	
	public void showUsers(List<UserInfoWeb> users) {
		this.cleanup();
		setHeaderText("Users: " + users.size());		
		HorizontalPanel action = addActionBar();
		action.add(new Button("Add to", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displayAddUsersToGroupsUI();
			}
		}));
		action.add(new Button("Remove from", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displayRemoveUsersFromGroupsUI();
			}
		}));
		action.add(new Button("Delete", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displayDeleteUsersUI();
			}
		}));
		addTitle(users.size() + " users");
		addSeparator();
		Grid grid = addDataGrid(1);
		addDataRow(grid, 0, "Selected Users", UserGroupUtils.getListString(
				UserGroupUtils.getUserNamesFromUsers(users), MAX_USER_DISPLAY_SIZE));
	}
	
	public void showEmptyPrompt() {
		this.cleanup();
		setHeaderText(DEFAULT_HEADER);
		addTitle("No group or user selected");
		addSeparator();
		addHtmlContent("Select groups or users to display their details and operations");
	}
	
	private void setNameColumn(Grid grid, int row, String name, boolean required) {
		CellFormatter cellFormatter = grid.getCellFormatter();
		grid.setText(row, 0, name);
		if (required) {
			cellFormatter.addStyleName(row, 0, DATA_REQUIRED_STYLE_NAME);
		} else {
			cellFormatter.addStyleName(row, 0, DATA_NAME_STYLE_NAME);
		}
	}
	
	private TextBox addTextBoxRow(Grid grid, int row, String name, String value, boolean required) {
		setNameColumn(grid, row, name, required);
		TextBox input = new TextBox();
		if (value != null) {
			input.setText(value);
		}
		input.addStyleName(DATA_TEXT_STYLE_NAME);
		grid.setWidget(row, 1, input);
		return input;
	}
	
	private CheckBox addCheckBoxRow(Grid grid, int row, String name, boolean value, boolean required) {
		setNameColumn(grid, row, name, required);
		CheckBox input = new CheckBox();
		input.setValue(value);
		grid.setWidget(row, 1, input);
		return input;
	}
	
	private PasswordTextBox addPasswordRow(Grid grid, int row, String name, String value, boolean required) {
		setNameColumn(grid, row, name, required);
		PasswordTextBox input = new PasswordTextBox();
		if (value != null) {
			input.setText(value);
		}
		input.addStyleName(DATA_TEXT_STYLE_NAME);
		grid.setWidget(row, 1, input);
		return input;
	}
	
	private TextArea addTextAreaRow(Grid grid, int row, String name, String value,
			boolean required) {
		setNameColumn(grid, row, name, required);
		TextArea input = new TextArea();
		if (value != null) {
			input.setText(value);
		}
		input.setVisibleLines(3);
		input.addStyleName(DATA_TEXT_STYLE_NAME);
		grid.setWidget(row, 1, input);
		return input;
	}

	private ListBox addListRow(Grid grid, int row, String name, List<String> values,
			List<String> selected, boolean required) {
		setNameColumn(grid, row, name, required);
		ListBox input = new ListBox(true);
		input.setVisibleItemCount(5);
		if (values != null) {
			for (String value : values) {
				input.addItem(value);
			}
		}
		if (selected != null) {
			Set<String> selectedSet = new HashSet<String>(selected);
			for (int i = 0; i < values.size(); i++) {
				if (selectedSet.contains(input.getItemText(i))) {
					input.setItemSelected(i, true);
				} else {
					input.setItemSelected(i, false);
				}
			}
		}
		input.addStyleName(DATA_LIST_STYLE_NAME);
		grid.setWidget(row, 1, input);
		return input;
	}
	
	public void showAddGroup() {
		this.cleanup();
		setHeaderText("Adding group");
		HorizontalPanel action = addActionBar();
		addTitle("Add a new group");
		addSubtitle("<em style='color:red;'>Red</em> fields are required");
		addSeparator();
		Grid grid = addDataGrid(2);
		int i = 0;
		final TextBox nameBox = addTextBoxRow(grid, i++, "Name", "", true);
		final TextBox zoneBox = addTextBoxRow(grid, i++, "Availability Zone", "", false);
		action.add(new Button("Finish", new ClickHandler() {
			public void onClick(ClickEvent event) {
				GroupInfoWeb group = new GroupInfoWeb();
				group.name = nameBox.getText();
				group.zone = zoneBox.getText();
				control.addGroup(group);
			}
		}));
		action.add(new Button("Cancel", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displaySelectedUsers();
			}
		}));
	}
	
	public void showEditGroup(final GroupInfoWeb group) {
		this.cleanup();
		setHeaderText("Editing " + group.name);
		HorizontalPanel action = addActionBar();
		addTitle("Edit <i>" + group.name + "</i> group");
		addSeparator();
		Grid grid = addDataGrid(1);
		final TextBox zoneBox = addTextBoxRow(grid, 0, "Availability Zone", group.zone, false);
		action.add(new Button("Finish", new ClickHandler() {
			public void onClick(ClickEvent event) {
				GroupInfoWeb newGroup = new GroupInfoWeb();
				newGroup.name = group.name;
				newGroup.zone = zoneBox.getText();
				control.updateGroup(newGroup);
			}
		}));
		action.add(new Button("Cancel", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displaySelectedUsers();
			}
		}));
	}
	
	public void showDeleteGroup(final GroupInfoWeb group) {
		this.cleanup();
		setHeaderText("Deleting " + group.name);
		HorizontalPanel action = addActionBar();
		addSeparator();
		addHtmlContent("Are you sure to delete group <b> " + group.name + "</b>?");
		action.add(new Button("Yes", new ClickHandler() {
			public void onClick(ClickEvent event) {
				List<String> groupNames = new ArrayList<String>();
				groupNames.add(group.name);
				control.deleteGroups(groupNames);
			}
		}));
		action.add(new Button("No", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displaySelectedUsers();
			}
		}));
	}
	
	public void showDeleteGroups(final List<GroupInfoWeb> groups) {
		final List<String> groupNames = UserGroupUtils.getGroupNamesFromGroups(groups);
		this.cleanup();
		setHeaderText("Deleting " + groups.size() + " groups");
		HorizontalPanel action = addActionBar();
		addSeparator();
		addHtmlContent("Are you sure to delete groups: <b> " + 
				UserGroupUtils.getListString(groupNames, 0) + "</b>?");
		action.add(new Button("Yes", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.deleteGroups(groupNames);
			}
		}));
		action.add(new Button("No", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displaySelectedUsers();
			}
		}));
	}
	
	private List<String> getListSelectedItems(ListBox list) {
		List<String> items = new ArrayList<String>();
		for (int i = 0; i < list.getItemCount(); i++) {
			if (list.isItemSelected(i)) {
				items.add(list.getItemText(i));
			}
		}
		return items;
	}
	
	public void showAddUser(final List<String> groupNames, final List<String> selectedGroupNames) {
		this.cleanup();
		setHeaderText("Adding user");
		HorizontalPanel action = addActionBar();
		addTitle("Add a new user");
		addSubtitle("<em style='color:red;'>Red</em> fields are required");
		addSeparator();
		Grid grid = addDataGrid(12);
		int i = 0;
		final TextBox userName = addTextBoxRow(grid, i++, "User Name", "",
				true /* required */);
		final CheckBox admin = addCheckBoxRow(grid, i++, "Administrator", false,
				false /* required */);
		final PasswordTextBox password = addPasswordRow(grid, i++, "Password", "",
				true /* required */);
		final PasswordTextBox password2 = addPasswordRow(grid, i++, "Retype Password", "",
				true /* required */);
		final TextBox fullName = addTextBoxRow(grid, i++, "Full Name", "",
				true /* required */);
		final TextBox email = addTextBoxRow(grid, i++, "Email", "",
				true /* required */);
		final CheckBox skipConfirmation = addCheckBoxRow(grid, i++, "Skip Email Confirmation",
				false, false /* required */);
		final TextBox phone = addTextBoxRow(grid, i++, "Phone Number", "",
				false /* required */);
		final TextBox affiliation = addTextBoxRow(grid, i++, "Affiliation", "",
				false /* required */);
		final TextBox pi = addTextBoxRow(grid, i++, "Project Leader", "",
				false /* required */);
		final TextArea description = addTextAreaRow(grid, i++, "Project Description", "",
				false /* required */);
		final ListBox groups = addListRow(grid, i++, "Groups", groupNames, selectedGroupNames,
				false /* required */);
		action.add(new Button("Finish", new ClickHandler() {
			public void onClick(ClickEvent event) {
				// Verify validity of input
				String userNameValue = userName.getText();
				int length = userNameValue.length();
				if (length <= 0) {
					showStatus("User name can't be empty.", true /* isError */);
					return;
				}
				if (length > 30) {
					showStatus("User name length can't be larger than 30 characters",
							true /* isError */);
					return;
				}
				if (userNameValue.matches(".*[^\\w\\-\\.@]+.*")) {
					showStatus("User name contains invalid characters.", true /* isError */);
					return;
				}
				String passwordValue = password.getText();
				String password2Value = password2.getText();
				if (!passwordValue.equals(password2Value)) {
					showStatus("Passwords do not match.", true /* isError */);
					return;
				}
				if (passwordValue.toLowerCase().contains(userNameValue.toLowerCase())) {
					showStatus("Password can not contain user name", true /* isError */);
					return;
				}
				if (passwordValue.length() < UserGroupControl.MIN_PASSWORD_LENGTH) {
					showStatus("Password must have at least " +
							UserGroupControl.MIN_PASSWORD_LENGTH + " characters.",
							true /* isError */);
					return;
				}
				String fullNameValue = fullName.getText();
				if (fullNameValue.length() <= 0) {
					showStatus("Full name can not be empty.", true /* isError */);
					return;
				}
				String emailValue = email.getText();
				if (emailValue.length() <= 0) {
					showStatus("Email can not be empty.", true /* isError */);
					return;
				}
				// Add user remotely
				UserInfoWeb user = new UserInfoWeb();
				user.setUserName(userNameValue);
				user.setAdministrator(admin.getValue());
				user.setPassword(GWTUtils.md5(passwordValue));
				user.setRealName(fullNameValue);
				user.setEmail(emailValue);
				user.setConfirmed(skipConfirmation.getValue());
				user.setTelephoneNumber(phone.getText());
				user.setAffiliation(affiliation.getText());
				user.setProjectPIName(pi.getText());
				user.setProjectDescription(description.getText());
				control.addUser(user, getListSelectedItems(groups));
			}
		}));
		action.add(new Button("Cancel", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displaySelectedUsers();
			}
		}));
	}
	
	public void showEditUser(final UserInfoWeb user, final List<String> groupNames,
			final List<String> selectedGroupNames) {
		this.cleanup();
		setHeaderText("Editing " + user.getUserName());
		HorizontalPanel action = addActionBar();
		addTitle("Edit user <i>" + user.getUserName() + "</i>");
		addSeparator();
		Grid grid = addDataGrid(11);
		int i = 0;
		final CheckBox admin = addCheckBoxRow(grid, i++, "Administrator", user.isAdministrator(),
				false /* required */);
		final PasswordTextBox password = addPasswordRow(grid, i++, "Password", user.getPassword(),
				false /* required */);
		final PasswordTextBox password2 = addPasswordRow(grid, i++, "Retype Password",
				user.getPassword(), false /* required */);
		// admin can not change another admin's password
		if (user.isAdministrator()) {
			password.setEnabled(false);
			password2.setEnabled(false);
		}
		final TextBox fullName = addTextBoxRow(grid, i++, "Full Name", user.getRealName(),
				false /* required */);
		final TextBox email = addTextBoxRow(grid, i++, "Email", user.getEmail(),
				false /* required */);
		final CheckBox skipConfirmation;
		if (!user.isConfirmed()) {
			skipConfirmation = addCheckBoxRow(grid, i++, "Skip Email Confirmation", false,
					false /* required */);
		} else {
			skipConfirmation = null;
		}
		final TextBox phone = addTextBoxRow(grid, i++, "Phone Number", user.getTelephoneNumber(),
				false /* required */);
		final TextBox affiliation = addTextBoxRow(grid, i++, "Affiliation", user.getAffiliation(),
				false /* required */);
		final TextBox pi = addTextBoxRow(grid, i++, "Project Leader", user.getProjectPIName(),
				false /* required */);
		final TextArea description = addTextAreaRow(grid, i++, "Project Description",
				user.getProjectDescription(), false /* required */);
		final ListBox groups = addListRow(grid, i++, "Groups", groupNames, selectedGroupNames,
				false /* required */);
		action.add(new Button("Finish", new ClickHandler() {
			public void onClick(ClickEvent event) {
				// Verify validity of input
				String passwordValue = password.getText();
				String password2Value = password2.getText();
				if (!passwordValue.equals(password2Value)) {
					showStatus("Passwords do not match.", true /* isError */);
					return;
				}
				if (passwordValue.toLowerCase().contains(user.getUserName().toLowerCase())) {
					showStatus("Password can not contain user name", true /* isError */);
					return;
				}
				if (passwordValue.length() < UserGroupControl.MIN_PASSWORD_LENGTH) {
					showStatus("Password must have at least " +
							UserGroupControl.MIN_PASSWORD_LENGTH + " characters.",
							true /* isError */);
					return;
				}
				String fullNameValue = fullName.getText();
				if (fullNameValue.length() <= 0) {
					showStatus("Full name can not be empty.", true /* isError */);
					return;
				}
				String emailValue = email.getText();
				if (emailValue.length() <= 0) {
					showStatus("Email can not be empty.", true /* isError */);
					return;
				}
				// Add user remotely
				UserInfoWeb updatedUser = new UserInfoWeb();
				updatedUser.setUserName(user.getUserName());
				updatedUser.setAdministrator(admin.getValue());
				updatedUser.setPassword(GWTUtils.md5(passwordValue));
				updatedUser.setRealName(fullNameValue);
				updatedUser.setEmail(emailValue);
				if (skipConfirmation == null) {
					updatedUser.setConfirmed(user.isConfirmed());
				} else {
					updatedUser.setConfirmed(skipConfirmation.getValue());
				}
				updatedUser.setTelephoneNumber(phone.getText());
				updatedUser.setAffiliation(affiliation.getText());
				updatedUser.setProjectPIName(pi.getText());
				updatedUser.setProjectDescription(description.getText());
				
				List<String> addToGroups = new ArrayList<String>();
				for (int i = 0; i < groups.getItemCount(); i++) {
					if (groups.isItemSelected(i)) {
						addToGroups.add(groups.getItemText(i));
					}
				}
				control.updateUser(updatedUser, addToGroups);
			}
		}));
		action.add(new Button("Cancel", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displaySelectedUsers();
			}
		}));
	}
	
	public void showDeleteUser(final UserInfoWeb user) {
		this.cleanup();
		setHeaderText("Deleting " + user.getRealName());
		HorizontalPanel action = addActionBar();
		addSeparator();
		addHtmlContent("Are you sure to delete user <b> " + user.getRealName() + "</b> (" +
				user.getUserName() + ")");
		action.add(new Button("Yes", new ClickHandler() {
			public void onClick(ClickEvent event) {
				List<String> userNames = new ArrayList<String>();
				userNames.add(user.getUserName());
				control.deleteGroups(userNames);
			}
		}));
		action.add(new Button("No", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displaySelectedUsers();
			}
		}));
	}
	
	public void showDeleteUsers(final List<UserInfoWeb> users) {
		final List<String> userNames = UserGroupUtils.getUserNamesFromUsers(users);
		this.cleanup();
		setHeaderText("Deleting " + users.size() + " users");
		HorizontalPanel action = addActionBar();
		addSeparator();
		addHtmlContent("Are you sure to delete users <b> " + 
				UserGroupUtils.getListString(userNames, 0) + "</b>?");
		action.add(new Button("Yes", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.deleteUsers(userNames);
			}
		}));
		action.add(new Button("No", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displaySelectedUsers();
			}
		}));
	}
	
	public void showAddUsersToGroups(final List<UserInfoWeb> users, 
			final List<String> groupNames) {
		final List<String> userNames = UserGroupUtils.getUserNamesFromUsers(users);
		this.cleanup();
		setHeaderText("Adding users to groups");
		HorizontalPanel action = addActionBar();
		addTitle("Add " + users.size() + " users to selected groups");
		addSeparator();
		Grid grid = addDataGrid(1);
		int i = 0;
		final ListBox groups = addListRow(grid, i++, "Groups", groupNames, null,
				false /* required */);
		action.add(new Button("Yes", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.addUsersToGroups(userNames, getListSelectedItems(groups));
			}
		}));
		action.add(new Button("No", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displaySelectedUsers();
			}
		}));
	}
	
	public void showRemoveUsersFromGroups(final List<UserInfoWeb> users, 
			final List<String> groupNames, final List<String> selectedGroupNames) {
		final List<String> userNames = UserGroupUtils.getUserNamesFromUsers(users);
		this.cleanup();
		setHeaderText("Removing users from groups");
		HorizontalPanel action = addActionBar();
		addTitle("Remove " + users.size() + " users from selected groups");
		addSeparator();
		Grid grid = addDataGrid(1);
		int i = 0;
		final ListBox groups = addListRow(grid, i++, "Groups", groupNames, selectedGroupNames,
				false /* required */);
		action.add(new Button("Yes", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.removeUsersFromGroups(userNames, getListSelectedItems(groups));
			}
		}));
		action.add(new Button("No", new ClickHandler() {
			public void onClick(ClickEvent event) {
				control.displaySelectedUsers();
			}
		}));
	}
}