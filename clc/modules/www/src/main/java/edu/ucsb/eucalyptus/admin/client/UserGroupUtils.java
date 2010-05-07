package edu.ucsb.eucalyptus.admin.client;

import java.util.ArrayList;
import java.util.List;

public class UserGroupUtils {
	
	/**
	 * Get the concatenation of a list of string using ", ". If the max is given,
	 * only show max number of strings and append "..." to the end indicating there
	 * are more.
	 * @param list The list of strings to concatenate
	 * @param max The maximal number of strings to concatenate
	 * @return The string concatenation of the list
	 */
	public static String getListString(List<String> list, int max) {
		StringBuffer sb = new StringBuffer();
		if (list != null) {
			int i = 0;
			for (String value : list) {
				if (max > 0 && i >= max) {
					break;
				}
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(value);
				i++;
			}
			if (max > 0 && max < list.size()) {
				sb.append(", ...");
			}
		}
		return sb.toString();
	}
	
	public static List<String> getGroupNamesFromGroups(List<GroupInfoWeb> groups) {
		List<String> names = new ArrayList<String>();
		for (GroupInfoWeb gi : groups) {
			names.add(gi.name);
		}
		return names;
	}
	
	public static List<String> getUserNamesFromUsers(List<UserInfoWeb> users) {
		List<String> names = new ArrayList<String>();
		for (UserInfoWeb ui : users) {
			names.add(ui.getUserName());
		}
		return names;
	}
	
	public static String getBooleanValue(String trueValue, String falseValue, boolean bool) {
		if (bool) {
			return trueValue;
		}
		return falseValue;
	}
	
	public static boolean isSpecialGroup(String name) {
		return (UserGroupControl.GROUP_ALL.equals(name) ||
				UserGroupControl.GROUP_DEFAULT.equals(name));
	}
	
	public static List<String> removeSpecialGroups(List<String> groupNames) {
		List<String> result = new ArrayList<String>();
		for (String groupName : groupNames) {
			if (!isSpecialGroup(groupName)) {
				result.add(groupName);
			}
		}
		return result;
	}
	
	public static String getSpecialGroupHelp(String name) {
		if (UserGroupControl.GROUP_ALL.equals(name)) {
			return ("This is a special group containing all users. " + 
				"It can not be changed or deleted.");
		} else if (UserGroupControl.GROUP_DEFAULT.equals(name)) {
			return ("This is a special group. Any user that is created " +
					"without specifying a group belongs to this group. " +
					"It can not be changed or deleted");
		}
		return "";
	}
}
