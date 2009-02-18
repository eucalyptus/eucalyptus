package edu.ucsb.eucalyptus.admin.client;
import com.google.gwt.user.client.rpc.IsSerializable;

public class CloudInfoWeb implements IsSerializable {
	private String internalHostPort;
	private String externalHostPort;
	private String servicePath;
	private String cloudId;
	
	public CloudInfoWeb ()
	{
		internalHostPort = null;
		externalHostPort = null;
		servicePath = null;
		cloudId = null;
	}

	public String getInternalHostPort()
	{
		return internalHostPort;
	}

	public void setInternalHostPort( final String hostPort )
	{
		this.internalHostPort = hostPort;
	}

	public String getExternalHostPort()
	{
		return externalHostPort;
	}

	public void setExternalHostPort( final String hostPort )
	{
		this.externalHostPort = hostPort;
	}

	public String getServicePath()
	{
		return servicePath;
	}

	public void setServicePath( final String servicePath )
	{
		this.servicePath = servicePath;
	}

	public String getCloudId()
	{
		return cloudId;
	}

	public void setCloudId( final String cloudId )
	{
		this.cloudId = cloudId;
	}
}
