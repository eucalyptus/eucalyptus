# Copyright (c) 2016 Hewlett Packard Enterprise Development LP
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

import argparse
import collections
import datetime
import email.utils
import json
import string

from requestbuilder import Arg
from requestbuilder.exceptions import ArgumentError
import six
import yaml

from eucalyptus_admin.commands.bootstrap import BootstrapRequest
from eucalyptus_admin.commands.bootstrap.describeservices import \
    DescribeServices


FORMATS = {
    'oneline': '{timestamp} {severity} {subject-type} {subject-name} {message}'
}


class Event(object):
    __KEYS = ('timestamp', 'severity', 'id', 'subject_type', 'subject_name',
              'subject_host', 'subject_arn', 'message', 'stack_trace')

    def __init__(self):
        for field in self.__KEYS:
            setattr(self, field, None)

    @classmethod
    def from_status_detail(cls, detail):
        event = cls()
        event.timestamp = datetime.datetime.fromtimestamp(
            email.utils.mktime_tz(
                email.utils.parsedate_tz(
                    detail.get('timestamp'))))
        event.id = detail.get('uuid')
        event.severity = detail.get('severity')
        event.subject_name = detail.get('serviceName')
        event.subject_host = detail.get('serviceHost')
        event.subject_arn = detail.get('serviceFullName')
        if detail.get('message') and detail['message'] != 'UNSET':
            event.message = BlockFormatString(detail['message'])
        if detail.get('stackTrace'):
            event.stack_trace = BlockFormatString(detail['stackTrace'])
        return event

    @classmethod
    def possible_keys(cls):
        return [key.replace('_', '-') for key in cls.__KEYS]

    def keys(self):
        return [key for key in self.possible_keys()
                if getattr(self, key, None) is not None]

    def items(self):
        for field in self.__KEYS:
            if getattr(self, field) is None:
                continue
            yield (field.replace('_', '-'), getattr(self, field))

    ## TODO:  rename this
    def as_oneline_str(self, fmt):
        sdata = collections.defaultdict(lambda: '')
        for key, val in self.items():
            if val is None:
                val = ''
            elif isinstance(val, six.string_types):
                val = val.replace('\n', '\\\\n')
            sdata[key] = val
        return string.Formatter().vformat(fmt, (), sdata).decode(
            'string_escape')

    def as_ordered_dict(self):
        return collections.OrderedDict(self.items())

    @classmethod
    def represent_as_ordered_dict(cls, dumper, event):
        assert isinstance(event, cls)
        return dumper.represent_mapping(
            u'tag:yaml.org,2002:map',
            event.as_ordered_dict().items())


class BlockFormatString(str):
    @classmethod
    def represent_as_block(cls, dumper, data):
        assert isinstance(data, cls)
        # Non-printable characters cause pyyaml to quote the whole
        # block. The most common of these is tabs in java backtraces,
        # so we expand tabs ahead of time.
        return dumper.represent_scalar(u'tag:yaml.org,2002:str',
                                       data.strip().replace('\t', '    '),
                                       style='|')


class EventJSONEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, Event):
            return obj.as_ordered_dict()
        if isinstance(obj, datetime.datetime):
            return obj.isoformat(' ')
        return json.JSONEncoder.default(self, obj)


def generate_key_list():
    keys = sorted(Event.possible_keys())
    return '{0}, and {1}'.format(', '.join(keys[:-1]), keys[-1])


class DescribeEvents(BootstrapRequest):
    DESCRIPTION = ('*** TECH PREVIEW ***\n\n'
                   'Show information about service-affecting events\n\n'
                   'Possible tags for events include {0}.'
                   .format(generate_key_list()))
    ARGS = [Arg('-f', '--format', default='yaml', metavar='FORMAT',
                help='''show events in a given format, where FORMAT can be
                one of "yaml", "oneline", and "format:STRING".  See %(prog)s(1)
                for additional details about each format.  (default: yaml)'''),
            Arg('-s', '--show-stack-traces', action='store_true',
                help='also retrieve stack traces')]

    def configure(self):
        BootstrapRequest.configure(self)
        fmt = self.args.get('format')
        if (fmt in ('yaml', None) or fmt.startswith('format:') or
                fmt in FORMATS):
            return fmt
        raise ArgumentError(
            'invalid format string; choose from {0} or prepend with "format:" to '
            'build your own'.format(','.join(['yaml'] + FORMATS.keys())))

    def main(self):
        req = DescribeServices.from_other(
            self, ListAll=True, ShowEvents=True,
            ShowEventStacks=self.args.get('show_stack_traces'))
        result = req.main()
        events = []
        for service in result.get('serviceStatuses') or []:
            for r_event in service.get('statusDetails') or []:
                event = Event.from_status_detail(r_event)
                event.subject_type = service['serviceId'].get('type')
                events.append(event)
        return events

    def print_result(self, events):
        # We can use collections.OrderedDict because RHEL 6 has a backport.
        evdata = {'events': sorted(events, key=lambda x: x.timestamp)}
        if self.args.get('format') in ('yaml', None):
            yaml.add_representer(Event, Event.represent_as_ordered_dict)
            yaml.add_representer(BlockFormatString,
                                 BlockFormatString.represent_as_block)
            print yaml.dump(evdata, indent=2, default_flow_style=False)
        #elif self.args.get('format') == 'json':
        #    print json.dumps(evdata, indent=2, cls=EventJSONEncoder)
        else:
            if self.args.get('format').startswith('format:'):
                fmt = self.args['format'].split('format:', 1)[1]
            else:
                fmt = FORMATS[self.args['format']]
            for event in evdata['events']:
                print event.as_oneline_str(fmt=fmt)
