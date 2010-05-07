package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

public class GroupInfoWeb implements IsSerializable {
	public String getName( ) {
    return this.name;
  }
  public void setName( String name ) {
    this.name = name;
  }
  public List<String> getZones( ) {
    return this.zones;
  }
  public void setZones( List<String> zones ) {
    this.zones = zones;
  }
  public String name;
	public List<String> zones;
}
