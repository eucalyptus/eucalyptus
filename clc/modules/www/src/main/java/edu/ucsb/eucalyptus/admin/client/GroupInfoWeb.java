package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

public class GroupInfoWeb implements IsSerializable {
	public String name;
	public List<String> zones;
}
