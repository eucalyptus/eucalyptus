package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "storage_stats_info" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class StorageStatsInfo {
	@Id
	@GeneratedValue
	@Column( name = "storage_stats_info_id" )
	private Long id = -1l;
	@Column(name = "storage_name")
	private String name;
	@Column( name = "number_volumes" )
	private Integer numberOfVolumes;
	@Column( name = "total_space_used" )
	private Long totalSpaceUsed;

	public StorageStatsInfo() {}
	
	public StorageStatsInfo(final String name) {
		this.name = name;
	}
	
	public StorageStatsInfo(final String name, 
			Integer numberOfVolumes,
			Long totalSpaceUsed) {
		this.name = name;
		this.numberOfVolumes = numberOfVolumes;
		this.totalSpaceUsed = totalSpaceUsed;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getNumberOfVolumes() {
		return numberOfVolumes;
	}

	public void setNumberOfVolumes(Integer numberOfVolumes) {
		this.numberOfVolumes = numberOfVolumes;
	}

	public Long getTotalSpaceUsed() {
		return totalSpaceUsed;
	}

	public void setTotalSpaceUsed(Long totalSpaceUsed) {
		this.totalSpaceUsed = totalSpaceUsed;
	}
}
