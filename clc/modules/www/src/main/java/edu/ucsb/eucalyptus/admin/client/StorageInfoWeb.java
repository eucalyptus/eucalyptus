package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class StorageInfoWeb implements IsSerializable {
	private String name;
	private String volumesPath;
	private Integer maxVolumeSizeInGB;
	private Integer totalVolumesSizeInGB;
	private String storageInterface;
	private Boolean zeroFillVolumes;
	private Boolean committed;

	public StorageInfoWeb() {}

	public StorageInfoWeb( final String name, 
			String volumesPath,
			Integer maxVolumeSizeInGB,
			Integer totalVolumesSizeInGB,
			String storageInterface,
			Boolean zeroFillVolumes) {
		this.name = name;
		this.volumesPath = volumesPath;
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
		this.totalVolumesSizeInGB = totalVolumesSizeInGB;
		this.storageInterface = storageInterface;
		this.zeroFillVolumes = zeroFillVolumes;
		this.committed = false;
	}


	public void setCommitted ()
	{
		this.committed = true;
	}

	public Boolean isCommitted ()
	{
		return this.committed;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVolumesPath() {
		return volumesPath;
	}

	public void setVolumesPath(String volumesPath) {
		this.volumesPath = volumesPath;
	}

	public Integer getMaxVolumeSizeInGB() {
		return maxVolumeSizeInGB;
	}

	public void setMaxVolumeSizeInGB(Integer maxVolumeSizeInGB) {
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
	}

	public Integer getTotalVolumesSizeInGB() {
		return totalVolumesSizeInGB;
	}

	public void setTotalVolumesSizeInGB(Integer totalVolumesSizeInGB) {
		this.totalVolumesSizeInGB = totalVolumesSizeInGB;
	}

	public String getStorageInterface() {
		return storageInterface;
	}

	public void setStorageInterface(String storageInterface) {
		this.storageInterface = storageInterface;
	}

	public Boolean getZeroFillVolumes() {
		return zeroFillVolumes;
	}

	public void setZeroFillVolumes(Boolean zeroFillVolumes) {
		this.zeroFillVolumes = zeroFillVolumes;
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		StorageInfoWeb that = ( StorageInfoWeb ) o;

		if ( !name.equals( that.name ) ) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
}
