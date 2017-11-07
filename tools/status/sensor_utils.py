#!/usr/bin/python

# Copyright 2014 Ent. Services Development Corporation LP
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the
# following conditions are met:
#
#   Redistributions of source code must retain the above copyright
#   notice, this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer
#   in the documentation and/or other materials provided with the
#   distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

import sys
import json
import time
from argparse import ArgumentParser


default_euca_home=''
default_monitoring_home='/var/run/eucalyptus/status'
unknown_result = 'UNKNOWN'

def parse_file(sensor_file=None):
    try :
        f = open(sensor_file)
        parsed = json.load(f)
        f.close()
        return parsed
    except:
        return None

def build_args():
    parser = ArgumentParser()
    parser.add_argument('--sensor_name', help='Name of the sensor to read')
    parser.add_argument('--sensor_value', help='Name of the sensor value to read')
    parser.add_argument('--euca_home', help='Eucalyptus installation directory root. Default=/')
    return parser

def get_sensor_filename(sensor_name=None, monitoring_root=default_monitoring_home):
    return monitoring_root + '/' + sensor_name.replace('.', '/')

def get_value(sensor_name=None, sensor_value=None, euca_home=default_euca_home):
    if euca_home is None:
        euca_home=default_euca_home
  
    data_map = parse_file(get_sensor_filename(sensor_name, monitoring_root=euca_home + default_monitoring_home))
    result = unknown_result
    #Ensure there is a value and the data isn't expired based on the ttl
    if data_map is not None and (time.time() < data_map['timestamp'] + data_map['ttl']):
        result = data_map["values"][sensor_value]

    return result

if __name__ == '__main__':
    arg_parser = build_args()
    args = arg_parser.parse_args(sys.argv[1:])
    result_value = get_value(sensor_name=args.sensor_name, sensor_value=args.sensor_value, euca_home=args.euca_home)
    print result_value + '\n'
    if result_value == unknown_result:
        #Could not get a good reading, return error
        exit(1)
    else:
        #OK
        exit(0)
