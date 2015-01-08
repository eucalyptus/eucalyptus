/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
@GroovyAddClassUUID
package com.eucalyptus.compute.common.backend

import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.component.id.Eucalyptus
import edu.ucsb.eucalyptus.msgs.BaseMessageMarker
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import groovy.transform.InheritConstructors

@ComponentMessage( Eucalyptus.class )
interface ComputeBackendMessage extends BaseMessageMarker {
}

// Addresses
@InheritConstructors class AllocateAddressType extends com.eucalyptus.compute.common.AllocateAddressType implements ComputeBackendMessage { }
@InheritConstructors class AllocateAddressResponseType extends com.eucalyptus.compute.common.AllocateAddressResponseType implements ComputeBackendMessage { }
@InheritConstructors class AssociateAddressType extends com.eucalyptus.compute.common.AssociateAddressType implements ComputeBackendMessage { }
@InheritConstructors class AssociateAddressResponseType extends com.eucalyptus.compute.common.AssociateAddressResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeAddressesType extends com.eucalyptus.compute.common.DescribeAddressesType implements ComputeBackendMessage { }
@InheritConstructors class DescribeAddressesResponseType extends com.eucalyptus.compute.common.DescribeAddressesResponseType implements ComputeBackendMessage { }
@InheritConstructors class DisassociateAddressType extends com.eucalyptus.compute.common.DisassociateAddressType implements ComputeBackendMessage { }
@InheritConstructors class DisassociateAddressResponseType extends com.eucalyptus.compute.common.DisassociateAddressResponseType implements ComputeBackendMessage { }
@InheritConstructors class ReleaseAddressType extends com.eucalyptus.compute.common.ReleaseAddressType implements ComputeBackendMessage { }
@InheritConstructors class ReleaseAddressResponseType extends com.eucalyptus.compute.common.ReleaseAddressResponseType implements ComputeBackendMessage { }

// Block devices
@InheritConstructors class AttachVolumeType extends com.eucalyptus.compute.common.AttachVolumeType implements ComputeBackendMessage { }
@InheritConstructors class AttachVolumeResponseType extends com.eucalyptus.compute.common.AttachVolumeResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateSnapshotType extends com.eucalyptus.compute.common.CreateSnapshotType implements ComputeBackendMessage { }
@InheritConstructors class CreateSnapshotResponseType extends com.eucalyptus.compute.common.CreateSnapshotResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateVolumeType extends com.eucalyptus.compute.common.CreateVolumeType implements ComputeBackendMessage { }
@InheritConstructors class CreateVolumeResponseType extends com.eucalyptus.compute.common.CreateVolumeResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteSnapshotType extends com.eucalyptus.compute.common.DeleteSnapshotType implements ComputeBackendMessage { }
@InheritConstructors class DeleteSnapshotResponseType extends com.eucalyptus.compute.common.DeleteSnapshotResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVolumeType extends com.eucalyptus.compute.common.DeleteVolumeType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVolumeResponseType extends com.eucalyptus.compute.common.DeleteVolumeResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeSnapshotAttributeType extends com.eucalyptus.compute.common.DescribeSnapshotAttributeType implements ComputeBackendMessage { }
@InheritConstructors class DescribeSnapshotAttributeResponseType extends com.eucalyptus.compute.common.DescribeSnapshotAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeSnapshotsType extends com.eucalyptus.compute.common.DescribeSnapshotsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeSnapshotsResponseType extends com.eucalyptus.compute.common.DescribeSnapshotsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVolumeAttributeType extends com.eucalyptus.compute.common.DescribeVolumeAttributeType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVolumeAttributeResponseType extends com.eucalyptus.compute.common.DescribeVolumeAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVolumeStatusType extends com.eucalyptus.compute.common.DescribeVolumeStatusType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVolumeStatusResponseType extends com.eucalyptus.compute.common.DescribeVolumeStatusResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVolumesType extends com.eucalyptus.compute.common.DescribeVolumesType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVolumesResponseType extends com.eucalyptus.compute.common.DescribeVolumesResponseType implements ComputeBackendMessage { }
@InheritConstructors class DetachVolumeType extends com.eucalyptus.compute.common.DetachVolumeType implements ComputeBackendMessage { }
@InheritConstructors class DetachVolumeResponseType extends com.eucalyptus.compute.common.DetachVolumeResponseType implements ComputeBackendMessage { }
@InheritConstructors class EnableVolumeIOType extends com.eucalyptus.compute.common.EnableVolumeIOType implements ComputeBackendMessage { }
@InheritConstructors class EnableVolumeIOResponseType extends com.eucalyptus.compute.common.EnableVolumeIOResponseType implements ComputeBackendMessage { }
@InheritConstructors class ModifySnapshotAttributeType extends com.eucalyptus.compute.common.ModifySnapshotAttributeType implements ComputeBackendMessage { }
@InheritConstructors class ModifySnapshotAttributeResponseType extends com.eucalyptus.compute.common.ModifySnapshotAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class ModifyVolumeAttributeType extends com.eucalyptus.compute.common.ModifyVolumeAttributeType implements ComputeBackendMessage { }
@InheritConstructors class ModifyVolumeAttributeResponseType extends com.eucalyptus.compute.common.ModifyVolumeAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class ResetSnapshotAttributeType extends com.eucalyptus.compute.common.ResetSnapshotAttributeType implements ComputeBackendMessage { }
@InheritConstructors class ResetSnapshotAttributeResponseType extends com.eucalyptus.compute.common.ResetSnapshotAttributeResponseType implements ComputeBackendMessage { }

// Images
@InheritConstructors class ConfirmProductInstanceType extends com.eucalyptus.compute.common.ConfirmProductInstanceType implements ComputeBackendMessage { }
@InheritConstructors class ConfirmProductInstanceResponseType extends com.eucalyptus.compute.common.ConfirmProductInstanceResponseType implements ComputeBackendMessage { }
@InheritConstructors class CopyImageType extends com.eucalyptus.compute.common.CopyImageType implements ComputeBackendMessage { }
@InheritConstructors class CopyImageResponseType extends com.eucalyptus.compute.common.CopyImageResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateImageType extends com.eucalyptus.compute.common.CreateImageType implements ComputeBackendMessage { }
@InheritConstructors class CreateImageResponseType extends com.eucalyptus.compute.common.CreateImageResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeregisterImageType extends com.eucalyptus.compute.common.DeregisterImageType implements ComputeBackendMessage { }
@InheritConstructors class DeregisterImageResponseType extends com.eucalyptus.compute.common.DeregisterImageResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeImageAttributeType extends com.eucalyptus.compute.common.DescribeImageAttributeType implements ComputeBackendMessage { }
@InheritConstructors class DescribeImageAttributeResponseType extends com.eucalyptus.compute.common.DescribeImageAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeImagesType extends com.eucalyptus.compute.common.DescribeImagesType implements ComputeBackendMessage { }
@InheritConstructors class DescribeImagesResponseType extends com.eucalyptus.compute.common.DescribeImagesResponseType implements ComputeBackendMessage { }
@InheritConstructors class ModifyImageAttributeType extends com.eucalyptus.compute.common.ModifyImageAttributeType implements ComputeBackendMessage { }
@InheritConstructors class ModifyImageAttributeResponseType extends com.eucalyptus.compute.common.ModifyImageAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class RegisterImageType extends com.eucalyptus.compute.common.RegisterImageType implements ComputeBackendMessage { }
@InheritConstructors class RegisterImageResponseType extends com.eucalyptus.compute.common.RegisterImageResponseType implements ComputeBackendMessage { }
@InheritConstructors class ResetImageAttributeType extends com.eucalyptus.compute.common.ResetImageAttributeType implements ComputeBackendMessage { }
@InheritConstructors class ResetImageAttributeResponseType extends com.eucalyptus.compute.common.ResetImageAttributeResponseType implements ComputeBackendMessage { }

// Import / Export
@InheritConstructors class CancelConversionTaskType extends com.eucalyptus.compute.common.CancelConversionTaskType implements ComputeBackendMessage { }
@InheritConstructors class CancelConversionTaskResponseType extends com.eucalyptus.compute.common.CancelConversionTaskResponseType implements ComputeBackendMessage { }
@InheritConstructors class CancelExportTaskType extends com.eucalyptus.compute.common.CancelExportTaskType implements ComputeBackendMessage { }
@InheritConstructors class CancelExportTaskResponseType extends com.eucalyptus.compute.common.CancelExportTaskResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateInstanceExportTaskType extends com.eucalyptus.compute.common.CreateInstanceExportTaskType implements ComputeBackendMessage { }
@InheritConstructors class CreateInstanceExportTaskResponseType extends com.eucalyptus.compute.common.CreateInstanceExportTaskResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeConversionTasksType extends com.eucalyptus.compute.common.DescribeConversionTasksType implements ComputeBackendMessage { }
@InheritConstructors class DescribeConversionTasksResponseType extends com.eucalyptus.compute.common.DescribeConversionTasksResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeExportTasksType extends com.eucalyptus.compute.common.DescribeExportTasksType implements ComputeBackendMessage { }
@InheritConstructors class DescribeExportTasksResponseType extends com.eucalyptus.compute.common.DescribeExportTasksResponseType implements ComputeBackendMessage { }
@InheritConstructors class ImportInstanceType extends com.eucalyptus.compute.common.ImportInstanceType implements ComputeBackendMessage { }
@InheritConstructors class ImportInstanceResponseType extends com.eucalyptus.compute.common.ImportInstanceResponseType implements ComputeBackendMessage { }
@InheritConstructors class ImportVolumeType extends com.eucalyptus.compute.common.ImportVolumeType implements ComputeBackendMessage { }
@InheritConstructors class ImportVolumeResponseType extends com.eucalyptus.compute.common.ImportVolumeResponseType implements ComputeBackendMessage { }

// Instances
@InheritConstructors class DescribeInstanceAttributeResponseType extends com.eucalyptus.compute.common.DescribeInstanceAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeInstanceAttributeType extends com.eucalyptus.compute.common.DescribeInstanceAttributeType implements ComputeBackendMessage { }
@InheritConstructors class DescribeInstancesResponseType extends com.eucalyptus.compute.common.DescribeInstancesResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeInstanceStatusResponseType extends com.eucalyptus.compute.common.DescribeInstanceStatusResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeInstanceStatusType extends com.eucalyptus.compute.common.DescribeInstanceStatusType implements ComputeBackendMessage { }
@InheritConstructors class DescribeInstancesType extends com.eucalyptus.compute.common.DescribeInstancesType implements ComputeBackendMessage { }
@InheritConstructors class GetConsoleOutputResponseType extends com.eucalyptus.compute.common.GetConsoleOutputResponseType implements ComputeBackendMessage { }
@InheritConstructors class GetConsoleOutputType extends com.eucalyptus.compute.common.GetConsoleOutputType implements ComputeBackendMessage { }
@InheritConstructors class GetPasswordDataResponseType extends com.eucalyptus.compute.common.GetPasswordDataResponseType implements ComputeBackendMessage { }
@InheritConstructors class GetPasswordDataType extends com.eucalyptus.compute.common.GetPasswordDataType implements ComputeBackendMessage { }
@InheritConstructors class ModifyInstanceAttributeResponseType extends com.eucalyptus.compute.common.ModifyInstanceAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class ModifyInstanceAttributeType extends com.eucalyptus.compute.common.ModifyInstanceAttributeType implements ComputeBackendMessage { }
@InheritConstructors class MonitorInstancesResponseType extends com.eucalyptus.compute.common.MonitorInstancesResponseType implements ComputeBackendMessage { }
@InheritConstructors class MonitorInstancesType extends com.eucalyptus.compute.common.MonitorInstancesType implements ComputeBackendMessage { }
@InheritConstructors class RebootInstancesResponseType extends com.eucalyptus.compute.common.RebootInstancesResponseType implements ComputeBackendMessage { }
@InheritConstructors class RebootInstancesType extends com.eucalyptus.compute.common.RebootInstancesType implements ComputeBackendMessage { }
@InheritConstructors class ReportInstanceStatusType extends com.eucalyptus.compute.common.ReportInstanceStatusType implements ComputeBackendMessage { }
@InheritConstructors class ReportInstanceStatusResponseType extends com.eucalyptus.compute.common.ReportInstanceStatusResponseType implements ComputeBackendMessage { }
@InheritConstructors class ResetInstanceAttributeResponseType extends com.eucalyptus.compute.common.ResetInstanceAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class ResetInstanceAttributeType extends com.eucalyptus.compute.common.ResetInstanceAttributeType implements ComputeBackendMessage { }
@InheritConstructors class RunInstancesResponseType extends com.eucalyptus.compute.common.RunInstancesResponseType implements ComputeBackendMessage { }
@InheritConstructors class RunInstancesType extends com.eucalyptus.compute.common.RunInstancesType implements ComputeBackendMessage { }
@InheritConstructors class StartInstancesResponseType extends com.eucalyptus.compute.common.StartInstancesResponseType implements ComputeBackendMessage { }
@InheritConstructors class StartInstancesType extends com.eucalyptus.compute.common.StartInstancesType implements ComputeBackendMessage { }
@InheritConstructors class StopInstancesResponseType extends com.eucalyptus.compute.common.StopInstancesResponseType implements ComputeBackendMessage { }
@InheritConstructors class StopInstancesType extends com.eucalyptus.compute.common.StopInstancesType implements ComputeBackendMessage { }
@InheritConstructors class TerminateInstancesResponseType extends com.eucalyptus.compute.common.TerminateInstancesResponseType implements ComputeBackendMessage { }
@InheritConstructors class TerminateInstancesType extends com.eucalyptus.compute.common.TerminateInstancesType implements ComputeBackendMessage { }
@InheritConstructors class UnmonitorInstancesResponseType extends com.eucalyptus.compute.common.UnmonitorInstancesResponseType implements ComputeBackendMessage { }
@InheritConstructors class UnmonitorInstancesType extends com.eucalyptus.compute.common.UnmonitorInstancesType implements ComputeBackendMessage { }

// Instance types
@InheritConstructors class DescribeInstanceTypesType extends com.eucalyptus.compute.common.DescribeInstanceTypesType implements ComputeBackendMessage { }
@InheritConstructors class DescribeInstanceTypesResponseType extends com.eucalyptus.compute.common.DescribeInstanceTypesResponseType implements ComputeBackendMessage { }
@InheritConstructors class ModifyInstanceTypeAttributeType extends com.eucalyptus.compute.common.ModifyInstanceTypeAttributeType implements ComputeBackendMessage { }
@InheritConstructors class ModifyInstanceTypeAttributeResponseType extends com.eucalyptus.compute.common.ModifyInstanceTypeAttributeResponseType implements ComputeBackendMessage { }

// Key pairs
@InheritConstructors class CreateKeyPairType extends com.eucalyptus.compute.common.CreateKeyPairType implements ComputeBackendMessage { }
@InheritConstructors class CreateKeyPairResponseType extends com.eucalyptus.compute.common.CreateKeyPairResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteKeyPairType extends com.eucalyptus.compute.common.DeleteKeyPairType implements ComputeBackendMessage { }
@InheritConstructors class DeleteKeyPairResponseType extends com.eucalyptus.compute.common.DeleteKeyPairResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeKeyPairsType extends com.eucalyptus.compute.common.DescribeKeyPairsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeKeyPairsResponseType extends com.eucalyptus.compute.common.DescribeKeyPairsResponseType implements ComputeBackendMessage { }
@InheritConstructors class ImportKeyPairType extends com.eucalyptus.compute.common.ImportKeyPairType implements ComputeBackendMessage { }
@InheritConstructors class ImportKeyPairResponseType extends com.eucalyptus.compute.common.ImportKeyPairResponseType implements ComputeBackendMessage { }

// Location
@InheritConstructors class DescribeAvailabilityZonesType extends com.eucalyptus.compute.common.DescribeAvailabilityZonesType implements ComputeBackendMessage { }
@InheritConstructors class DescribeAvailabilityZonesResponseType extends com.eucalyptus.compute.common.DescribeAvailabilityZonesResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeRegionsType extends com.eucalyptus.compute.common.DescribeRegionsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeRegionsResponseType extends com.eucalyptus.compute.common.DescribeRegionsResponseType implements ComputeBackendMessage { }
@InheritConstructors class MigrateInstancesType extends com.eucalyptus.compute.common.MigrateInstancesType implements ComputeBackendMessage { }
@InheritConstructors class MigrateInstancesResponseType extends com.eucalyptus.compute.common.MigrateInstancesResponseType implements ComputeBackendMessage { }

// Placement groups
@InheritConstructors class CreatePlacementGroupResponseType extends com.eucalyptus.compute.common.CreatePlacementGroupResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreatePlacementGroupType extends com.eucalyptus.compute.common.CreatePlacementGroupType implements ComputeBackendMessage { }
@InheritConstructors class DeletePlacementGroupResponseType extends com.eucalyptus.compute.common.DeletePlacementGroupResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeletePlacementGroupType extends com.eucalyptus.compute.common.DeletePlacementGroupType implements ComputeBackendMessage { }
@InheritConstructors class DescribePlacementGroupsResponseType extends com.eucalyptus.compute.common.DescribePlacementGroupsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribePlacementGroupsType extends com.eucalyptus.compute.common.DescribePlacementGroupsType implements ComputeBackendMessage { }

// Reserved instances
@InheritConstructors class DescribeReservedInstancesListingsResponseType extends com.eucalyptus.compute.common.DescribeReservedInstancesListingsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeReservedInstancesListingsType extends com.eucalyptus.compute.common.DescribeReservedInstancesListingsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeReservedInstancesModificationsResponseType extends com.eucalyptus.compute.common.DescribeReservedInstancesModificationsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeReservedInstancesModificationsType extends com.eucalyptus.compute.common.DescribeReservedInstancesModificationsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeReservedInstancesOfferingsResponseType extends com.eucalyptus.compute.common.DescribeReservedInstancesOfferingsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeReservedInstancesOfferingsType extends com.eucalyptus.compute.common.DescribeReservedInstancesOfferingsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeReservedInstancesResponseType extends com.eucalyptus.compute.common.DescribeReservedInstancesResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeReservedInstancesType extends com.eucalyptus.compute.common.DescribeReservedInstancesType implements ComputeBackendMessage { }
@InheritConstructors class CreateReservedInstancesListingType extends com.eucalyptus.compute.common.CreateReservedInstancesListingType implements ComputeBackendMessage { }
@InheritConstructors class CreateReservedInstancesListingResponseType extends com.eucalyptus.compute.common.CreateReservedInstancesListingResponseType implements ComputeBackendMessage { }
@InheritConstructors class CancelReservedInstancesListingType extends com.eucalyptus.compute.common.CancelReservedInstancesListingType implements ComputeBackendMessage { }
@InheritConstructors class CancelReservedInstancesListingResponseType extends com.eucalyptus.compute.common.CancelReservedInstancesListingResponseType implements ComputeBackendMessage { }
@InheritConstructors class ModifyReservedInstancesType extends com.eucalyptus.compute.common.ModifyReservedInstancesType implements ComputeBackendMessage { }
@InheritConstructors class ModifyReservedInstancesResponseType extends com.eucalyptus.compute.common.ModifyReservedInstancesResponseType implements ComputeBackendMessage { }
@InheritConstructors class PurchaseReservedInstancesOfferingType extends com.eucalyptus.compute.common.PurchaseReservedInstancesOfferingType implements ComputeBackendMessage { }
@InheritConstructors class PurchaseReservedInstancesOfferingResponseType extends com.eucalyptus.compute.common.PurchaseReservedInstancesOfferingResponseType implements ComputeBackendMessage { }

// Security groups
@InheritConstructors class AuthorizeSecurityGroupEgressType extends com.eucalyptus.compute.common.AuthorizeSecurityGroupEgressType implements ComputeBackendMessage { }
@InheritConstructors class AuthorizeSecurityGroupEgressResponseType extends com.eucalyptus.compute.common.AuthorizeSecurityGroupEgressResponseType implements ComputeBackendMessage { }
@InheritConstructors class AuthorizeSecurityGroupIngressType extends com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressType implements ComputeBackendMessage { }
@InheritConstructors class AuthorizeSecurityGroupIngressResponseType extends com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateSecurityGroupType extends com.eucalyptus.compute.common.CreateSecurityGroupType implements ComputeBackendMessage { }
@InheritConstructors class CreateSecurityGroupResponseType extends com.eucalyptus.compute.common.CreateSecurityGroupResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteSecurityGroupType extends com.eucalyptus.compute.common.DeleteSecurityGroupType implements ComputeBackendMessage { }
@InheritConstructors class DeleteSecurityGroupResponseType extends com.eucalyptus.compute.common.DeleteSecurityGroupResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeSecurityGroupsType extends com.eucalyptus.compute.common.DescribeSecurityGroupsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeSecurityGroupsResponseType extends com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType implements ComputeBackendMessage { }
@InheritConstructors class RevokeSecurityGroupEgressType extends com.eucalyptus.compute.common.RevokeSecurityGroupEgressType implements ComputeBackendMessage { }
@InheritConstructors class RevokeSecurityGroupEgressResponseType extends com.eucalyptus.compute.common.RevokeSecurityGroupEgressResponseType implements ComputeBackendMessage { }
@InheritConstructors class RevokeSecurityGroupIngressType extends com.eucalyptus.compute.common.RevokeSecurityGroupIngressType implements ComputeBackendMessage { }
@InheritConstructors class RevokeSecurityGroupIngressResponseType extends com.eucalyptus.compute.common.RevokeSecurityGroupIngressResponseType implements ComputeBackendMessage { }

// Spot instances
@InheritConstructors class DescribeSpotInstanceRequestsType extends com.eucalyptus.compute.common.DescribeSpotInstanceRequestsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeSpotInstanceRequestsResponseType extends com.eucalyptus.compute.common.DescribeSpotInstanceRequestsResponseType implements ComputeBackendMessage { }
@InheritConstructors class CancelSpotInstanceRequestsType extends com.eucalyptus.compute.common.CancelSpotInstanceRequestsType implements ComputeBackendMessage { }
@InheritConstructors class CancelSpotInstanceRequestsResponseType extends com.eucalyptus.compute.common.CancelSpotInstanceRequestsResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateSpotDatafeedSubscriptionType extends com.eucalyptus.compute.common.CreateSpotDatafeedSubscriptionType implements ComputeBackendMessage { }
@InheritConstructors class CreateSpotDatafeedSubscriptionResponseType extends com.eucalyptus.compute.common.CreateSpotDatafeedSubscriptionResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteSpotDatafeedSubscriptionType extends com.eucalyptus.compute.common.DeleteSpotDatafeedSubscriptionType implements ComputeBackendMessage { }
@InheritConstructors class DeleteSpotDatafeedSubscriptionResponseType extends com.eucalyptus.compute.common.DeleteSpotDatafeedSubscriptionResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeSpotDatafeedSubscriptionType extends com.eucalyptus.compute.common.DescribeSpotDatafeedSubscriptionType implements ComputeBackendMessage { }
@InheritConstructors class DescribeSpotDatafeedSubscriptionResponseType extends com.eucalyptus.compute.common.DescribeSpotDatafeedSubscriptionResponseType implements ComputeBackendMessage { }
@InheritConstructors class RequestSpotInstancesType extends com.eucalyptus.compute.common.RequestSpotInstancesType implements ComputeBackendMessage { }
@InheritConstructors class RequestSpotInstancesResponseType extends com.eucalyptus.compute.common.RequestSpotInstancesResponseType implements ComputeBackendMessage { }

// Tagging
@InheritConstructors class CreateTagsType extends com.eucalyptus.compute.common.CreateTagsType implements ComputeBackendMessage { }
@InheritConstructors class CreateTagsResponseType extends com.eucalyptus.compute.common.CreateTagsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteTagsType extends com.eucalyptus.compute.common.DeleteTagsType implements ComputeBackendMessage { }
@InheritConstructors class DeleteTagsResponseType extends com.eucalyptus.compute.common.DeleteTagsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeTagsType extends com.eucalyptus.compute.common.DescribeTagsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeTagsResponseType extends com.eucalyptus.compute.common.DescribeTagsResponseType implements ComputeBackendMessage { }

// VPC
@InheritConstructors class AcceptVpcPeeringConnectionType extends com.eucalyptus.compute.common.AcceptVpcPeeringConnectionType implements ComputeBackendMessage { }
@InheritConstructors class AcceptVpcPeeringConnectionResponseType extends com.eucalyptus.compute.common.AcceptVpcPeeringConnectionResponseType implements ComputeBackendMessage { }
@InheritConstructors class AssignPrivateIpAddressesType extends com.eucalyptus.compute.common.AssignPrivateIpAddressesType implements ComputeBackendMessage { }
@InheritConstructors class AssignPrivateIpAddressesResponseType extends com.eucalyptus.compute.common.AssignPrivateIpAddressesResponseType implements ComputeBackendMessage { }
@InheritConstructors class AssociateDhcpOptionsType extends com.eucalyptus.compute.common.AssociateDhcpOptionsType implements ComputeBackendMessage { }
@InheritConstructors class AssociateDhcpOptionsResponseType extends com.eucalyptus.compute.common.AssociateDhcpOptionsResponseType implements ComputeBackendMessage { }
@InheritConstructors class AssociateRouteTableType extends com.eucalyptus.compute.common.AssociateRouteTableType implements ComputeBackendMessage { }
@InheritConstructors class AssociateRouteTableResponseType extends com.eucalyptus.compute.common.AssociateRouteTableResponseType implements ComputeBackendMessage { }
@InheritConstructors class AttachInternetGatewayType extends com.eucalyptus.compute.common.AttachInternetGatewayType implements ComputeBackendMessage { }
@InheritConstructors class AttachInternetGatewayResponseType extends com.eucalyptus.compute.common.AttachInternetGatewayResponseType implements ComputeBackendMessage { }
@InheritConstructors class AttachNetworkInterfaceType extends com.eucalyptus.compute.common.AttachNetworkInterfaceType implements ComputeBackendMessage { }
@InheritConstructors class AttachNetworkInterfaceResponseType extends com.eucalyptus.compute.common.AttachNetworkInterfaceResponseType implements ComputeBackendMessage { }
@InheritConstructors class AttachVpnGatewayType extends com.eucalyptus.compute.common.AttachVpnGatewayType implements ComputeBackendMessage { }
@InheritConstructors class AttachVpnGatewayResponseType extends com.eucalyptus.compute.common.AttachVpnGatewayResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateCustomerGatewayType extends com.eucalyptus.compute.common.CreateCustomerGatewayType implements ComputeBackendMessage { }
@InheritConstructors class CreateCustomerGatewayResponseType extends com.eucalyptus.compute.common.CreateCustomerGatewayResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateDhcpOptionsType extends com.eucalyptus.compute.common.CreateDhcpOptionsType implements ComputeBackendMessage { }
@InheritConstructors class CreateDhcpOptionsResponseType extends com.eucalyptus.compute.common.CreateDhcpOptionsResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateInternetGatewayType extends com.eucalyptus.compute.common.CreateInternetGatewayType implements ComputeBackendMessage { }
@InheritConstructors class CreateInternetGatewayResponseType extends com.eucalyptus.compute.common.CreateInternetGatewayResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateNetworkAclEntryType extends com.eucalyptus.compute.common.CreateNetworkAclEntryType implements ComputeBackendMessage { }
@InheritConstructors class CreateNetworkAclEntryResponseType extends com.eucalyptus.compute.common.CreateNetworkAclEntryResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateNetworkAclType extends com.eucalyptus.compute.common.CreateNetworkAclType implements ComputeBackendMessage { }
@InheritConstructors class CreateNetworkAclResponseType extends com.eucalyptus.compute.common.CreateNetworkAclResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateNetworkInterfaceType extends com.eucalyptus.compute.common.CreateNetworkInterfaceType implements ComputeBackendMessage { }
@InheritConstructors class CreateNetworkInterfaceResponseType extends com.eucalyptus.compute.common.CreateNetworkInterfaceResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateRouteType extends com.eucalyptus.compute.common.CreateRouteType implements ComputeBackendMessage { }
@InheritConstructors class CreateRouteResponseType extends com.eucalyptus.compute.common.CreateRouteResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateRouteTableType extends com.eucalyptus.compute.common.CreateRouteTableType implements ComputeBackendMessage { }
@InheritConstructors class CreateRouteTableResponseType extends com.eucalyptus.compute.common.CreateRouteTableResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateSubnetType extends com.eucalyptus.compute.common.CreateSubnetType implements ComputeBackendMessage { }
@InheritConstructors class CreateSubnetResponseType extends com.eucalyptus.compute.common.CreateSubnetResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateVpcPeeringConnectionType extends com.eucalyptus.compute.common.CreateVpcPeeringConnectionType implements ComputeBackendMessage { }
@InheritConstructors class CreateVpcPeeringConnectionResponseType extends com.eucalyptus.compute.common.CreateVpcPeeringConnectionResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateVpcType extends com.eucalyptus.compute.common.CreateVpcType implements ComputeBackendMessage { }
@InheritConstructors class CreateVpcResponseType extends com.eucalyptus.compute.common.CreateVpcResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateVpnConnectionType extends com.eucalyptus.compute.common.CreateVpnConnectionType implements ComputeBackendMessage { }
@InheritConstructors class CreateVpnConnectionResponseType extends com.eucalyptus.compute.common.CreateVpnConnectionResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateVpnConnectionRouteType extends com.eucalyptus.compute.common.CreateVpnConnectionRouteType implements ComputeBackendMessage { }
@InheritConstructors class CreateVpnConnectionRouteResponseType extends com.eucalyptus.compute.common.CreateVpnConnectionRouteResponseType implements ComputeBackendMessage { }
@InheritConstructors class CreateVpnGatewayType extends com.eucalyptus.compute.common.CreateVpnGatewayType implements ComputeBackendMessage { }
@InheritConstructors class CreateVpnGatewayResponseType extends com.eucalyptus.compute.common.CreateVpnGatewayResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteCustomerGatewayType extends com.eucalyptus.compute.common.DeleteCustomerGatewayType implements ComputeBackendMessage { }
@InheritConstructors class DeleteCustomerGatewayResponseType extends com.eucalyptus.compute.common.DeleteCustomerGatewayResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteDhcpOptionsType extends com.eucalyptus.compute.common.DeleteDhcpOptionsType implements ComputeBackendMessage { }
@InheritConstructors class DeleteDhcpOptionsResponseType extends com.eucalyptus.compute.common.DeleteDhcpOptionsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteInternetGatewayType extends com.eucalyptus.compute.common.DeleteInternetGatewayType implements ComputeBackendMessage { }
@InheritConstructors class DeleteInternetGatewayResponseType extends com.eucalyptus.compute.common.DeleteInternetGatewayResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteNetworkAclEntryType extends com.eucalyptus.compute.common.DeleteNetworkAclEntryType implements ComputeBackendMessage { }
@InheritConstructors class DeleteNetworkAclEntryResponseType extends com.eucalyptus.compute.common.DeleteNetworkAclEntryResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteNetworkAclType extends com.eucalyptus.compute.common.DeleteNetworkAclType implements ComputeBackendMessage { }
@InheritConstructors class DeleteNetworkAclResponseType extends com.eucalyptus.compute.common.DeleteNetworkAclResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteNetworkInterfaceType extends com.eucalyptus.compute.common.DeleteNetworkInterfaceType implements ComputeBackendMessage { }
@InheritConstructors class DeleteNetworkInterfaceResponseType extends com.eucalyptus.compute.common.DeleteNetworkInterfaceResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteRouteType extends com.eucalyptus.compute.common.DeleteRouteType implements ComputeBackendMessage { }
@InheritConstructors class DeleteRouteResponseType extends com.eucalyptus.compute.common.DeleteRouteResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteRouteTableType extends com.eucalyptus.compute.common.DeleteRouteTableType implements ComputeBackendMessage { }
@InheritConstructors class DeleteRouteTableResponseType extends com.eucalyptus.compute.common.DeleteRouteTableResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteSubnetType extends com.eucalyptus.compute.common.DeleteSubnetType implements ComputeBackendMessage { }
@InheritConstructors class DeleteSubnetResponseType extends com.eucalyptus.compute.common.DeleteSubnetResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVpcPeeringConnectionType extends com.eucalyptus.compute.common.DeleteVpcPeeringConnectionType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVpcPeeringConnectionResponseType extends com.eucalyptus.compute.common.DeleteVpcPeeringConnectionResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVpcType extends com.eucalyptus.compute.common.DeleteVpcType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVpcResponseType extends com.eucalyptus.compute.common.DeleteVpcResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVpnConnectionType extends com.eucalyptus.compute.common.DeleteVpnConnectionType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVpnConnectionResponseType extends com.eucalyptus.compute.common.DeleteVpnConnectionResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVpnConnectionRouteType extends com.eucalyptus.compute.common.DeleteVpnConnectionRouteType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVpnConnectionRouteResponseType extends com.eucalyptus.compute.common.DeleteVpnConnectionRouteResponseType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVpnGatewayType extends com.eucalyptus.compute.common.DeleteVpnGatewayType implements ComputeBackendMessage { }
@InheritConstructors class DeleteVpnGatewayResponseType extends com.eucalyptus.compute.common.DeleteVpnGatewayResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeAccountAttributesType extends com.eucalyptus.compute.common.DescribeAccountAttributesType implements ComputeBackendMessage { }
@InheritConstructors class DescribeAccountAttributesResponseType extends com.eucalyptus.compute.common.DescribeAccountAttributesResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeCustomerGatewaysType extends com.eucalyptus.compute.common.DescribeCustomerGatewaysType implements ComputeBackendMessage { }
@InheritConstructors class DescribeCustomerGatewaysResponseType extends com.eucalyptus.compute.common.DescribeCustomerGatewaysResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeDhcpOptionsType extends com.eucalyptus.compute.common.DescribeDhcpOptionsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeDhcpOptionsResponseType extends com.eucalyptus.compute.common.DescribeDhcpOptionsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeInternetGatewaysType extends com.eucalyptus.compute.common.DescribeInternetGatewaysType implements ComputeBackendMessage { }
@InheritConstructors class DescribeInternetGatewaysResponseType extends com.eucalyptus.compute.common.DescribeInternetGatewaysResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeNetworkAclsType extends com.eucalyptus.compute.common.DescribeNetworkAclsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeNetworkAclsResponseType extends com.eucalyptus.compute.common.DescribeNetworkAclsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeNetworkInterfaceAttributeType extends com.eucalyptus.compute.common.DescribeNetworkInterfaceAttributeType implements ComputeBackendMessage { }
@InheritConstructors class DescribeNetworkInterfaceAttributeResponseType extends com.eucalyptus.compute.common.DescribeNetworkInterfaceAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeNetworkInterfacesType extends com.eucalyptus.compute.common.DescribeNetworkInterfacesType implements ComputeBackendMessage { }
@InheritConstructors class DescribeNetworkInterfacesResponseType extends com.eucalyptus.compute.common.DescribeNetworkInterfacesResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeRouteTablesType extends com.eucalyptus.compute.common.DescribeRouteTablesType implements ComputeBackendMessage { }
@InheritConstructors class DescribeRouteTablesResponseType extends com.eucalyptus.compute.common.DescribeRouteTablesResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeSubnetsType extends com.eucalyptus.compute.common.DescribeSubnetsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeSubnetsResponseType extends com.eucalyptus.compute.common.DescribeSubnetsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVpcAttributeType extends com.eucalyptus.compute.common.DescribeVpcAttributeType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVpcAttributeResponseType extends com.eucalyptus.compute.common.DescribeVpcAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVpcPeeringConnectionsType extends com.eucalyptus.compute.common.DescribeVpcPeeringConnectionsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVpcPeeringConnectionsResponseType extends com.eucalyptus.compute.common.DescribeVpcPeeringConnectionsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVpcsType extends com.eucalyptus.compute.common.DescribeVpcsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVpcsResponseType extends com.eucalyptus.compute.common.DescribeVpcsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVpnConnectionsType extends com.eucalyptus.compute.common.DescribeVpnConnectionsType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVpnConnectionsResponseType extends com.eucalyptus.compute.common.DescribeVpnConnectionsResponseType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVpnGatewaysType extends com.eucalyptus.compute.common.DescribeVpnGatewaysType implements ComputeBackendMessage { }
@InheritConstructors class DescribeVpnGatewaysResponseType extends com.eucalyptus.compute.common.DescribeVpnGatewaysResponseType implements ComputeBackendMessage { }
@InheritConstructors class DetachInternetGatewayType extends com.eucalyptus.compute.common.DetachInternetGatewayType implements ComputeBackendMessage { }
@InheritConstructors class DetachInternetGatewayResponseType extends com.eucalyptus.compute.common.DetachInternetGatewayResponseType implements ComputeBackendMessage { }
@InheritConstructors class DetachNetworkInterfaceType extends com.eucalyptus.compute.common.DetachNetworkInterfaceType implements ComputeBackendMessage { }
@InheritConstructors class DetachNetworkInterfaceResponseType extends com.eucalyptus.compute.common.DetachNetworkInterfaceResponseType implements ComputeBackendMessage { }
@InheritConstructors class DetachVpnGatewayType extends com.eucalyptus.compute.common.DetachVpnGatewayType implements ComputeBackendMessage { }
@InheritConstructors class DetachVpnGatewayResponseType extends com.eucalyptus.compute.common.DetachVpnGatewayResponseType implements ComputeBackendMessage { }
@InheritConstructors class DisableVgwRoutePropagationType extends com.eucalyptus.compute.common.DisableVgwRoutePropagationType implements ComputeBackendMessage { }
@InheritConstructors class DisableVgwRoutePropagationResponseType extends com.eucalyptus.compute.common.DisableVgwRoutePropagationResponseType implements ComputeBackendMessage { }
@InheritConstructors class DisassociateRouteTableType extends com.eucalyptus.compute.common.DisassociateRouteTableType implements ComputeBackendMessage { }
@InheritConstructors class DisassociateRouteTableResponseType extends com.eucalyptus.compute.common.DisassociateRouteTableResponseType implements ComputeBackendMessage { }
@InheritConstructors class EnableVgwRoutePropagationType extends com.eucalyptus.compute.common.EnableVgwRoutePropagationType implements ComputeBackendMessage { }
@InheritConstructors class EnableVgwRoutePropagationResponseType extends com.eucalyptus.compute.common.EnableVgwRoutePropagationResponseType implements ComputeBackendMessage { }
@InheritConstructors class ModifyNetworkInterfaceAttributeType extends com.eucalyptus.compute.common.ModifyNetworkInterfaceAttributeType implements ComputeBackendMessage { }
@InheritConstructors class ModifyNetworkInterfaceAttributeResponseType extends com.eucalyptus.compute.common.ModifyNetworkInterfaceAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class ModifySubnetAttributeType extends com.eucalyptus.compute.common.ModifySubnetAttributeType implements ComputeBackendMessage { }
@InheritConstructors class ModifySubnetAttributeResponseType extends com.eucalyptus.compute.common.ModifySubnetAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class ModifyVpcAttributeType extends com.eucalyptus.compute.common.ModifyVpcAttributeType implements ComputeBackendMessage { }
@InheritConstructors class ModifyVpcAttributeResponseType extends com.eucalyptus.compute.common.ModifyVpcAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class RejectVpcPeeringConnectionType extends com.eucalyptus.compute.common.RejectVpcPeeringConnectionType implements ComputeBackendMessage { }
@InheritConstructors class RejectVpcPeeringConnectionResponseType extends com.eucalyptus.compute.common.RejectVpcPeeringConnectionResponseType implements ComputeBackendMessage { }
@InheritConstructors class ReplaceNetworkAclAssociationType extends com.eucalyptus.compute.common.ReplaceNetworkAclAssociationType implements ComputeBackendMessage { }
@InheritConstructors class ReplaceNetworkAclAssociationResponseType extends com.eucalyptus.compute.common.ReplaceNetworkAclAssociationResponseType implements ComputeBackendMessage { }
@InheritConstructors class ReplaceNetworkAclEntryType extends com.eucalyptus.compute.common.ReplaceNetworkAclEntryType implements ComputeBackendMessage { }
@InheritConstructors class ReplaceNetworkAclEntryResponseType extends com.eucalyptus.compute.common.ReplaceNetworkAclEntryResponseType implements ComputeBackendMessage { }
@InheritConstructors class ReplaceRouteType extends com.eucalyptus.compute.common.ReplaceRouteType implements ComputeBackendMessage { }
@InheritConstructors class ReplaceRouteResponseType extends com.eucalyptus.compute.common.ReplaceRouteResponseType implements ComputeBackendMessage { }
@InheritConstructors class ReplaceRouteTableAssociationType extends com.eucalyptus.compute.common.ReplaceRouteTableAssociationType implements ComputeBackendMessage { }
@InheritConstructors class ReplaceRouteTableAssociationResponseType extends com.eucalyptus.compute.common.ReplaceRouteTableAssociationResponseType implements ComputeBackendMessage { }
@InheritConstructors class ResetNetworkInterfaceAttributeType extends com.eucalyptus.compute.common.ResetNetworkInterfaceAttributeType implements ComputeBackendMessage { }
@InheritConstructors class ResetNetworkInterfaceAttributeResponseType extends com.eucalyptus.compute.common.ResetNetworkInterfaceAttributeResponseType implements ComputeBackendMessage { }
@InheritConstructors class UnassignPrivateIpAddressesType extends com.eucalyptus.compute.common.UnassignPrivateIpAddressesType implements ComputeBackendMessage { }
@InheritConstructors class UnassignPrivateIpAddressesResponseType extends com.eucalyptus.compute.common.UnassignPrivateIpAddressesResponseType implements ComputeBackendMessage { }

