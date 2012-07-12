
from clcinterface import ClcInterface

# This class provides an implmentation of the clcinterface using canned json
# strings. Might be better to represent as object graph so we can modify
# values in the mock.
class MockClcInterface(ClcInterface):

  def __init__(self):
    pass

  def get_all_images(self):
    return '[ { "root_device_type": "instance-store", "ramdisk_id": null, "id": "eri-197D3A1A", "owner_alias": null, "tags": {}, "platform": "", "state": "available", "location": "admin/initrd.img-2.6.28-11-generic.manifest.xml", "type": "ramdisk", "virtualization_type": null, "architecture": "i386", "description": null, "block_device_mapping": {}, "kernel_id": null, "owner_id": "000000000001", "is_public": true, "instance_lifecycle": null, "name": "eri-197D3A1A", "hypervisor": null, "region": { "connection": null, "endpoint": "192.168.25.10", "name": "eucalyptus", "connection_cls": null }, "item": "", "connection": [], "root_device_name": "/dev/sda1", "ownerId": "000000000001", "product_codes": [] }, { "root_device_type": "instance-store", "ramdisk_id": "eri-197D3A1A", "id": "emi-7B903AAE", "owner_alias": null, "tags": {}, "platform": "", "state": "available", "location": "admin/euca-centos-5.8-2012.05.14-x86_64.manifest.xml", "type": "machine", "virtualization_type": null, "architecture": "i386", "description": null, "block_device_mapping": {}, "kernel_id": "eki-56B43CB2", "owner_id": "072279894205", "is_public": true, "instance_lifecycle": null, "name": "emi-7B903AAE", "hypervisor": null, "region": { "connection": null, "endpoint": "192.168.25.10", "name": "eucalyptus", "connection_cls": null }, "item": "", "connection": [], "root_device_name": "/dev/sda1", "ownerId": "072279894205", "product_codes": [] }, { "root_device_type": "instance-store", "ramdisk_id": "eri-197D3A1A", "id": "emi-BCD33EA4", "owner_alias": null, "tags": {}, "platform": "", "state": "available", "location": "stackato/stackato-img-kvm-v1.2.6.manifest.xml", "type": "machine", "virtualization_type": null, "architecture": "x86_64", "description": "stackato agent image", "block_device_mapping": {}, "kernel_id": "eki-56B43CB2", "owner_id": "072279894205", "is_public": true, "instance_lifecycle": null, "name": "emi-BCD33EA4", "hypervisor": null, "region": { "connection": null, "endpoint": "192.168.25.10", "name": "eucalyptus", "connection_cls": null }, "item": "", "connection": [], "root_device_name": "/dev/sda1", "ownerId": "072279894205", "product_codes": [] }, { "root_device_type": "instance-store", "ramdisk_id": null, "id": "eki-56B43CB2", "owner_alias": null, "tags": {}, "platform": "", "state": "available", "location": "admin/vmlinuz-2.6.28-11-generic.manifest.xml", "type": "kernel", "virtualization_type": null, "architecture": "i386", "description": null, "block_device_mapping": {}, "kernel_id": null, "owner_id": "000000000001", "is_public": true, "instance_lifecycle": null, "name": "eki-56B43CB2", "hypervisor": null, "region": { "connection": null, "endpoint": "192.168.25.10", "name": "eucalyptus", "connection_cls": null }, "item": "", "connection": [], "root_device_name": "/dev/sda1", "ownerId": "000000000001", "product_codes": [] } ]'

  def get_all_instances(self):
    return '[ { "item": "", "region": { "connection": null, "endpoint": "192.168.25.10", "name": "eucalyptus", "connection_cls": null }, "instances": [ { "kernel": "eki-56B43CB2", "root_device_type": "instance-store", "private_dns_name": "0.0.0.0", "previous_state": null, "spot_instance_request_id": null, "id": "i-03C44065", "state_reason": null, "monitored": false, "item": "", "subnet_id": null, "block_device_mapping": {}, "instance_class": null, "shutdown_state": null, "group_name": null, "state": "pending", "client_token": null, "_in_monitoring_element": false, "ramdisk": "eri-197D3A1A", "tags": {}, "key_name": "dakkey", "image_id": "emi-7B903AAE", "reason": "NORMAL: -- []", "groups": [], "public_dns_name": "0.0.0.0", "monitoring": "", "requester_id": null, "state_code": 0, "ip_address": "0.0.0.0", "placement": "PARTI00", "ami_launch_index": "0", "dns_name": "0.0.0.0", "region": { "connection": null, "endpoint": "192.168.25.10", "name": "eucalyptus", "connection_cls": null }, "launch_time": "2012-07-03T17:49:41.269Z", "persistent": false, "instance_type": "m1.small", "connection": [], "root_device_name": "/dev/sda1", "instanceState": "", "private_ip_address": "0.0.0.0", "vpc_id": null, "product_codes": [] } ], "connection": [], "groups": [ { "item": "", "id": "default", "name": null } ], "id": "r-ECE1441F", "owner_id": "072279894205" } ]'

  def get_all_addresses(self):
    return '[]'

  def get_all_key_pairs(self):
    return '[]'

  def get_all_security_groups(self):
    return '[]'

  def get_all_volumes(self):
    return '[]'

  def get_all_snapshots(self):
    return '[]'

