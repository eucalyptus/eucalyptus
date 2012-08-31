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
	
	public ArtObject getArt(Period period, ReportingCriterion groupBy,
			ReportingCriterion criterion) 
	{
		return null;
	}
	
}
