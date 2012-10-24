#!/usr/bin/python
# -*- coding: utf-8 -*-

"""
@license: OWASP Enterprise Security API (ESAPI)
     
    This file is part of the Open Web Application Security Project (OWASP)
    Enterprise Security API (ESAPI) project. For details, please see
    U{http://www.owasp.org/index.php/ESAPI<http://www.owasp.org/index.php/ESAPI>}.

    The ESAPI is published by OWASP under the BSD license. You should read and 
    accept the LICENSE before you use, modify, and/or redistribute this software.
    
@summary: A PushbackString is used by Codecs to allow them to push decoded 
    characters back onto a string for further decoding.
@copyright: Copyright (c) 2009 - The OWASP Foundation
@author: Craig Younkins (craig.younkins@owasp.org)
"""

def is_hex_digit(char):
    """
    Returns true if the parameter character is a hexadecimal digit 0
    through 9, a through f, or A through F.
    """
    if char is None: 
        return False
    return (( '0' <= char <= '9') or
            ( 'a' <= char <= 'f') or
            ( 'A' <= char <= 'F'))
           
def is_octal_digit(char):
    """
    Returns true if the parameter character is an octal digit between
    0 through 7.
    """
    if char is None: 
        return False
    return '0' <= char <= '7'


class PushbackString:
    """
    A PushbackString is used by Codecs to allow them to push decoded 
    characters back onto a string for further decoding. This is necessary to 
    detect double-encoding.
    """

    _input = None
    _pushback = None
    _temp = None
    _index = 0
    _mark = 0

    def __init__(self, input_):
        self._input = input_
    
    def pushback(self, char):
        """
        Set a character for pushback.
        """
        self._pushback = char
        
    def index(self):
        """
        Get the current index of the PushbackString. Typically used in error
        messages.
        """
        return self._index
        
    def has_next(self):
        """
        Returns True if there is another character accesible by next(), False
        if not.
        """
        if self._pushback is not None: 
            return True
        if self._input is None: 
            return False
        if len(self._input) == 0: 
            return False
        if self._index >= len(self._input): 
            return False
        return True
    
    def next(self):
        """
        Retrieves the next character in the string, according to the current 
        index.
        """
        if self._pushback is not None:
            save = self._pushback
            self._pushback = None
            return save
        
        if self._input is None: 
            return None
        if len(self._input) == 0: 
            return None
        if self._index >= len(self._input): 
            return None
        ret = self._input[self._index]
        self._index += 1
        return ret
        
    def next_hex(self):
        """
        Returns the next character if and only if it is a hex character.
        """
        next_char = self.next()
        if next_char is None: 
            return None
        if is_hex_digit(next_char): 
            return next_char
        return None
        
    def next_octal(self):
        """
        Returns the next character if and only if it is an octal character.
        """
        next_char = self.next()
        if next_char is None: 
            return None
        if is_octal_digit(next_char): 
            return next_char
        return None
        
    def peek(self, test_char=None):
        """
        Returns the next character without advancing the index.
        """
        if test_char:
            if self._pushback is not None and self._pushback == test_char: 
                return True
            if self._input is None: 
                return False
            if len(self._input) == 0: 
                return False
            if self._index >= len(self._input): 
                return False
            return self._input[self._index] == test_char
            
        if not test_char:
            if self._pushback is not None: 
                return self._pushback
            if self._input is None: 
                return None
            if len(self._input) == 0: 
                return None
            if self._index >= len(self._input): 
                return None
            
            return self._input[self._index]
            
    def mark(self):
        """
        Mark the current index, so the client can reset() to it if need be.
        """
        self._temp = self._pushback
        self._mark = self._index
        
    def reset(self):
        """
        Return to the last mark.
        """
        self._pushback = self._temp
        self._index = self._mark
        
    def remainder(self):
        """
        Get the remaining part of the string from the current index onward,
        including a pushback if it exists.
        """
        output = self._input[self._index:]
        if self._pushback:
            output = self._pushback + output
        return output