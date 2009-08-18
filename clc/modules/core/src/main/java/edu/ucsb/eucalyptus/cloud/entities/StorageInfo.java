package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.util.BindingUtil;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "storage_info" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class
StorageInfo {

	@Id
	@GeneratedValue
	@Column( name = "storage_id" )
	private Long id = -1l;
	@Column( name = "storage_name" )
	private String name;
	@Column( name = "system_storage_volume_size_gb" )
	private Integer maxTotalVolumeSizeInGb;
	@Column( name = "storage_interface" )
	private String storageInterface;
	@Column( name = "system_storage_max_volume_size_gb")
	private Integer maxVolumeSizeInGB;
	@Column( name = "system_storage_volumes_dir" )
	private String volumesDir;

	public StorageInfo(){}

	public StorageInfo( final String name )
	{
		this.name = name;
	}

	public StorageInfo(final String name, 
			final Integer maxTotalVolumeSizeInGb,
			final String storageInterface, 
			final Integer maxVolumeSizeInGB,
			final String volumesDir) {
		this.name = name;
		this.maxTotalVolumeSizeInGb = maxTotalVolumeSizeInGb;
		this.storageInterface = storageInterface;
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
		this.volumesDir = volumesDir;
	}

	public Long getId()
	{
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getMaxTotalVolumeSizeInGb() {
		return maxTotalVolumeSizeInGb;
	}

	public void setMaxTotalVolumeSizeInGb(Integer maxTotalVolumeSizeInGb) {
		this.maxTotalVolumeSizeInGb = maxTotalVolumeSizeInGb;
	}

	public String getStorageInterface() {
		return storageInterface;
	}

	public void setStorageInterface(String storageInterface) {
		this.storageInterface = storageInterface;
	}

	public Integer getMaxVolumeSizeInGB() {
		return maxVolumeSizeInGB;
	}

	public void setMaxVolumeSizeInGB(Integer maxVolumeSizeInGB) {
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
	}

	public String getVolumesDir() {
		return volumesDir;
	}

	public void setVolumesDir(String volumesDir) {
		this.volumesDir = volumesDir;
	}

	@Override
	public boolean equals( Object o )
	{
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		StorageInfo that = ( StorageInfo ) o;

		if ( !name.equals( that.name ) ) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public String toString()
	{
		return this.name;
	}

	public static StorageInfo byName( String name )
	{
		return new StorageInfo(name);
	}

}
