package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "walrus_stats_info" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class WalrusStatsInfo {
	@Id
	@GeneratedValue
	@Column( name = "walrus_stats_info_id" )
	private Long id = -1l;
	@Column(name = "walrus_name")
	private String name;
	@Column( name = "number_buckets" )
	private Integer numberOfBuckets;
	@Column( name = "total_space_used" )
	private Long totalSpaceUsed;

	public WalrusStatsInfo() {}
	
	public WalrusStatsInfo(final String name) {
		this.name = name;
	}
	
	public WalrusStatsInfo(final String name, 
			Integer numberOfBuckets,
			Long totalSpaceUsed) {
		this.name = name;
		this.numberOfBuckets = numberOfBuckets;
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

	public Integer getNumberOfBuckets() {
		return numberOfBuckets;
	}

	public void setNumberOfBuckets(Integer numberOfBuckets) {
		this.numberOfBuckets = numberOfBuckets;
	}

	public Long getTotalSpaceUsed() {
		return totalSpaceUsed;
	}

	public void setTotalSpaceUsed(Long totalSpaceUsed) {
		this.totalSpaceUsed = totalSpaceUsed;
	}
}
