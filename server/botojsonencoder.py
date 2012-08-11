import boto
import copy
import json

from json import JSONEncoder
from json import JSONDecoder
from boto.ec2 import EC2Connection
from boto.ec2.ec2object import EC2Object
from boto.regioninfo import RegionInfo
from boto.ec2.image import ImageAttribute
from boto.ec2.instance import Group
from boto.ec2.securitygroup import GroupOrCIDR
from boto.ec2.securitygroup import IPPermissions
from boto.ec2.volume import AttachmentSet
from .response import Response


class BotoJsonEncoder(JSONEncoder):
    def default(self, obj):
        if issubclass(obj.__class__, EC2Object):
            values = copy.copy(obj.__dict__)
            values['__obj_name__'] = obj.__class__.__name__
            return (values)
        elif isinstance(obj, RegionInfo):
            values = copy.copy(obj.__dict__)
            values['__obj_name__'] = 'RegionInfo'
            return (values)
        elif isinstance(obj, Response):
            return obj.__dict__
        elif isinstance(obj, EC2Connection):
            return []
        elif issubclass(obj.__class__, Group):
            values = copy.copy(obj.__dict__)
            values['__obj_name__'] = obj.__class__.__name__
            return (values)
        elif issubclass(obj.__class__, ImageAttribute):
            values = copy.copy(obj.__dict__)
            values['__obj_name__'] = 'ImageAttribute'
            return (values)
        elif issubclass(obj.__class__, AttachmentSet):
            values = copy.copy(obj.__dict__)
            values['__obj_name__'] = 'AttachmentSet'
            return (values)
        elif issubclass(obj.__class__, IPPermissions):
            values = copy.copy(obj.__dict__)
            # this is because I found a "parent" property set to self - dak
            values['parent'] = None
            values['__obj_name__'] = 'IPPermissions'
            return (values)
        elif issubclass(obj.__class__, GroupOrCIDR):
            values = copy.copy(obj.__dict__)
            values['__obj_name__'] = 'GroupOrCIDR'
            return (values)
        return super(BotoJsonEncoder, self).default(obj)

class BotoJsonDecoder(JSONDecoder):
    # if we need to map classes to specific boto objects, we'd do that here
    # it seems like we can get away with generic objects for now.
    # regioninfo isn't used, so we just show this for an example
    def default(self, obj):
        if obj['__obj_name__'] == 'RegionInfo':
            ret = RegionInfo()
