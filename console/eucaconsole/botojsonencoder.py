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

import boto
import copy
import json
import logging

from json import JSONEncoder
from json import JSONDecoder
from boto.ec2 import EC2Connection
from boto.ec2.ec2object import EC2Object
from boto.regioninfo import RegionInfo
from boto.ec2.blockdevicemapping import BlockDeviceType
from boto.ec2.image import ImageAttribute
from boto.ec2.instance import ConsoleOutput
from boto.ec2.instance import Group
# these things came in with boto 2.6
try:
    from boto.ec2.instance import InstanceState
    from boto.ec2.instance import InstancePlacement
except ImportError:
    pass
from boto.ec2.securitygroup import GroupOrCIDR
from boto.ec2.securitygroup import IPPermissions
from boto.ec2.volume import AttachmentSet
from .response import ClcError
from .response import Response
from esapi.codecs.html_entity import HTMLEntityCodec

class BotoJsonEncoder(JSONEncoder):
    # use this codec directly vs using factory which messes with logging config
    codec = HTMLEntityCodec()
    IMMUNE_HTML = ',.-_ '
    IMMUNE_HTMLATTR = ',.-_'

    # these are system generated values that aren't a risk for XSS attacks
    FIELD_WHITELIST = [
        'id',
        'image_id',
        'kernel_id',
        'ramdisk_id',
        'reservation_id',
        'owner_id',
        'root_device_type',
        'state',
        'state_reason',
        'state_code',
        'monitored',
        'platform',
        'volume_id',
        'snapshot_id',
        'launch_time',
        'attach_time',
        'create_time',
        'start_time',
        'instance_type',
        'zone',
        'progress',
        'ip_protocol',
        'fingerprint',
    ];
    
    def __sanitize_and_copy__(self, dict):
        try:
            ret = copy.copy(dict)
            for key in ret.keys():
                if key in self.FIELD_WHITELIST:
                    continue
                if isinstance(ret[key], basestring):
                    ret[key] = self.codec.encode(self.IMMUNE_HTML, ret[key])
            return ret
        except Exception, e:
            logging.error(e)

    def default(self, obj):
        if issubclass(obj.__class__, EC2Object):
            values = self.__sanitize_and_copy__(obj.__dict__)
            values['__obj_name__'] = obj.__class__.__name__
            return (values)
        elif isinstance(obj, RegionInfo):
            return []
        elif isinstance(obj, ClcError):
            return self.__sanitize_and_copy__(obj.__dict__)
        elif isinstance(obj, Response):
            return obj.__dict__
        elif isinstance(obj, EC2Connection):
            return []
        elif isinstance(obj, Group):
            values = self.__sanitize_and_copy__(obj.__dict__)
            values['__obj_name__'] = 'Group'
            return (values)
        elif isinstance(obj, ConsoleOutput):
            values = self.__sanitize_and_copy__(obj.__dict__)
            values['__obj_name__'] = 'ConsoleOutput'
            return (values)
        elif isinstance(obj, ImageAttribute):
            values = self.__sanitize_and_copy__(obj.__dict__)
            values['__obj_name__'] = 'ImageAttribute'
            return (values)
        elif isinstance(obj, AttachmentSet):
            values = self.__sanitize_and_copy__(obj.__dict__)
            values['__obj_name__'] = 'AttachmentSet'
            return (values)
        elif isinstance(obj, IPPermissions):
            values = self.__sanitize_and_copy__(obj.__dict__)
            # this is because I found a "parent" property set to self - dak
            values['parent'] = None
            values['__obj_name__'] = 'IPPermissions'
            return (values)
        elif isinstance(obj, GroupOrCIDR):
            values = self.__sanitize_and_copy__(obj.__dict__)
            values['__obj_name__'] = 'GroupOrCIDR'
            return (values)
        elif isinstance(obj, BlockDeviceType):
            values = self.__sanitize_and_copy__(obj.__dict__)
            values['connection'] = None
            values['__obj_name__'] = 'BlockDeviceType'
            return (values)
        if isinstance(obj, InstanceState):
            values = self.__sanitize_and_copy__(obj.__dict__)
            values['__obj_name__'] = 'InstanceState'
            return (values)
        elif isinstance(obj, InstancePlacement):
            values = self.__sanitize_and_copy__(obj.__dict__)
            values['__obj_name__'] = 'InstancePlacement'
            return (values)
        return super(BotoJsonEncoder, self).default(obj)

class BotoJsonDecoder(JSONDecoder):
    # if we need to map classes to specific boto objects, we'd do that here
    # it seems like we can get away with generic objects for now.
    # regioninfo isn't used, so we just show this for an example
    def default(self, obj):
        if obj['__obj_name__'] == 'RegionInfo':
            ret = RegionInfo()
