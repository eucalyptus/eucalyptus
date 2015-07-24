# Copyright 2015 Eucalyptus Systems, Inc.
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

import json
import os
import subprocess
import sys
import tempfile
import textwrap

from requestbuilder import Arg, EMPTY, MutuallyExclusiveArgList
from requestbuilder.exceptions import ArgumentError
import yaml

from eucalyptus_admin.commands.properties import PropertiesRequest
from eucalyptus_admin.commands.properties.describeproperties import \
    DescribeProperties
from eucalyptus_admin.commands.properties.modifypropertyvalue import \
    ModifyPropertyValue


PROPERTY_TYPES = {'authentication.ldap_integration_configuration': 'json',
                  'cloud.network.network_configuration': 'json',
                  'cloudformation.swf_client_config': 'json',
                  'cloudformation.swf_workflow_worker_config': 'json',
                  'region.region_configuration': 'json'}


def _property_key_value(kvp_str):
    if '=' in kvp_str:
        key, val = kvp_str.split('=', 1)
        if val.startswith('@'):
            with open(val[1:]) as val_file:
                val = val_file.read()
        return (key.strip(), val.strip())
    else:
        return (kvp_str.strip(), None)


class Euctl(PropertiesRequest):
    DESCRIPTION = textwrap.dedent(
        '''\
        The %(prog)s utility retrieves cloud state and allows cloud
        administrators to set cloud state.  The state to be retrieved
        or set is described using a ``Management Information Base''
        (MIB) style name, described as a dotted set of components.

        When retrieving a variable, a subset of the MIB name may be
        specified to retrieve a list of variables in that subset.  For
        example, to list all the dns variables:

            $ %(prog)s dns

        When setting a variable, the MIB name should be followed by an
        equal sign (=) and the new value:

            $ %(prog)s dns.enabled=true

        To reset a varable to its default value, specify the -r option:

            $ %(prog)s -r dns.enabled

        The information available from %(prog)s consists of integers,
        strings, and structures.  The structured information can only
        be retrieved by specialized programs and, in some cases, this
        program's --edit and --dump options.
        ''')

    ARGS = [Arg('prop_pairs', metavar='NAME[=VALUE|=@FILE]', nargs='*',
                type=_property_key_value,
                help='''Output the specified variable, and where a value
                is given, attempt to set it to the specified value.'''),
            Arg('-A', '--all-types', action='store_true', help='''List all
                the known variable names, including structures.  Those with
                string or integer values will be output as usual; for the
                structured values, the methods of retrieving them are
                given.'''),
            Arg('-r', '--reset', action='store_true',
                help='Reset the given variables to their default values.'),
            Arg('-d', dest='show_default', action='store_true', help='''Show
                variables' default values instead of their current values.'''),
            Arg('-n', dest='suppress_name', action='store_true',
                help='''Suppress output of the variable name.  This is
                useful for setting shell variables.'''),
            Arg('-q', dest='suppress_all', action='store_true',
                help='''Suppress all output when setting a variable.  This
                option overrides the behavior of -n.'''),
            MutuallyExclusiveArgList(
                Arg('--edit', action='store_true', help='''Edit a structured
                    variable interactively.  Only one variable may be edited
                    per invocation.  When looking for an editor, the program
                    will first try the environment variable VISUAL, then the
                    environment variable EDITOR, and finally the default
                    editor, vi(1).'''),
                Arg('--dump', action='store_true', help='''Output the value of
                    a structured variable in its entirety.''')),
            Arg('--format', choices=('json', 'yaml'), default='json',
                help='''Try to use the specified format when displaying
                a structured variable.  (default: json)''')]

    def configure(self):
        PropertiesRequest.configure(self)
        argcheck = None
        if self.args.get('dump'):
            argcheck = '--dump'
        elif self.args.get('edit'):
            argcheck = '--edit'
        if argcheck:
            if len(self.args.get('prop_pairs') or []) != 1:
                raise ArgumentError(
                    'argument {0} must be accompanied by exactly one '
                    'property name'.format(argcheck))
            if self.args.get('prop_pairs')[0][1]:
                raise ArgumentError(
                    'argument {0}: a value may not accompany the '
                    'property name'.format(argcheck))

    def main(self):
        # FIXME:  This doesn't handle empty property values.
        if self.args.get('dump'):
            prop_name = self.args.get('prop_pairs')[0][0]
            self.log.info('dumping property value   %s', prop_name)
            prop = self._get_unique_property(prop_name)
            formatter = self._get_formatter()
            print formatter.dumps(prop.value).strip()
        elif self.args.get('edit'):
            prop_name = self.args.get('prop_pairs')[0][0]
            self.log.info('editing property value   %s', prop_name)
            prop = self._get_unique_property(prop_name)
            has_changed = self.edit_interactively(prop)
            if has_changed:
                self.log.info('setting property value   %s', prop_name)
                self._set_property(prop_name, value=prop.dumps())
            else:
                print >> sys.stderr, prop_name, 'is unchanged'
        else:
            for key, val in self.args.get('prop_pairs') or [(None, None)]:
                if key == 'euca':
                    # DescribeProperties doesn't work on the magical,
                    # groovy code-injecting "euca" property, so we have
                    # to handle input and output in one shot.
                    #
                    # We don't do this all the time is case the value
                    # we chose doesn't actually get set for some reason.
                    self.log.info('handling one-shot property "euca"')
                    req = ModifyPropertyValue.from_other(self, Name=key,
                                                         Value=val)
                    response = req.main()
                    print response.get('value')
                    continue
                if val is not None:
                    self.log.info('setting property value   %s: %s', key, val)
                    self._set_property(key, value=val)
                if val is None and self.args.get('reset'):
                    self.log.info('resetting property value %s', key)
                    self._set_property(key, reset=True)
                self.log.info('showing property value   %s', key)
                req = DescribeProperties.from_other(self, Property=[key])
                response = req.main()
                for prop_dict in response.get('properties') or []:
                    if self.args.get('show_default'):
                        value = prop_dict.get('defaultValue')
                    else:
                        value = prop_dict.get('value')
                    prop = _build_property(prop_dict.get('name'), value)
                    if not self.args.get('suppress_all'):
                        prop.print_(
                            suppress_name=self.args.get('suppress_name'),
                            force=self.args.get('all_types'))

    def _set_property(self, prop_name, value=None, reset=None):
        if value == '':
            value = EMPTY
        request = ModifyPropertyValue.from_other(self, Name=prop_name,
                                                 Value=value, Reset=reset)
        response = request.main()
        return response.get('oldValue')

    def _get_all_properties(self, prop_name):
        request = DescribeProperties.from_other(self, Property=[prop_name])
        properties = []
        for prop_dict in request.main().get('properties') or []:
            properties.append(_build_property(prop_dict['name'],
                                              prop_dict.get('value')))
        return properties

    def _get_unique_property(self, prop_name):
        properties = self._get_all_properties(prop_name)
        if len(properties) < 1:
            raise RuntimeError('no such property: {0}'.format(prop_name))
        if len(properties) > 1:
            # This is probably where we should implement subtree editing
            # when the service supports it in the future.
            raise RuntimeError('{0} matches more than one property'
                               .format(prop_name))
        return properties[0]

    def edit_interactively(self, prop):
        formatter = self._get_formatter()
        temp_fd, temp_name = tempfile.mkstemp()
        temp_fd = os.fdopen(temp_fd, 'w')
        try:
            formatter.dump(prop.value, temp_fd)
            editor = os.getenv('VISUAL') or os.getenv('EDITOR') or 'vi'
            while True:
                temp_fd.close()
                subprocess.check_call((editor, temp_name))
                temp_fd = open(temp_name)
                try:
                    new_value = formatter.load(temp_fd)
                except yaml.error.MarkedYAMLError as err:
                    self.log.debug('YAML parse failed', exc_info=True)
                    msg = ('Parsing of new value failed on line {0}: {1}\n{2}'
                           .format(err.problem_mark.line, err.problem,
                                   err.problem_mark.get_snippet()))
                    print >> sys.stderr, msg
                    yesno = raw_input('Edit again? [y/N] ')
                    if not yesno.lower().startswith('y'):
                        raise
                except ValueError as err:
                    # Probably json.loads
                    self.log.debug('New value parse failed', exc_info=True)
                    msg = 'Parsing of new value failed: {0}'.format(
                        err.args[0])
                    print >> sys.stderr, msg
                    yesno = raw_input('Edit again? [y/N] ')
                    if not yesno.lower().startswith('y'):
                        raise
                else:
                    break
        finally:
            temp_fd.close()
            os.remove(temp_name)
        if new_value == prop.value:
            return False  # not changed
        prop.value = new_value
        return True  # changed

    def _get_formatter(self):
        if self.args.get('format') == 'json':
            return _JSONFormatter()
        if self.args.get('format') == 'yaml':
            return _YAMLFormatter()
        raise NotImplementedError('formatter "{0}" not implemented'
                                  .format(self.args.get('format')))


def _build_property(prop_name, prop_value):
    if PROPERTY_TYPES.get(prop_name) == 'json':
        prop = _JSONProperty(prop_name)
    else:
        prop = _Property(prop_name)
    prop.loads(prop_value)
    return prop


class _Formatter(object):
    @staticmethod
    def dumps(value):
        return value

    @staticmethod
    def loads(str_value):
        return str_value

    @classmethod
    def dump(cls, value, fileobj):
        fileobj.write(cls.dumps(value))

    @classmethod
    def load(cls, fileobj):
        return cls.loads(fileobj.read())


class _JSONFormatter(_Formatter):
    @staticmethod
    def dumps(value):
        if not value:
            return ''
        return json.dumps(value, indent=2)

    @staticmethod
    def loads(str_value):
        if not str_value:
            return ''
        if str_value == "{ 'sync': { 'enable':'false' } }":
            # Invalid JSON:  https://eucalyptus.atlassian.net/browse/EUCA-10667
            str_value = str_value.replace("'", '"')
        return json.loads(str_value)


class _YAMLFormatter(_Formatter):
    @staticmethod
    def dumps(value):
        return yaml.safe_dump(value, default_flow_style=False)

    @staticmethod
    def loads(str_value):
        return yaml.safe_load(str_value)


class _Property(object):
    FORMATTER = _Formatter

    def __init__(self, name):
        self.name = name
        self.value = None
        self._formatter = self.FORMATTER()

    def dumps(self):
        if not self.value:
            return ''
        return self._formatter.dumps(self.value)

    def dump(self, fileobj):
        fileobj.write(self.dumps())

    def loads(self, value):
        if value:
            self.value = self._formatter.loads(value)
        else:
            self.value = ''

    def load(self, fileobj):
        self.loads(fileobj.read())

    def print_(self, suppress_name=False, force=False):
        if self.value == {}:
            value = ''
        else:
            value = self.value
        if suppress_name:
            print value.strip()
        else:
            print '{0} = {1}'.format(self.name.strip(), value.strip())


class _JSONProperty(_Property):
    FORMATTER = _JSONFormatter

    def print_(self, suppress_name=False, force=False):
        if force:
            print >> sys.stderr, 'use euctl --dump to view {0}'.format(
                self.name)
