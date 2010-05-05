import boto,sys

class BooleanResponse:
  def __init__(self, reply=False):
    self.reply = reply
    self.error = None
      
  def __repr__(self):
    if self.error:
      print 'RESPONSE %s' % self.error
      sys.exit(1)
    else:
      return 'RESPONSE %s' % self.reply 

  def startElement(self, name, attrs, connection):
    return None

  def endElement(self, name, value, connection):
    if name == 'euca:_return':
      self.reply  = value
    elif name == 'Message':
      self.error = value
    else:
      setattr(self, name, value)


class StringList(list):

  def __repr__(self):
    r = ""
    for i in self:
      r = "%s %s" % (r,i)
    return r
    
  def startElement(self, name, attrs, connection):
    pass

  def endElement(self, name, value, connection):
    if name == 'euca:entry':
      self.append(value)
      