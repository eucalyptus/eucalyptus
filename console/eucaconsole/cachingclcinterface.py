# Copyright 2012 Eucalyptus Systems, Inc.
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the following
# conditions are met:
#
#   Redistributions of source code must retain the above copyright notice,
#   this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in the
#   documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

from cache import Cache
import ConfigParser
import functools
import logging

from boto.ec2.ec2object import EC2Object
from boto.ec2.image import Image
from boto.ec2.instance import Instance
from boto.ec2.keypair import KeyPair
from eucaconsole.threads import Threads

from .clcinterface import ClcInterface

# This class provides an implmentation of the clcinterface that caches responses
# from the underlying clcinterface. It will only make requests to the underlying layer
# at the rate defined by pollfreq. It is assumed this will be created per-session and
# therefore will only contain data for a single user. If a more global cache is desired,
# some things will need to be re-written.
class CachingClcInterface(ClcInterface):
    clc = None
    caches = None

    def __init__(self, clcinterface, config):
        self.caches = {}
        self.clc = clcinterface
        pollfreq = config.getint('server', 'pollfreq')
        if pollfreq < 5:    # let's say min frequency is 5
            pollfreq = 5
        try:
            freq = config.getint('server', 'pollfreq.zones')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.caches['zones'] = Cache('zone', freq, self.clc.get_all_zones)

        try:
            freq = config.getint('server', 'pollfreq.images')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.caches['images'] = Cache('image', freq, self.clc.get_all_images)

        try:
            freq = config.getint('server', 'pollfreq.instances')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.caches['instances'] = Cache('instance', freq, self.clc.get_all_instances)

        try:
            freq = config.getint('server', 'pollfreq.keypairs')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.caches['keypairs'] = Cache('keypair', freq, self.clc.get_all_key_pairs)

        try:
            freq = config.getint('server', 'pollfreq.groups')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.caches['groups'] = Cache('sgroup', freq, self.clc.get_all_security_groups)

        try:
            freq = config.getint('server', 'pollfreq.addresses')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.caches['addresses'] = Cache('eip', freq, self.clc.get_all_addresses)

        try:
            freq = config.getint('server', 'pollfreq.volumes')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.caches['volumes'] = Cache('volume', freq, self.clc.get_all_volumes)

        try:
            freq = config.getint('server', 'pollfreq.snapshots')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.caches['snapshots'] = Cache('snapshot', freq, self.clc.get_all_snapshots)

        try:
            freq = config.getint('server', 'pollfreq.tags')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.caches['tags'] = Cache('tag', freq, self.clc.get_all_tags)

    def __normalize_instances__(self, instances):
        ret = []
        if not(instances):
            return []
        for res in instances:
            if issubclass(res.__class__, EC2Object):
                for inst in res.instances:
                    inst.reservation_id = res.id
                    inst.owner_id = res.owner_id
                    inst.groups = res.groups
                    if res.groups:
                        inst.group_name = res.groups[0].name
                    ret.append(inst)
            else:
                for inst in res['instances']:
                    inst['reservation_id'] = res['id']
                    inst['owner_id'] = res['owner_id']
                    inst['groups'] = res['groups']
                    if res['groups']:
                        inst['group_name'] = res['groups'][0]['id']
                    ret.append(inst)
        return ret

    def get_all_regions(self, filters, callback):
        Threads.instance().runThread(self.__get_all_regions_cb__,
                    ({'filters':filters}, callback))

    def __get_all_regions_cb__(self, kwargs, callback):
        try:
            ret = self.clc.get_all_regions(kwargs['filters'])
            logging.info("regions = "+str(ret))
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_zones(self, filters, callback):
        callback(Response(data=self.caches['zones'].values))

    def get_all_images(self, owners, filters, callback):
        callback(Response(data=self.caches['images'].values))

    # returns list of image attributes
    def get_image_attribute(self, image_id, attribute):
        Threads.instance().runThread(self.__get_image_attribute_cb__,
                    ({'image_id':image_id, 'attribute':attribute}, callback))

    def __get_image_attribute_cb__(self, kwargs, callback):
        try:
            ret = self.clc.get_image_attribute(kwargs['image_id'], kwargs['attribute'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def modify_image_attribute(self, image_id, attribute, operation, users, groups):
        self.caches['images'].expireCache()
        Threads.instance().runThread(self.__modify_image_attribute_cb__,
                    ({'image_id':image_id, 'attribute':attribute,
                      'operation':operation, 'users':users, 'groups':groups}, callback))


    def __modify_image_attribute_cb__(self, kwargs, callback):
        try:
            ret = self.clc.modify_image_attribute(kwargs['image_id'], kwargs['attribute'], kwargs['operation'], kwargs['users'], kwargs['groups'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def reset_image_attribute(self, image_id, attribute):
        self.caches['images'].expireCache()
        Threads.instance().runThread(self.__reset_image_attribute_cb__,
                    ({'image_id':image_id, 'attribute':attribute}, callback))

    def __reset_image_attribute_cb__(self, kwargs, callback):
        try:
            ret = self.clc.reset_image_attribute(kwargs['image_id'], kwargs['attribute'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_instances(self, filters, callback):
        callback(Response(data=self.__normalize_instances__(self.caches['instances'].values)))

    def run_instances(self, image_id, min_count=1, max_count=1,
                      key_name=None, security_groups=None,
                      user_data=None, addressing_type=None,
                      instance_type='m1.small', placement=None,
                      kernel_id=None, ramdisk_id=None,
                      monitoring_enabled=False, subnet_id=None,
                      block_device_map=None,
                      disable_api_termination=False,
                      instance_initiated_shutdown_behavior=None,
                      private_ip_address=None,
                      placement_group=None, client_token=None,
                      security_group_ids=None,
                      additional_info=None, instance_profile_name=None,
                      instance_profile_arn=None, tenancy=None, callback=None):
        self.caches['instances'].expireCache()
        Threads.instance().runThread(self.__run_instances_cb__,
                    ({'image_id':image_id, 'min_count':min_count, 'max_count':max_count,
                      'key_name':key_name, 'security_groups':security_groups,
                      'user_data':user_data, 'addressing_type':addressing_type,
                      'instance_type':instance_type, 'placement':placement,
                      'kernel_id':kernel_id, 'ramdisk_id':ramdisk_id,
                      'monitoring_enabled':monitoring_enabled, 'subnet_id':subnet_id,
                      'block_device_map':block_device_map,
                      'disable_api_termination':disable_api_termination,
                      'instance_initiated_shutdown_behavior':instance_initiated_shutdown_behavior,
                      'private_ip_address':private_ip_address,
                      'placement_group':placement_group, 'client_token':client_token,
                      'security_group_ids':security_group_ids,
                      'additional_info':additional_info,
                      'instance_profile_name':instance_profile_name,
                      'instance_profile_arn':instance_profile_arn,
                      'tenancy':tenancy}, callback))

    def __run_instances_cb__(self, kwargs, callback):
        try:
            ret = self.clc.run_instances(kwargs['image_id'], kwargs['min_count'], kwargs['max_count'],
                      kwargs['key_name'], kwargs['security_groups'],
                      kwargs['user_data'], kwargs['addressing_type'],
                      kwargs['instance_type'], kwargs['placement'],
                      kwargs['kernel_id'], kwargs['ramdisk_id'],
                      kwargs['monitoring_enabled'], kwargs['subnet_id'],
                      kwargs['block_device_map'],
                      kwargs['disable_api_termination'],
                      kwargs['instance_initiated_shutdown_behavior'],
                      kwargs['private_ip_address'],
                      kwargs['placement_group'], kwargs['client_token'],
                      kwargs['security_group_ids'],
                      kwargs['additional_info'], kwargs['instance_profile_name'],
                      kwargs['instance_profile_arn'], kwargs['tenancy'])
            Threads.instance().invokeCallback(callback,
                            Response(data=self.__normalize_instances__([ret])))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns instance list
    def terminate_instances(self, instance_ids, callback):
        self.caches['instances'].expireCache()
        Threads.instance().runThread(self.__terminate_instances_cb__,
                    ({'instance_ids':instance_ids}, callback))

    def __terminate_instances_cb__(self, kwargs, callback):
        try:
            ret = self.clc.terminate_instances(kwargs['instance_ids'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns instance list
    def stop_instances(self, instance_ids, force=False, callback=None):
        self.caches['instances'].expireCache()
        Threads.instance().runThread(self.__stop_instances_cb__,
                    ({'instance_ids':instance_ids, 'force':force}, callback))

    def __stop_instances_cb__(self, kwargs, callback):
        try:
            ret = self.clc.stop_instances(kwargs['instance_ids'], kwargs['force'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns instance list
    def start_instances(self, instance_ids, callback):
        self.caches['instances'].expireCache()
        Threads.instance().runThread(self.__start_instances_cb__,
                    ({'instance_ids':instance_ids}, callback))

    def __start_instances_cb__(self, kwargs, callback):
        try:
            ret = self.clc.start_instances(kwargs['instance_ids'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns instance status
    def reboot_instances(self, instance_ids, callback):
        self.caches['instances'].expireCache()
        Threads.instance().runThread(self.__reboot_instances_cb__,
                    ({'instance_ids':instance_ids}, callback))

    def __reboot_instances_cb__(self, kwargs, callback):
        try:
            ret = self.clc.reboot_instances(kwargs['instance_ids'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns console output
    def get_console_output(self, instance_id, callback):
        Threads.instance().runThread(self.__get_console_output_cb__, ({'instance_id':instance_id}, callback))

    def __get_console_output_cb__(self, kwargs, callback):
        try:
            ret = self.clc.get_console_output(kwargs['instance_id'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns password data
    def get_password_data(self, instance_id):
        return self.clc.get_password_data(instance_id)

    def get_all_addresses(self, filters, callback):
        callback(Response(data=self.caches['addresses'].values))

    # returns address info
    def allocate_address(self, callback):
        self.caches['addresses'].expireCache()
        Threads.instance().runThread(self.__allocate_address_cb__, ({}, callback))

    def __allocate_address_cb__(self, kwargs, callback):
        try:
            ret = self.clc.allocate_address()
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def release_address(self, publicip, callback):
        self.caches['addresses'].expireCache()
        Threads.instance().runThread(self.__release_address_cb__, ({'publicip':publicip}, callback))

    def __release_address_cb__(self, kwargs, callback):
        try:
            ret = self.clc.release_address(kwargs['publicip'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def associate_address(self, publicip, instanceid, callback):
        self.caches['addresses'].expireCache()
        Threads.instance().runThread(self.__associate_address_cb__,
                            ({'publicip':publicip, 'instanceid':instanceid}, callback))

    def __associate_address_cb__(self, kwargs, callback):
        try:
            ret = self.clc.associate_address(kwargs['publicip'], kwargs['instanceid'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def disassociate_address(self, publicip, callback):
        self.caches['addresses'].expireCache()
        Threads.instance().runThread(self.__disassociate_address_cb__,
                            ({'publicip':publicip}, callback))
    def __disassociate_address_cb__(self, kwargs, callback):
        try:
            ret = self.clc.disassociate_address(kwargs['publicip'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_key_pairs(self, filters, callback):
        callback(Response(data=self.caches['keypairs'].values))

    # returns keypair info and key
    def create_key_pair(self, key_name, callback):
        self.caches['keypairs'].expireCache()
        Threads.instance().runThread(self.__create_key_pair_cb__, ({'key_name':key_name}, callback))

    def __create_key_pair_cb__(self, kwargs, callback):
        try:
            ret = self.clc.create_key_pair(kwargs['key_name'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns nothing
    def delete_key_pair(self, key_name, callback):
        self.caches['keypairs'].expireCache()
        Threads.instance().runThread(self.__delete_key_pair_cb__, ({'key_name':key_name}, callback))

    def __delete_key_pair_cb__(self, kwargs, callback):
        try:
            ret = self.clc.delete_key_pair(kwargs['key_name'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns keypair info and key
    def import_key_pair(self, key_name, public_key_material, callback):
        self.caches['keypairs'].expireCache()
        Threads.instance().runThread(self.__import_key_pair_cb__,
                            ({'key_name':key_name, 'public_key_material':public_key_material}, callback))

    def __import_key_pair_cb__(self, kwargs, callback):
        try:
            ret = self.clc.import_key_pair(kwargs['key_name'], kwargs['public_key_material'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_security_groups(self, filters, callback):
        callback(Response(data=self.caches['groups'].values))

    # returns True if successful
    def create_security_group(self, name, description, callback):
        self.caches['groups'].expireCache()
        Threads.instance().runThread(self.__create_security_group_cb__,
                    ({'name':name, 'description':description}, callback))

    def __create_security_group_cb__(self, kwargs, callback):
        try:
            ret = self.clc.create_security_group(kwargs['name'], kwargs['description'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def delete_security_group(self, name=None, group_id=None, callback=None):
        # invoke this on a separate thread
        self.caches['groups'].expireCache()
        Threads.instance().runThread(self.__delete_security_group_cb__,
                    ({'name':name, 'group_id':group_id}, callback))

    def __delete_security_group_cb__(self, kwargs, callback):
        try:
            ret = self.clc.delete_security_group(kwargs['name'], kwargs['group_id'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def authorize_security_group(self, name=None,
                                 src_security_group_name=[],
                                 src_security_group_owner_id=[],
                                 ip_protocol=[], from_port=[], to_port=[],
                                 cidr_ip=[], group_id=[],
                                 src_security_group_group_id=[], callback=None):
        self.caches['groups'].expireCache()
        Threads.instance().runThread(self.__authorize_security_group_cb__,
                    ({'name':name, 'src_security_group_name':src_security_group_name,
                      'src_security_group_owner_id':src_security_group_owner_id,
                      'ip_protocol':ip_protocol, 'from_port':from_port,
                      'to_port':to_port, 'cidr_ip':cidr_ip, 'group_id':group_id,
                      'src_security_group_group_id':src_security_group_group_id}, callback))

    def __authorize_security_group_cb__(self, kwargs, callback):
        try:
            ret = []
            for i in range(len(kwargs['ip_protocol'])):
                ret.append(self.clc.authorize_security_group(kwargs['name'],
                    kwargs['src_security_group_name'][i] if kwargs['src_security_group_name'] else None,
                    kwargs['src_security_group_owner_id'][i] if kwargs['src_security_group_owner_id'] else None,
                    kwargs['ip_protocol'][i], kwargs['from_port'][i], kwargs['to_port'][i],
                    kwargs['cidr_ip'][i] if kwargs['cidr_ip'] else None, kwargs['group_id'][i] if kwargs['group_id'] else None,
                    kwargs['src_security_group_group_id'][i] if kwargs['src_security_group_group_id'] else None))
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def revoke_security_group(self, name=None,
                                 src_security_group_name=[],
                                 src_security_group_owner_id=[],
                                 ip_protocol=[], from_port=[], to_port=[],
                                 cidr_ip=[], group_id=[],
                                 src_security_group_group_id=[], callback=None):
        self.caches['groups'].expireCache()
        Threads.instance().runThread(self.__revoke_security_group_cb__,
                    ({'name':name, 'src_security_group_name':src_security_group_name,
                      'src_security_group_owner_id':src_security_group_owner_id,
                      'ip_protocol':ip_protocol, 'from_port':from_port,
                      'to_port':to_port, 'cidr_ip':cidr_ip, 'group_id':group_id,
                      'src_security_group_group_id':src_security_group_group_id}, callback))

    def __revoke_security_group_cb__(self, kwargs, callback):
        try:
            ret = []
            for i in range(len(kwargs['ip_protocol'])):
                ret.append(self.clc.revoke_security_group(kwargs['name'],
                    kwargs['src_security_group_name'][i] if kwargs['src_security_group_name'] else None,
                    kwargs['src_security_group_owner_id'][i] if kwargs['src_security_group_owner_id'] else None,
                    kwargs['ip_protocol'][i], kwargs['from_port'][i], kwargs['to_port'][i],
                    kwargs['cidr_ip'][i] if kwargs['cidr_ip'] else None, kwargs['group_id'][i] if kwargs['group_id'] else None,
                    kwargs['src_security_group_group_id'][i] if kwargs['src_security_group_group_id'] else None))
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_volumes(self, filters, callback):
        callback(Response(data=self.caches['volumes'].values))

    # returns volume info
    def create_volume(self, size, availability_zone, snapshot_id, callback):
        self.caches['volumes'].expireCache()
        Threads.instance().runThread(self.__create_volume_cb__,
                            ({'size':size, 'availability_zone':availability_zone, 'snapshot_id':snapshot_id}, callback))

    def __create_volume_cb__(self, kwargs, callback):
        try:
            ret = self.clc.create_volume(kwargs['size'], kwargs['availability_zone'], kwargs['snapshot_id'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def delete_volume(self, volume_id, callback):
        self.caches['volumes'].expireCache()
        Threads.instance().runThread(self.__delete_volume_cb__, ({'volume_id':volume_id}, callback))

    def __delete_volume_cb__(self, kwargs, callback):
        try:
            ret = self.clc.delete_volume(kwargs['volume_id'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def attach_volume(self, volume_id, instance_id, device, callback):
        self.caches['volumes'].expireCache()
        Threads.instance().runThread(self.__attach_volume_cb__,
                            ({'volume_id':volume_id, 'instance_id':instance_id, 'device':device}, callback))

    def __attach_volume_cb__(self, kwargs, callback):
        try:
            ret = self.clc.attach_volume(kwargs['volume_id'], kwargs['instance_id'], kwargs['device'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def detach_volume(self, volume_id, force=False, callback=None):
        self.caches['volumes'].expireCache()
        Threads.instance().runThread(self.__detach_volume_cb__,
                            ({'volume_id':volume_id, 'force':force}, callback))

    def __detach_volume_cb__(self, kwargs, callback):
        try:
            ret = self.clc.detach_volume(kwargs['volume_id'], kwargs['force'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_snapshots(self, filters, callback):
        callback(Response(data=self.caches['snapshots'].values))

    # returns snapshot info
    def create_snapshot(self, volume_id, description, callback):
        self.caches['snapshots'].expireCache()
        Threads.instance().runThread(self.__create_snapshot_cb__,
                            ({'volume_id':volume_id, 'description':description}, callback))

    def __create_snapshot_cb__(self, kwargs, callback):
        try:
            ret = self.clc.create_snapshot(kwargs['volume_id'], kwargs['description'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def delete_snapshot(self, snapshot_id, callback):
        self.caches['snapshots'].expireCache()
        Threads.instance().runThread(self.__delete_snapshot_cb__, ({'snapshot_id':snapshot_id}, callback))

    def __delete_snapshot_cb__(self, kwargs, callback):
        try:
            ret = self.clc.delete_snapshot(kwargs['snapshot_id'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns list of snapshots attributes
    def get_snapshot_attribute(self, snapshot_id, attribute, callback):
        self.caches['snapshots'].expireCache()
        Threads.instance().runThread(self.__get_snapshot_attribute_cb__,
                            ({'snapshot_id':snapshot_id, 'attribute':attribute}, callback))

    def __get_snapshot_attribute_cb__(self, kwargs, callback):
        try:
            ret = self.clc.get_snapshot_attribute(kwargs['snapshot_id'], kwargs['attribute'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def modify_snapshot_attribute(self, snapshot_id, attribute, operation, users, groups, callback):
        self.caches['snapshots'].expireCache()
        Threads.instance().runThread(self.__modify_snapshot_attribute_cb__,
                            ({'snapshot_id':snapshot_id, 'attribute':attribute,
                              'operation':operation, 'user':user, 'groups':groups}, callback))

    def __modify_snapshot_attribute_cb__(self, kwargs, callback):
        try:
            ret = self.clc.modify_snapshot_attribute(kwargs['snapshot_id'], kwargs['attribute'],
                                    kwargs['operation'], kwargs['user'], kwargs['groups'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def reset_snapshot_attribute(self, snapshot_id, attribute, callback):
        self.caches['snapshots'].expireCache()
        Threads.instance().runThread(self.__reset_snapshot_attribute_cb__,
                            ({'snapshot_id':snapshot_id, 'attribute':attribute}, callback))

    def __reset_snapshot_attribute_cb__(self, kwargs, callback):
        try:
            ret = self.clc.reset_snapshot_attribute(kwargs['snapshot_id'], kwargs['attribute'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def deregister_image(self, image_id, callback):
        self.caches['images'].expireCache()
        Threads.instance().runThread(self.__deregister_image_cb__,
                    ({'image_id':image_id}, callback))

    def __deregister_image_cb__(self, kwargs, callback):
        try:
            ret = self.clc.deregister_image(kwargs['image_id'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def register_image(self, name, image_location=None, description=None, architecture=None, kernel_id=None, ramdisk_id=None, root_dev_name=None, block_device_map=None, callback=None):
        self.caches['images'].expireCache()
        Threads.instance().runThread(self.__register_image_cb__,
                            ({'name':name, 'image_location':image_location, 'description':description,
                              'architecture':architecture, 'kernel_id':kernel_id,
                              'ramdisk_id':ramdisk_id, 'root_dev_name':root_dev_name,
                              'block_device_map':block_device_map}, callback))
    def __register_image_cb__(self, kwargs, callback):
        try:
            ret = self.clc.register_image(kwargs['name'], kwargs['image_location'], kwargs['description'],
                                          kwargs['architecture'], kwargs['kernel_id'], kwargs['ramdisk_id'],
                                          kwargs['root_dev_name'], kwargs['block_device_map'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_tags(self, filters, callback):
        callback(Response(data=self.caches['tags'].values))

    # returns tag info
    def create_tags(self, resourceIds, tags, callback):
        self.caches['tags'].expireCache()
        Threads.instance().runThread(self.__create_tag_cb__,
                            ({'resource_ids':resourceIds, 'tags':tags}, callback))

    def __create_tag_cb__(self, kwargs, callback):
        try:
            ret = self.clc.create_tags(kwargs['resource_ids'], kwargs['tags'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    # returns True if successful
    def delete_tags(self, resourceIds, tags, callback):
        self.caches['tags'].expireCache()
        Threads.instance().runThread(self.__delete_tag_cb__,
                            ({'resource_ids':resourceIds, 'tags':tags}, callback))

    def __delete_tag_cb__(self, kwargs, callback):
        try:
            ret = self.clc.delete_tags(kwargs['resource_ids'], kwargs['tags'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

class Response(object):
    data = None
    error = None

    def __init__(self, data=None, error=None):
        self.data = data
        self.error = error
