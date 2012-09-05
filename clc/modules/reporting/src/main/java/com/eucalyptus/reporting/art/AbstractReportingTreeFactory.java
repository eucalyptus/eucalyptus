/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.reporting.art;

import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.ReportingCriterion;

/**
 * <p>AbstractReportingTree is an abstract representation of a report, in a tree structure.
 * It includes resource usage for some given time period, broken down by availability
 * zone, cluster, account, user, etc. It can be rendered into HTML, CSV, or other formats,
 * by a Renderer.
 * 
 * <p>The tree is similar to a JSON ("JavaScript Object Notation") structure, with objects
 * (similar to HashMaps), Arrays (Lists), and values (Strings, Longs, and Doubles). Objects
 * and Arrays can include other objects and arrays, leading to a tree-like, recursive structure.
 * 
 * <p>Calling the getArt method causes the database to be scanned, and a tree to be constructed
 * of all resource usage, broken down by whatever criteria you wish. Computations are made upon
 * the data from the database, interpolations are performed, averages are calculated, and nodes
 * are added to the tree of aggregate usage.
 * 
 * <p>Any ART object can display a pretty-printed recursive text version by calling toString().
 * However, this mechanism is not intended as a way to serialize or deserialize this tree. It's
 * expected that a separate mechanism for converting an ART tree to a text format and back will
 * be constructed for the Data Warehouse. 
 * 
 * <p>The objects in the ART will have the following attributes (+ means optional):
 * 
 * Report Object (This is the ROOT object returned)
 *   objectTypeName	: STRING ("Report")
 *   beginMs		: LONG
 *   endMs			: LONG
 *   +zones			: ARRAY (of AvailabilityZone) (NOTE: ONE of these will be present depending upon the report criterion selected)
 *   +clusters		: ARRAY (of Cluster)
 *   +accounts		: ARRAY (of accounts)
 *   +groups		: ARRAY (of groups)
 *   +users			: ARRAY (of User)
 *   usageTotals	: UsageTotals
 *  
 * Availability Zone Object
 *   objectTypeName	: STRING ("AvailabilityZone")
 *   name			: STRING
 *   clusters		: ARRAY (of Cluster)
 *   usageTotals	: UsageTotals
 *   
 * Cluster Object
 *   objectTypeName	: STRING ("Cluster")
 *   name			: STRING
 *   +accounts		: ARRAY (of accounts)
 *   +groups		: ARRAY (of groups)
 *   usageTotals	: UsageTotals
 * 
 * Group Object
 *   objectTypeName	: STRING ("Group")
 *   name			: STRING
 *   users			: ARRAY (of User)
 *   usageTotals	: UsageTotals
 * 
 * Account Object
 *   objectTypeName	: STRING ("Account")
 *   name			: STRING
 *   users			: ARRAY (of User)
 *   usageTotals	: UsageTotals
 *
 * User Object
 *   objectTypeName	: STRING ("User")
 *   username		: STRING (example: "tom")
 *   fullName		: STRING (example: "Tom Werges")
 *   instances		: ARRAY (of Instance objects)
 *   volumes		: ARRAY (of Volume objects)
 *   snapshots		: ARRAY (of Snapshot objects)
 *   usageTotals	: UsageTotals
 *   
 * Instance Object
 *   objectTypeName	: STRING ("Instance")
 *   instanceType	: STRING (example: "m1.small")
 *   id				: STRING (example: "i-23423434")
 *   instanceUsage	: InstanceUsage
 *   
 * InstanceUsage Object
 *   objectTypeName		: STRING ("InstanceUsage")
 *   cpuPercentAvg		: DOUBLE
 *   diskIo				: LONG
 *   netIoWithinZoneIn	: LONG
 *   netIoBetweenZoneIn	: LONG
 *   netIoPublicIpIn	: LONG
 *   netIoWithinZoneOut	: LONG
 *   netIoBetweenZoneOut: LONG
 *   netIoPublicIpOut	: LONG
 *   
 * InstanceTotals Object
 *   objectTypeName			: STRING ("InstanceTotals")
 *   m1SmallUsage			: InstanceUsage
 *   c1MediumInstanceUsage	: InstanceUsage
 *   m1LargeInstanceUsage	: InstanceUsage
 *   m1XLargeInstanceUsage	: InstanceUsage
 *   
 * S3Bucket Object
 *    objectTypeName		: STRING ("S3Bucket")
 *    sizeGB				: LONG
 *    numGetRequests		: LONG
 *    numPutRequests		: LONG
 * 
 * S3BucketTotals Object
 *    objectTypeName			: STRING ("S3BucketTotals")
 *    sizeGB					: LONG
 *    numGetRequests			: LONG
 *    numPutRequests			: LONG
 * 
 * S3Object Object
 *    objectTypeName			: STRING ("S3Object")
 *    sizeGB					: LONG
 *    numGetRequests			: LONG
 *    numPutRequests			: LONG
 *
 * S3ObjectTotals Object
 *    objectTypeName			: STRING ("S3ObjectTotals")
 *    sizeGB					: LONG
 *    numGetRequests			: LONG
 *    numPutRequests			: LONG
 *
 * ElasticIp Object
 *    objectTypeName			: STRING ("ElasticIp")
 *    ipAddress					: STRING
 *
 * ElasticIpTotals Object
 *    objectTypeName			: STRING ("ElasticIpTotals")
 *    instanceTotals			: ARRAY (of ElasticIpInstanceTotals)
 *    
 * ElasticIpInstanceTotals
 *    objectTypeName			: STRING ("ElasticIpInstanceTotals")
 *
 * Volume Object
 *   objectTypeName				: STRING ("Volume")
 *   id   						: STRING (example: "vol-45344545")
 *   sizeGB						: LONG
 *   readGB						: LONG
 *   writtenGB					: LONG
 *
 * VolumeTotals Object
 *    objectTypeName				: STRING ("VolumeTotals")
 *    readGB						: LONG
 *    writtenGB					: LONG    
 * 
 * VolumeSnapshot Object
 *    objectTypeName				: STRING ("VolumeSnapshot")
 * 	  id							: STRING
 *    volumeId						: STRING
 *    numReads						: LONG
 *    sizeGB						: LONG
 *    
 * VolumeSnapshotTotals Object
 *    objectTypeName				: STRING ("VolumeSnapshotTotals")
 *    numReads						: LONG
 *    sizeGB						: LONG
 *
 * UsageTotals Object
 *   objectTypeName		: STRING ("UsageTotals")
 *   +instanceTotals	: InstanceTotals
 *   +volumeTotals		: VolumeTotals
 *   +snapshotTotals	: VolumeSnapshotTotals
 *   +elasticIpTotals	: ElasticIpTotals
 *   +s3BucketTotals	: S3BucketTotals
 *   +s3ObjectTotals 	: S3ObjectTotals
 *   
 */
public class AbstractReportingTreeFactory
{

	private static AbstractReportingTreeFactory instance;
	
	private AbstractReportingTreeFactory()
	{
		
	}
	
	public static AbstractReportingTreeFactory getInstance()
	{
		if (instance == null) {
			instance = new AbstractReportingTreeFactory();
		}
		return instance;
	}
	
	/**
	 * GroupBy? Criterion? Perhaps we should just have Period? Or a collection of GroupBy
	 * and Criterion objects?
	 */
	public ArtObject getArt(Period period, ReportingCriterion groupBy,
			ReportingCriterion criterion) 
	{
		return null;
		/* Generate a report
		 */
		
		/* Scan thru attributes and usage events separately.
		 * This should return an iterator with an (attribute,usageEvent) tuple for each row.
		 * Keep instance totals
		 * for each instance. For edge cases, perform interpolations and calculations. 
		 * Afterwards, render this into a tree by creating parent nodes for
		 * each total.
		 * Then perform totals for the parent nodes.
		 * 
		 * What about attach, detach?
		 * What about zero usage but running throughout?
		 *   Only if we find an event afterwards for instance.
		 * If an edge case, perform interpolations.
		 * Add usage to node object
		 * Add all usages for totals up the tree.
		 */
		
	}
	
	
	private static final int NUM_ZONES                 = 2;
	private static final int NUM_ACCOUNTS_PER_ZONE     = 5;
	private static final int NUM_USERS_PER_ACCOUNT     = 20;
	private static final int NUM_INSTANCES_PER_USER    = 10;
	private static final int NUM_VOLUMES_PER_USER      = 5;
	private static final int NUM_SNAPSHOTS_PER_VOLUME  = 2;
	private static final int NUM_ELASTIC_IPS_PER_USER  = 5;
	private static final int NUM_S3_BUCKETS_PER_USER   = 5;
	private static final int NUM_S3_OBJECTS_PER_BUCKET = 5;

	
    /**
     * @return An ART with false reporting values. Includes zones, clusters, accounts, users,
     * instances, volumes, elastic ips, snapshots, S3 Buckets, S3Objects, usage of all those
     * entities, usage totals, volumes per instance, and elastic ips per instance.
     */
    private ArtObject getFalseTree()
    {
    	//TODO: Add usage total objects
    	ArtObject reportObject = new ArtObject();
    	reportObject.putAttribute("ObjectTypeName", "Report");
    	reportObject.putAttribute("BeginMs", 1104566400000l); //Jan 1, 2005 12:00AM
    	reportObject.putAttribute("EndMs", 1104566400000l+(86400000l*5l)); //5 days later
    	ArtArray zonesArray = new ArtArray();
    	reportObject.putAttribute("AvailabilityZones", zonesArray);
    	reportObject.putAttribute("UsageTotals", "Report");

    	/* Create Availability Zones */
    	for (int i=0; i<NUM_ZONES; i++) {
    		ArtObject zoneObject = new ArtObject();
    		zonesArray.add(zoneObject);
    		zoneObject.putAttribute("ObjectTypeName", "AvailabilityZone");
    		zoneObject.putAttribute("Name", "Zone-" + i);
    		ArtArray accountsArray = new ArtArray();
    		zoneObject.putAttribute("Accounts", accountsArray);

    		/* Create Accounts */
    		for (int j=0; j<NUM_ACCOUNTS_PER_ZONE; j++) {
    			ArtObject accountObject = new ArtObject();
    			accountsArray.add(accountObject);
    			accountObject.putAttribute("ObjectTypeName", "Account");
    			accountObject.putAttribute("Name", "Account-" + i);
    			ArtArray usersArray = new ArtArray();
    			accountObject.putAttribute("Users", usersArray);

    			/* Create Users */
    			for (int k=0; k<NUM_USERS_PER_ACCOUNT; k++) {
    				ArtObject userObject = new ArtObject();
    				usersArray.add(userObject);
    				userObject.putAttribute("ObjectTypeName", "User");
    				userObject.putAttribute("Name", "User-" + i);
    				userObject.putAttribute("FullName", "Frank P User #" + i);
    				ArtArray instancesArray = new ArtArray();
    				ArtArray volumesArray = new ArtArray();
    				ArtArray snapshotsArray = new ArtArray();
    				ArtArray elasticIpsArray = new ArtArray();
    				ArtArray s3BucketsArray = new ArtArray();
    				ArtArray s3ObjectsArray = new ArtArray();
    				userObject.putAttribute("Instances", instancesArray);
    				userObject.putAttribute("Volumes", volumesArray);
    				userObject.putAttribute("Snapshots", snapshotsArray);
    				userObject.putAttribute("elasticIpsArray", elasticIpsArray);
    				userObject.putAttribute("s3BucketsArray", s3BucketsArray);
    				userObject.putAttribute("s3ObjectsArray", s3ObjectsArray);
    				String[] instanceTypes = {"m1Small", "c1Medium", "m1Large", "m1XLarge"};

    				/* Create Instances and InstanceUsage */
    				for (int instancesNum=0; instancesNum<NUM_INSTANCES_PER_USER; instancesNum++) {
    					ArtObject instanceObject = new ArtObject();
    					instancesArray.add(instanceObject);
    					instanceObject.putAttribute("ObjectTypeName", "Instance");
    					instanceObject.putAttribute("InstanceType", instanceTypes[instancesNum % instanceTypes.length]);
    					instanceObject.putAttribute("Id", "i-a32" + instancesNum);
    					ArtObject instanceUsageObject = new ArtObject();
    					instanceObject.putAttribute("InstanceUsage", instanceUsageObject);
    					instanceUsageObject.putAttribute("CpuPercentAverage", (double)(instancesNum*5));
    					instanceUsageObject.putAttribute("DiskIoGB", (long)(instancesNum*2));
    					instanceUsageObject.putAttribute("NetIoWithinZoneInGB", (long)(instancesNum*3));
    					instanceUsageObject.putAttribute("NetIoBetweenZonesInGB", (long)(instancesNum*4));
    							instanceUsageObject.putAttribute("NetIoPublicIpInGB", (long)(instancesNum*5));
    					instanceUsageObject.putAttribute("NetIoWithinZoneOutGB", (long)(instancesNum*6));
    					instanceUsageObject.putAttribute("NetIoBetweenZonesOutGB", (long)(instancesNum*7));
    							instanceUsageObject.putAttribute("NetIoPublicIpOutGB", (long)(instancesNum*8));
    				}

    				/* Create Volumes and Snapshots */
    				for (int volumesNum=0; volumesNum<NUM_VOLUMES_PER_USER; volumesNum++) {
    					ArtObject volumeObject = new ArtObject();
    					volumesArray.add(volumeObject);
    					volumeObject.putAttribute("ObjectTypeName", "Volume");
    					String volumeId = "vol-a32" + volumesNum;
    					volumeObject.putAttribute("Id", volumeId);
    					long sizeGB = (long)(volumesNum*10);
    					volumeObject.putAttribute("SizeGB", sizeGB);
    					volumeObject.putAttribute("ReadGB", (long)(volumesNum*50));
    					volumeObject.putAttribute("WrittenGB", (long)(volumesNum*50));
    					for (int snapshotsNum=0; snapshotsNum<NUM_SNAPSHOTS_PER_VOLUME; snapshotsNum++) {
    						ArtObject snapshotObject = new ArtObject();
    						snapshotsArray.add(snapshotObject);
    						snapshotObject.putAttribute("ObjectTypeName", "Snapshot");
    						snapshotObject.putAttribute("Id", "snap-" + snapshotsNum);
    						snapshotObject.putAttribute("VolumeId", volumeId);
    						snapshotObject.putAttribute("NumReads", (long)(snapshotsNum*3));
    						snapshotObject.putAttribute("SizeGB", sizeGB);
    					}
    				}
        			/* Create Elastic IPs */
        			for (int elasticIpsNum=0; elasticIpsNum<NUM_ELASTIC_IPS_PER_USER; elasticIpsNum++) {
        				ArtObject elasticIpObject = new ArtObject();
        				elasticIpsArray.add(elasticIpObject);
        				elasticIpObject.putAttribute("ObjectTypeName", "ElasticIp");

        			}

        			/* Create S3 Buckets and Objects */
        			for (int s3BucketsNum=0; s3BucketsNum<NUM_S3_BUCKETS_PER_USER; s3BucketsNum++) {
        				ArtObject s3BucketObject = new ArtObject();
        				s3BucketsArray.add(s3BucketObject);
        				s3BucketObject.putAttribute("ObjectTypeName", "S3Bucket");
        				s3BucketObject.putAttribute("Name", "bucket-" + s3BucketsNum);
        				for (int s3ObjectsNum=0; s3ObjectsNum<NUM_S3_OBJECTS_PER_BUCKET; s3ObjectsNum++) {
        					ArtObject s3ObjectObject = new ArtObject();
        					s3ObjectsArray.add(s3ObjectObject);
        					s3ObjectObject.putAttribute("ObjectTypeName", "S3Object");
        					s3ObjectObject.putAttribute("Name", "object-" + s3ObjectsNum);
        					s3ObjectObject.putAttribute("SizeGB", (long)(s3ObjectsNum*10));
        					s3ObjectObject.putAttribute("ObjectTypeName", (long)(s3ObjectsNum*2));
        					s3ObjectObject.putAttribute("ObjectTypeName", (long)(s3ObjectsNum*3));
        				}
        			}
    			}
    		}
    	}
    	return reportObject;
    }


	
}
