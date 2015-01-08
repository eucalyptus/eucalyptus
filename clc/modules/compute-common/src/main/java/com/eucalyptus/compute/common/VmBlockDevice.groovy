/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpEmbedded
import com.google.common.collect.Lists
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

public class BlockVolumeMessage extends ComputeMessage {
  
  public BlockVolumeMessage( ) {
    super( );
  }
  
  public BlockVolumeMessage( ComputeMessage msg ) {
    super( msg );
  }
  
  public BlockVolumeMessage( String userId ) {
    super( userId );
  }
}
public class BlockSnapshotMessage extends ComputeMessage {
  
  public BlockSnapshotMessage( ) {
    super( );
  }
  
  public BlockSnapshotMessage( ComputeMessage msg ) {
    super( msg );
  }
  
  public BlockSnapshotMessage( String userId ) {
    super( userId );
  }
}


public class CreateVolumeType extends BlockVolumeMessage {
  String size;
  String snapshotId;
  String availabilityZone;
  String volumeType = "standard"
  Integer iops
  Boolean encrypted
}
public class CreateVolumeResponseType extends BlockVolumeMessage {
  
  Volume volume = new Volume();
}

public class DeleteVolumeType extends BlockVolumeMessage {
  
  String volumeId;
}
public class DeleteVolumeResponseType extends BlockVolumeMessage {
}

public class DescribeVolumesType extends BlockVolumeMessage {
  
  @HttpParameterMapping (parameter = "VolumeId")
  ArrayList<String> volumeSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeVolumesResponseType extends BlockVolumeMessage {
  
  ArrayList<Volume> volumeSet = new ArrayList<Volume>();
}

public class AttachVolumeType extends BlockVolumeMessage {

  String volumeId;
  String instanceId;
  String device;
  public AttachVolumeType( ) {
  }
  public AttachVolumeType( String volumeId, String instanceId, String device ) {
    this.volumeId = volumeId;
    this.instanceId = instanceId;
    this.device = device;
  }

}
public class AttachVolumeResponseType extends BlockVolumeMessage {

  AttachedVolume attachedVolume = new AttachedVolume();
}

public class DetachVolumeType extends BlockVolumeMessage {

  String volumeId;
  String instanceId;
  String device;
  Boolean force = false;
}
public class DetachVolumeResponseType extends BlockVolumeMessage {

  AttachedVolume detachedVolume = new AttachedVolume();
}

public class CreateSnapshotType extends BlockSnapshotMessage {
  
  String volumeId;
  String description;
}
public class CreateSnapshotResponseType extends BlockSnapshotMessage {
  
  Snapshot snapshot = new Snapshot();
}

public class DeleteSnapshotType extends BlockSnapshotMessage {
  
  String snapshotId;
}
public class DeleteSnapshotResponseType extends BlockSnapshotMessage {
}

public class DescribeSnapshotsType extends BlockSnapshotMessage {
  
  @HttpParameterMapping (parameter = "SnapshotId")
  ArrayList<String> snapshotSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Owner")
  ArrayList<String> ownersSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "RestorableBy")
  ArrayList<String> restorableBySet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeSnapshotsResponseType extends BlockSnapshotMessage {
  
  ArrayList<Snapshot> snapshotSet = new ArrayList<Snapshot>();
}

public class AttachedVolume extends EucalyptusData implements Comparable<AttachedVolume> {
  String volumeId;
  String instanceId;
  String device;
  String status;
  Date attachTime = new Date();

  def AttachedVolume(final String volumeId, final String instanceId, final String device) {
    this.volumeId = volumeId;
    this.instanceId = instanceId;
    this.device = device;
    this.status = "attaching";
  }

  public AttachedVolume( String volumeId ) {
    this.volumeId = volumeId;
  }

  public AttachedVolume() {
  }

  public boolean equals(final Object o) {
    if ( this.is(o) ) return true;
    if ( o == null || !getClass().equals( o.class ) ) return false;
    AttachedVolume that = (AttachedVolume) o;
    if ( volumeId ? !volumeId.equals(that.volumeId) : that.volumeId != null ) return false;
    return true;
  }

  public int hashCode() {
    return (volumeId ? volumeId.hashCode() : 0);
  }

  public int compareTo( AttachedVolume that ) {
    return this.volumeId.compareTo( that.volumeId );
  }

  public String toString() {
    return "AttachedVolume ${volumeId} ${instanceId} ${status} ${device} ${attachTime}"
  }
}

public class Volume extends EucalyptusData {
  
  String volumeId;
  String size;
  String volumeType = 'standard'
  Integer iops
  Boolean encrypted
  String snapshotId;
  String availabilityZone;
  String status;
  Date createTime = new Date();
  ArrayList<AttachedVolume> attachmentSet = new ArrayList<AttachedVolume>();
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
  
  public Volume() {
  }
  public Volume(String volumeId) {
    this.volumeId = volumeId;
  }
}

public class Snapshot extends EucalyptusData {
  
  String snapshotId;
  String volumeId;
  String status;
  Date startTime = new Date();
  String progress;
  String ownerId;
  String ownerAlias;
  String volumeSize = "n/a";
  Boolean encrypted
  String description;
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
}

//TODO:ADDED
public class CreateVolumePermissionOperationType extends EucalyptusData {
    @HttpEmbedded( multiple = true )
    ArrayList<CreateVolumePermissionItemType> add = Lists.newArrayList()
    @HttpEmbedded( multiple = true )
    ArrayList<CreateVolumePermissionItemType> remove = Lists.newArrayList()

    def CreateVolumePermissionOperationType() {
    }
}

public class CreateVolumePermissionItemType extends EucalyptusData {

    String userId;
    String group;

    def CreateVolumePermissionItemType() {
    }

    def CreateVolumePermissionItemType(final String userId, final String group) {
        this.userId = userId;
        this.group = group;
    }

    public static CreateVolumePermissionItemType newUserCreateVolumePermission(String userId) {
        return new CreateVolumePermissionItemType(userId, null);
    }

    public static CreateVolumePermissionItemType newGroupCreateVolumePermission() {
        return new CreateVolumePermissionItemType(null, "all" );
    }

    public boolean user() {
        return this.userId != null
    }

    public boolean group() {
        return this.group != null
    }

    boolean equals(final o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        final CreateVolumePermissionItemType that = (CreateVolumePermissionItemType) o

        if (getGroup() != that.getGroup()) return false
        if (userId != that.userId) return false

        return true
    }

    int hashCode() {
        int result
        result = (userId != null ? userId.hashCode() : 0)
        result = 31 * result + (getGroup() != null ? getGroup().hashCode() : 0)
        return result
    }
}

public class ModifySnapshotAttributeResponseType extends BlockSnapshotMessage {
}

public class ModifySnapshotAttributeType extends BlockSnapshotMessage {
    enum SnapshotAttribute { CreateVolumePermission, ProductCode }

    String snapshotId;

    @HttpParameterMapping (parameter = "ProductCode")
    ArrayList<String> productCodes = Lists.newArrayList()

    CreateVolumePermissionOperationType createVolumePermission

    @HttpParameterMapping( parameter = "UserId")
    ArrayList<String> queryUserId = Lists.newArrayList()
    @HttpParameterMapping (parameter = ["Group"])
    ArrayList<String> queryUserGroup = Lists.newArrayList()
    String attribute;
    String operationType;

    SnapshotAttribute snapshotAttribute( ) {
        if ( attribute ) {
            'createVolumePermission'.equals( attribute ) ?
                    SnapshotAttribute.CreateVolumePermission :
                    SnapshotAttribute.ProductCode
        } else {
            createVolumePermission && (!createVolumePermission.getAdd().isEmpty() || !createVolumePermission.getRemove().isEmpty()) ?
                    SnapshotAttribute.CreateVolumePermission :
                    !productCodes.isEmpty() ?
                            SnapshotAttribute.ProductCode : null
        }
    }

    boolean add() {
        !asAddCreateVolumePermissionsItemTypes().isEmpty()
    }

    boolean remove() {
        !asRemoveCreateVolumePermissionsItemTypes().isEmpty()
    }

    List<String> addUserIds() {
        add() ?
                asAddCreateVolumePermissionsItemTypes().collect { CreateVolumePermissionItemType add -> add.userId }.findAll { String id -> id } as List<String> :
                Lists.newArrayList()
    }

    List<String> removeUserIds() {
        remove() ?
                asRemoveCreateVolumePermissionsItemTypes().collect{ CreateVolumePermissionItemType remove -> remove.userId }.findAll{ String id -> id } as List<String> :
                Lists.newArrayList()
    }

    boolean addGroupAll() {
        asAddCreateVolumePermissionsItemTypes().find{ CreateVolumePermissionItemType add -> 'all'.equals( add.getGroup() ) }
    }

    boolean removeGroupAll() {
        asRemoveCreateVolumePermissionsItemTypes().find{ CreateVolumePermissionItemType add -> 'all'.equals( add.getGroup() ) }
    }

    List<CreateVolumePermissionItemType> asAddCreateVolumePermissionsItemTypes() {
        attribute ?
                'add'.equals( operationType ) ? asCreateVolumePermissionItemTypes() : Lists.newArrayList() :
                createVolumePermission.getAdd()
    }

    List<CreateVolumePermissionItemType> asRemoveCreateVolumePermissionsItemTypes() {
        attribute ?
                'add'.equals( operationType ) ? Lists.newArrayList() : asCreateVolumePermissionItemTypes() :
                createVolumePermission.getRemove()
    }

    private List<CreateVolumePermissionItemType> asCreateVolumePermissionItemTypes() {
        queryUserId.isEmpty() ?
                queryUserGroup.collect{ String group -> new CreateVolumePermissionItemType( group: group ) } :
                queryUserId.collect{ String userId -> new CreateVolumePermissionItemType( userId: userId ) }
    }

}

public class DescribeSnapshotAttributeResponseType extends BlockSnapshotMessage {
    String snapshotId
    ArrayList<CreateVolumePermissionItemType> createVolumePermission = Lists.newArrayList( )
    ArrayList<String> productCodes = Lists.newArrayList( )

    boolean hasCreateVolumePermissions() {
        this.createVolumePermission
    }

    boolean hasProductCodes() {
        this.productCodes
    }
}

public class DescribeSnapshotAttributeType extends BlockSnapshotMessage {

    String snapshotId;
    String createVolumePermission = "hi";
    String productCodes = "hi";
    String attribute;

    public void applyAttribute() {
        this.createVolumePermission = null;
        this.productCodes = null;
        this.setProperty(attribute, "hi");
    }
}

public class ResetSnapshotAttributeResponseType extends BlockSnapshotMessage {
    public ResetSnapshotAttributeResponseType() {  }
}

public class ResetSnapshotAttributeType extends BlockSnapshotMessage {
    String snapshotId;
    @HttpParameterMapping (parameter = "Attribute")
    String createVolumePermission;
}

public class DescribeVolumeAttributeType extends BlockVolumeMessage {
  String volumeId
  String attribute
}

public class DescribeVolumeAttributeResponseType extends BlockVolumeMessage {
  String volumeId
  Boolean autoEnableIO
  Boolean productCodes // not supported, would be a list of codes

  boolean hasAutoEnableIO() {
    this.autoEnableIO != null
  }
  boolean hasProductCodes() {
    this.productCodes != null
  }
}

public class DescribeVolumeStatusType extends BlockVolumeMessage {
  ArrayList<String> volumeId
  Integer maxResults
  String nextToken
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}

public class VolumeStatusItemType extends EucalyptusData {
  String volumeId
  String availabilityZone
  String status
  String ioEnabledStatus
}

public class DescribeVolumeStatusResponseType extends BlockVolumeMessage {
  ArrayList<VolumeStatusItemType> volumeStatusSet
}

public class EnableVolumeIOType extends BlockVolumeMessage {
  String volumeId
}

public class EnableVolumeIOResponseType extends BlockVolumeMessage { }

public class ModifyVolumeAttributeType extends BlockVolumeMessage {
  String volumeId
  @HttpParameterMapping (parameter = "AutoEnableIO.Value")
  Boolean autoEnableIO
}

public class ModifyVolumeAttributeResponseType extends BlockVolumeMessage { }
