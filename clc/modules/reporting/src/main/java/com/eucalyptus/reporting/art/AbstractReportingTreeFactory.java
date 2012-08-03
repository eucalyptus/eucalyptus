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
 *   ObjectTypeName		: STRING ("Report")
 *   BeginMs			: LONG
 *   EndMs				: LONG
 *   +AvailabilityZones	: ARRAY (of AvailabilityZone) (NOTE: ONE of these will be present depending upon the report criterion selected)
 *   +Clusters			: ARRAY (of Cluster)
 *   +Accounts			: ARRAY (of accounts)
 *   +Groups			: ARRAY (of groups)
 *   +Users				: ARRAY (of User)
 *   UsageTotals		: UsageTotals
 *  
 * Availability Zone Object
 *   ObjectTypeName		: STRING ("AvailabilityZone")
 *   Name				: STRING
 *   +Clusters			: ARRAY (of Cluster)
 *   +Accounts			: ARRAY (of accounts)
 *   +Groups			: ARRAY (of groups)
 *   +Users				: ARRAY (of User)
 *   UsageTotals		: UsageTotals
 *   
 * Cluster Object
 *   ObjectTypeName	: STRING ("Cluster")
 *   Name			: STRING
 *   +Accounts		: ARRAY (of accounts)
 *   +Groups		: ARRAY (of groups)
 *   UsageTotals	: UsageTotals
 * 
 * Group Object
 *   ObjectTypeName	: STRING ("Group")
 *   Name			: STRING
 *   Users			: ARRAY (of User)
 *   UsageTotals	: UsageTotals
 * 
 * Account Object
 *   ObjectTypeName	: STRING ("Account")
 *   Name			: STRING
 *   Users			: ARRAY (of User)
 *   UsageTotals	: UsageTotals
 *
 * User Object
 *   ObjectTypeName	: STRING ("User")
 *   Username		: STRING (example: "tom")
 *   FullName		: STRING (example: "Tom Werges")
 *   Instances		: ARRAY (of Instance objects)
 *   Volumes		: ARRAY (of Volume objects)
 *   Snapshots		: ARRAY (of Snapshot objects)
 *   ElasticIps		: ARRAY (of ElasticIp objects)
 *   S3Buckets		: ARRAY (of S3Bucket objects)
 *   S3Objects		: ARRAY (of S3Object objects)
 *   UsageTotals	: UsageTotals
 *   
 * Instance Object
 *   ObjectTypeName	: STRING ("Instance")
 *   InstanceType	: STRING (example: "m1.small")
 *   Id				: STRING (example: "i-23423434")
 *   InstanceUsage	: InstanceUsage
 *   //TODO volumes
 *   //TODO elastic IP attachments
 *   
 * InstanceUsage Object
 *   ObjectTypeName			: STRING ("InstanceUsage")
 *   CpuPercentAvg			: DOUBLE
 *   DiskIoGB				: LONG
 *   NetIoWithinZoneInGB	: LONG
 *   NetIoBetweenZoneInGB	: LONG
 *   NetIoPublicIpInGB		: LONG
 *   NetIoWithinZoneOutGB	: LONG
 *   NetIoBetweenZoneOutGB	: LONG
 *   NetIoPublicIpOutGB		: LONG
 *   
 * InstanceTotals Object
 *   ObjectTypeName			: STRING ("InstanceTotals")
 *   M1SmallUsage			: InstanceUsage
 *   C1MediumInstanceUsage	: InstanceUsage
 *   M1LargeInstanceUsage	: InstanceUsage
 *   M1XLargeInstanceUsage	: InstanceUsage
 *   
 * S3Bucket Object
 *    ObjectTypeName		: STRING ("S3Bucket")
 *    Name					: STRING
 * 
 * S3BucketTotals Object
 *    ObjectTypeName			: STRING ("S3BucketTotals")
 *    SizeGB					: LONG
 *    NumGetRequests			: LONG
 *    NumPutRequests			: LONG
 * 
 * S3Object Object
 *    ObjectTypeName			: STRING ("S3Object")
 *    Name						: STRING
 *    SizeGB					: LONG
 *    NumGetRequests			: LONG
 *    NumPutRequests			: LONG
 *    //TODO: getPutRequests? Doesn't this overwrite the object and change its size?
 *    //TODO: Can a user delete an S3 object and create another one of the same name?
 *
 * S3ObjectTotals Object
 *    ObjectTypeName			: STRING ("S3ObjectTotals")
 *    SizeGB					: LONG
 *    NumGetRequests			: LONG
 *    NumPutRequests			: LONG
 *
 * ElasticIp Object
 *    ObjectTypeName			: STRING ("ElasticIp")
 *    IpAddress					: STRING
 *
 * ElasticIpTotals Object
 *    ObjectTypeName			: STRING ("ElasticIpTotals")
 *    InstanceTotals			: ARRAY (of ElasticIpInstanceTotals)
 *    
 * ElasticIpInstanceTotals
 *    ObjectTypeName			: STRING ("ElasticIpInstanceTotals")
 *
 * Volume Object
 *   ObjectTypeName				: STRING ("Volume")
 *   Id   						: STRING (example: "vol-45344545")
 *   SizeGB						: LONG
 *   ReadGB						: LONG
 *   WrittenGB					: LONG
 *
 * VolumeTotals Object
 *    ObjectTypeName				: STRING ("VolumeTotals")
 *    ReadGB						: LONG
 *    WrittenGB						: LONG    
 * 
 * VolumeSnapshot Object
 *    ObjectTypeName				: STRING ("VolumeSnapshot")
 * 	  Id							: STRING
 *    VolumeId						: STRING
 *    NumReads						: LONG
 *    SizeGB						: LONG
 *    
 * VolumeSnapshotTotals Object
 *    ObjectTypeName				: STRING ("VolumeSnapshotTotals")
 *    NumReads						: LONG
 *    SizeGB						: LONG
 *
 * UsageTotals Object
 *   ObjectTypeName		: STRING ("UsageTotals")
 *   +InstanceTotals	: InstanceTotals
 *   +VolumeTotals		: VolumeTotals
 *   +SnapshotTotals	: VolumeSnapshotTotals
 *   +ElasticIpTotals	: ElasticIpTotals
 *   +S3BucketTotals	: S3BucketTotals
 *   +S3ObjectTotals 	: S3ObjectTotals
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
	
	public ArtObject getArt(Period period, ReportingCriterion groupBy,
			ReportingCriterion criterion) 
	{
		//TODO: Replace with actual report generation when available
		return getFalseTree();
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
