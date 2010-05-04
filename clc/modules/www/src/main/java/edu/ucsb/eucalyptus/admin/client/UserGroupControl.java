package edu.ucsb.eucalyptus.admin.client;

import edu.ucsb.eucalyptus.admin.client.UserGroupEntityList.DataRow;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserGroupControl {
	
	private class GroupRow implements DataRow {
		private GroupInfoWeb group;
		GroupRow(GroupInfoWeb group) {
			this.group = group;
		}
		public String get(int col) {
			switch (col) {
			case 0:
				return group.name;
			case 1:
				return group.zone;
			}
			return "";
		}
		public String toTooltip() {
			return group.name + "@" + group.zone;
		}
		public GroupInfoWeb getGroupInfo() {
			return this.group;
		}
	}
	
	private class UserRow implements DataRow {
		private UserInfoWeb user;
		UserRow(UserInfoWeb user) {
			this.user = user;
		}
		public String get(int col) {
			switch (col) {
			case 0:
				return user.getRealName();
			case 1:
				return user.getUserName();
			case 2:
				return user.getAffiliation();
			}
			return "";
		}
		public String toTooltip() {
			return user.getRealName();
		}
		public UserInfoWeb getUserInfo() {
			return this.user;
		}
	}

	public static final int MIN_PASSWORD_LENGTH = 5;
	
	private UserGroupTabPanel rootPanel;
	
	private List<DataRow> groups;
	private List<DataRow> users;
	
	private String sessionId;
	
	private List<GroupInfoWeb> selectedGroups;
	private List<UserInfoWeb> selectedUsers;
	
	UserGroupControl(String sessionId) {
		this.sessionId = sessionId;
		
		groups = new ArrayList<DataRow>();
		users = new ArrayList<DataRow>();
		
		selectedGroups = new ArrayList<GroupInfoWeb>();
		selectedUsers = new ArrayList<UserInfoWeb>();
		
		List<String> groupColumns = new ArrayList<String>();
		groupColumns.add("Name");
		groupColumns.add("Zone");
		
		List<String> userColumns = new ArrayList<String>();
		userColumns.add("Name");
		userColumns.add("ID");
		userColumns.add("Affiliation");
		
		rootPanel = new UserGroupTabPanel(this, groupColumns, userColumns);
	}
	
	public void onSelectionChange(UserGroupEntityPanel source) {
		if (source == rootPanel.getGroupPanel()) {
			displaySelectedGroups();
		} else if (source == rootPanel.getUserPanel()){
			displaySelectedUsers();
		}
	}
	
	public UserGroupTabPanel getRootPanel() {
		return this.rootPanel;
	}
	
	private void refillGroups(List<GroupInfoWeb> list) {
		groups.clear();
		for (GroupInfoWeb group : list) {
			groups.add(new GroupRow(group));
		}
	}
	
	private void refillUsers(List<UserInfoWeb> list) {
		users.clear();
		for (UserInfoWeb user : list) {
			users.add(new UserRow(user));
		}
	}
	
	public void display() {
		groups.clear();
		users.clear();
		
		// Initialize the UI
		rootPanel.getGroupPanel().setHeaderText(UserGroupEntityPanel.DEFAULT_GROUP_HEADER);
		rootPanel.getGroupPanel().display(groups);
		rootPanel.getUserPanel().setHeaderText(UserGroupEntityPanel.DERAULT_USER_HEADER);
		rootPanel.getUserPanel().display(users);		
		rootPanel.getPropertyPanel().setHeaderText(UserGroupPropertyPanel.DEFAULT_HEADER);
		rootPanel.getPropertyPanel().showStatus("Loading groups and users...", false);	

		// Asynchronously loading the groups
		EucalyptusWebBackend.App.getInstance().getGroups(sessionId, "",
				new AsyncCallback<List<GroupInfoWeb>>() {
					public void onSuccess(List<GroupInfoWeb> result) {
						refillGroups(result);
						rootPanel.getGroupPanel().display(groups);
						if (groups.size() > 0) {
							rootPanel.getGroupPanel().getList().setSelected(0);
							displaySelectedGroups();
						} else {
							rootPanel.getPropertyPanel().showEmptyPrompt();
						}
					}
					public void onFailure(Throwable caught) {
						rootPanel.getPropertyPanel().showStatus(
								"Loading groups failed! " + getFailureMessage(caught), true);
					}
				});
	}
	
	private void getSelectedGroups() {
		selectedGroups.clear();
		List<DataRow> selected = rootPanel.getGroupPanel().getList().getSelected();
		for (DataRow row : selected) {
			GroupRow groupRow = (GroupRow) row;
			selectedGroups.add(groupRow.getGroupInfo());
		}
	}
	
	private void getSelectedUsers() {
		selectedUsers.clear();
		List<DataRow> selected = rootPanel.getUserPanel().getList().getSelected();
		for (DataRow row : selected) {
			UserRow userRow = (UserRow) row;
			selectedUsers.add(userRow.getUserInfo());
		}
	}
	
	private void displayGroupProperty(List<GroupInfoWeb> groupInfos) {
		switch (groupInfos.size()) {
		case 0:
			rootPanel.getPropertyPanel().showEmptyPrompt();
			break;
		case 1:
			rootPanel.getPropertyPanel().showGroup(groupInfos.get(0));
			break;
		default:
			rootPanel.getPropertyPanel().showGroups(groupInfos);
			break;
		}
	}
	
	private void displayUserProperty(List<UserInfoWeb> userInfos) {
		switch (userInfos.size()) {
		case 0:
			break;
		case 1:
			UserInfoWeb ui = userInfos.get(0);
			final Grid dataGrid = rootPanel.getPropertyPanel().showUser(ui);
			EucalyptusWebBackend.App.getInstance().getGroupsByUser(sessionId, ui.getUserName(),
					new AsyncCallback<List<String>>() {
						public void onSuccess(List<String> result) {
							dataGrid.setText(10, 1, UserGroupUtils.getListString(result, 0));
						}
						public void onFailure(Throwable caught) {
							rootPanel.getPropertyPanel().showStatus(
									"Loading user's groups failed! " + getFailureMessage(caught),
									true);
						}
					});
			break;
		default:
			rootPanel.getPropertyPanel().showUsers(userInfos);
			break;
		}
	}
	
	private List<String> getGroupNamesFromInfos(List<GroupInfoWeb> groupInfos) {
		List<String> names = new ArrayList<String>();
		for (GroupInfoWeb gi : groupInfos) {
			names.add(gi.name);
		}
		return names;
	}
	
	private void displayUserPanelHeader(List<GroupInfoWeb> groupInfos) {
		switch (groupInfos.size()) {
		case 0:
			rootPanel.getUserPanel().setHeaderText(UserGroupEntityPanel.DERAULT_USER_HEADER);
			break;
		case 1:
			rootPanel.getUserPanel().setHeaderText("Users of " + groupInfos.get(0).name);
			break;
		default:
			rootPanel.getUserPanel().setHeaderText("Users of " + groupInfos.size() + " groups");
			break;
		}
	}
	
	public static String getFailureMessage(Throwable caught) {
		if (caught != null && caught.getMessage() != null) {
			return caught.getMessage();
		}
		return "";
	}
	
	public void displaySelectedGroups() {
		// Get group data list from the selected row indices
		getSelectedGroups();	
		// Display property of selected groups
		displayGroupProperty(selectedGroups);
		// Display user panel header text of selected groups
		displayUserPanelHeader(selectedGroups);
		// Now clear up the user data and the user panel
		users.clear();
		rootPanel.getUserPanel().display(users);
		// If we selected any number of groups, load the users and display asynchronously
		if (selectedGroups.size() > 0) {
			final List<String> groupNames = getGroupNamesFromInfos(selectedGroups);
			EucalyptusWebBackend.App.getInstance().getUsersByGroups(
					sessionId, groupNames,
					new AsyncCallback<List<UserInfoWeb>>() {
						public void onSuccess(List<UserInfoWeb> result) {
							refillUsers(result);
							rootPanel.getUserPanel().display(users);
							rootPanel.getPropertyPanel().setSubtitle(users.size() + " users");
						}
						public void onFailure(Throwable caught) {
							rootPanel.getPropertyPanel().showStatus(
									"Loading users failed! " + getFailureMessage(caught), true);
						}
					});
		}
	}
	
	public void displaySelectedUsers() {
		// Get user data list from selected row indices
		getSelectedUsers();
		if (selectedUsers.size() > 0) {
			// Display property of selected users
			displayUserProperty(selectedUsers);
		} else {
			// If none of the users is selected, we display selected groups instead
			// Get group data list from the selected row indices
			getSelectedGroups();	
			// Display property of selected groups
			displayGroupProperty(selectedGroups);	
		}
	}
	
	private List<String> getGroupNames() {
		List<String> names = new ArrayList<String>();
		for (DataRow gi : groups) {
			names.add(((GroupRow)gi).getGroupInfo().name);
		}
		Collections.sort(names);
		return names;
	}
	
	private List<String> getSelectedGroupNames() {
		List<String> names = UserGroupUtils.getGroupNamesFromGroups(selectedGroups);
		Collections.sort(names);
		return names;
	}
	
	public void displayAddUserUI() {
		rootPanel.getPropertyPanel().showAddUser(getGroupNames(), getSelectedGroupNames());
	}
	
	public void displayAddGroupUI() {
		rootPanel.getPropertyPanel().showAddGroup();
	}
	
	public void displayEditGroupUI() {
		rootPanel.getPropertyPanel().showEditGroup(selectedGroups.get(0));
	}
	
	public void displayDeleteGroupUI() {
		rootPanel.getPropertyPanel().showDeleteGroup(selectedGroups.get(0));
	}
	
	public void displayDeleteGroupsUI() {
		rootPanel.getPropertyPanel().showDeleteGroups(selectedGroups);
	}
	
	public void displayEditUserUI() {
		final UserInfoWeb user = selectedUsers.get(0);
		EucalyptusWebBackend.App.getInstance().getGroupsByUser(sessionId, user.getUserName(),
				new AsyncCallback<List<String>>() {
					public void onSuccess(List<String> result) {
						rootPanel.getPropertyPanel().showEditUser(user, getGroupNames(), result);
					}
					public void onFailure(Throwable caught) {
						rootPanel.getPropertyPanel().showStatus(
								"Loading user's groups failed! " + getFailureMessage(caught),
								true);
					}
				});
		
	}
	
	public void displayDeleteUserUI() {
		rootPanel.getPropertyPanel().showDeleteUser(selectedUsers.get(0));
	}
	
	public void displayDeleteUsersUI() {
		rootPanel.getPropertyPanel().showDeleteUsers(selectedUsers);
	}
	
	public void displayAddUsersToGroupsUI() {
		rootPanel.getPropertyPanel().showAddUsersToGroups(selectedUsers, getGroupNames());
	}
	
	public void displayRemoveUsersFromGroupsUI() {
		rootPanel.getPropertyPanel().showRemoveUsersFromGroups(selectedUsers, getGroupNames(),
				getSelectedGroupNames());
	}
	
	public void addGroup(GroupInfoWeb group) {
		EucalyptusWebBackend.App.getInstance().addGroup(sessionId, group,
				new AsyncCallback<Void>() {
					public void onSuccess(Void result) {
						display();
					}
					public void onFailure(Throwable caught) {
						displaySelectedUsers();
						rootPanel.getPropertyPanel().showStatus(
								"Adding group failed! " + getFailureMessage(caught), true);
					}
				});
	}
	
	public void updateGroup(GroupInfoWeb group) {
		EucalyptusWebBackend.App.getInstance().updateGroup(sessionId, group,
				new AsyncCallback<Void>() {
					public void onSuccess(Void result) {
						display();
					}
					public void onFailure(Throwable caught) {
						displaySelectedUsers();
						rootPanel.getPropertyPanel().showStatus(
								"Updating group failed! " + getFailureMessage(caught), true);
					}
				});
	}
	
	public void deleteGroups(List<String> groupNames) {
		EucalyptusWebBackend.App.getInstance().deleteGroups(sessionId, groupNames,
				new AsyncCallback<Void>() {
					public void onSuccess(Void result) {
						display();
					}
					public void onFailure(Throwable caught) {
						displaySelectedUsers();
						rootPanel.getPropertyPanel().showStatus(
								"Deleting groups failed! " + getFailureMessage(caught), true);
					}
				});
	}
	
	public void addUser(UserInfoWeb user, List<String> groupNames) {
		EucalyptusWebBackend.App.getInstance().addUser(sessionId, user, groupNames,
				new AsyncCallback<Void>() {
					public void onSuccess(Void result) {
						displaySelectedGroups();
					}
					public void onFailure(Throwable caught) {
						displaySelectedUsers();
						rootPanel.getPropertyPanel().showStatus(
								"Adding user failed! " + getFailureMessage(caught), true);
					}
				});
	}
	
	public void updateUser(UserInfoWeb user, List<String> groupNames) {
		EucalyptusWebBackend.App.getInstance().updateUser(sessionId, user, groupNames,
				new AsyncCallback<Void>() {
					public void onSuccess(Void result) {
						displaySelectedGroups();
					}
					public void onFailure(Throwable caught) {
						displaySelectedUsers();
						rootPanel.getPropertyPanel().showStatus(
								"Editing user failed! " + getFailureMessage(caught), true);
					}
				});
	}
	
	public void deleteUsers(List<String> userNames) {
		EucalyptusWebBackend.App.getInstance().deleteUsers(sessionId, userNames,
				new AsyncCallback<Void>() {
					public void onSuccess(Void result) {
						displaySelectedGroups();
					}
					public void onFailure(Throwable caught) {
						displaySelectedUsers();
						rootPanel.getPropertyPanel().showStatus(
								"Editing user failed! " + getFailureMessage(caught), true);
					}
				});
	}
	
	public void addUsersToGroups(List<String> userNames, List<String> groupNames) {
		EucalyptusWebBackend.App.getInstance().addUsersToGroups(sessionId, userNames, groupNames,
				new AsyncCallback<Void>() {
					public void onSuccess(Void result) {
						displaySelectedGroups();
					}
					public void onFailure(Throwable caught) {
						displaySelectedUsers();
						rootPanel.getPropertyPanel().showStatus(
								"Editing user failed! " + getFailureMessage(caught), true);
					}
				});
	}
	
	public void removeUsersFromGroups(List<String> userNames, List<String> groupNames) {
		EucalyptusWebBackend.App.getInstance().removeUsersFromGroups(sessionId, userNames, groupNames,
				new AsyncCallback<Void>() {
					public void onSuccess(Void result) {
						displaySelectedGroups();
					}
					public void onFailure(Throwable caught) {
						displaySelectedUsers();
						rootPanel.getPropertyPanel().showStatus(
								"Editing user failed! " + getFailureMessage(caught), true);
					}
				});
	}
}