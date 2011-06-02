import boto,sys,euca_admin
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin import EucaAdmin
from optparse import OptionParser

SERVICE_PATH = '/services/Component'
class ConvertVolumes():
  
  
  def __init__(self, host_name=None, storage_provider=None):
      self.host_name = host_name
      self.storage_provider = storage_provider
      self.euca = EucaAdmin(path=SERVICE_PATH)
          
  def __repr__(self):
      return 'ConvertVolumes %s %s' % (self.host_name, self.storage_provider) 

  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    setattr(self, name, value)
  
  def get_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-H","--host",dest="sc_host",help="Hostname or IP of the storage controller.")
    parser.add_option("-B","--backend",dest="provider",help="Storage backend provider to convert from.")
    return parser

  def execute(self, sc_host, provider):
    try:
      reply = self.euca.connection.get_object('ConvertVolumes', {'Component':'storage', 
                                                                 'OriginalProvider':provider,
                                                                 'Host':sc_host}, BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

class CloneVolume():
  

  def __init__(self, host_name=None, volume_id=None):
      self.host_name = host_name
      self.volume_id = volume_id
      self.euca = EucaAdmin(path=SERVICE_PATH)
          
  def __repr__(self):
      return 'Clone Volume %s %s' % (self.host_name, self.volume_id) 

  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    setattr(self, name, value)
  
  def get_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-H","--host",dest="sc_host",help="Hostname or IP of the storage controller.")
    parser.add_option("-v","--volumeId",dest="volume_id",help="Parent Volume Id.")
    return parser

  def execute(self, sc_host, volume_id):
    try:
      reply = self.euca.connection.get_object('CloneVolume', {'Component':'storage', 
                                                                 'VolumeId':volume_id,
                                                                 'Host':sc_host}, BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

