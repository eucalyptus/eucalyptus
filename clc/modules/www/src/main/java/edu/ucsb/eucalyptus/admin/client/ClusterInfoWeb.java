package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ClusterInfoWeb implements IsSerializable {
	private String name;
	private String host;
	private Integer port;
	private Boolean committed;
	private String storageVolumesPath;
	private Integer storageMaxVolumeSizeInGB;
	private Integer storageVolumesStorageInGB;

	public ClusterInfoWeb()
	{
	}

	public ClusterInfoWeb( final String name, 
		final String host, 
		final Integer port,
		final String storageVolumesPath,
		final Integer storageMaxVolumeSizeInGB,
		final Integer storageVolumesStorageInGB )
	{
		this.name = name;
		this.host = host;
		this.port = port;
		this.storageVolumesPath = storageVolumesPath;
		this.storageMaxVolumeSizeInGB = storageMaxVolumeSizeInGB;
		this.storageVolumesStorageInGB = storageVolumesStorageInGB;
		this.committed = false;
	}

	public String getHost()
	{
		return host;
	}

	public void setHost( final String host )
	{
		this.host = host;
	}

	public Integer getPort()
	{
		return port;
	}

	public void setPort( final Integer port )
	{
		this.port = port;
	}

	public String getName()
	{
		return name;
	}

	public void setName( final String name )
	{
		this.name = name;
	}

	public void setCommitted ()
	{
		this.committed = true;
	}

	public Boolean isCommitted ()
	{
		return this.committed;
	}

	public Integer getStorageMaxVolumeSizeInGB()
	{
		return storageMaxVolumeSizeInGB;
	}

	public void setStorageMaxVolumeSizeInGB( final Integer storageMaxVolumeSizeInGB )
	{
		this.storageMaxVolumeSizeInGB = storageMaxVolumeSizeInGB;
	}

	public Integer getStorageVolumesStorageInGB()
	{
		return storageVolumesStorageInGB;
	}

	public void setStorageVolumesStorageInGB( final Integer storageVolumesStorageInGB )
	{
		this.storageVolumesStorageInGB = storageVolumesStorageInGB;
	}

	public String getStorageVolumesPath()
	{
		return storageVolumesPath;
	}

	public void setStorageVolumesPath( final String storageVolumesPath )
	{
		this.storageVolumesPath = storageVolumesPath;
	}

	@Override
		public boolean equals( final Object o )
	{
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		ClusterInfoWeb that = ( ClusterInfoWeb ) o;

		if ( !name.equals( that.name ) ) return false;

		return true;
	}

	@Override
		public int hashCode()
	{
		return name.hashCode();
	}
}
