#!/usr/bin/python
# -*- coding: utf-8 -*-

"""
@license: OWASP Enterprise Security API (ESAPI)
     
    This file is part of the Open Web Application Security Project (OWASP)
    Enterprise Security API (ESAPI) project. For details, please see
    U{http://www.owasp.org/index.php/ESAPI<http://www.owasp.org/index.php/ESAPI>}.

    The ESAPI is published by OWASP under the BSD license. You should read and 
    accept the LICENSE before you use, modify, and/or redistribute this software.
    
@summary: The Codec interface defines a set of methods for encoding and 
    decoding application level encoding schemes.
@copyright: Copyright (c) 2009 - The OWASP Foundation
@author: Craig Younkins (craig.younkins@owasp.org)
"""

from esapi.codecs.push_back_string import PushbackString
      
def is_8bit(char):
    """
    Returns True if ord(char) < 256, False otherwise
    """
    return ord(char) < 256
        
def is_control_char(char):
    if (ord(char) <= 0x1F or        # C0 control codes
       0x7F <= ord(char) <= 0x9F):  # Del and C1 control codes
        return True
    
    return False
        
def get_hex_for_char(char):
    """
    Returns the hex equivalent of the given character in the form 3C
    """
    return hex(ord(char))[2:]

class Codec():
    """
    The Codec interface defines a set of methods for encoding and decoding 
    application level encoding schemes, such as HTML entity encoding and 
    percent encoding (aka URL encoding). Codecs are used in output encoding
    and canonicalization.  The design of these codecs allows for 
    character-by-character decoding, which is necessary to detect 
    double-encoding and the use of multiple encoding schemes, both of which are
    techniques used by attackers to bypass validation and bury encoded attacks
    in data.

    @author: Craig Younkins (craig.younkins@owasp.org)
    @see: L{esapi.encoder}
    """
    
    def __init__(self):
        pass
           
    def encode(self, immune, raw):
        """
        Encode a String so that it can be safely used in a specific context.

        @param immune: @param raw
                the String to encode
        @return: the encoded String
        """    
        ret = ''
        try:
            for char in raw:
                ret += self.encode_character(immune, char)
            return ret
        except TypeError:
            return None
        
    def encode_character(self, immune, char):
        """
        Default implementation that should be overridden in specific codecs.

        @param immune: characters immune to encoding
        @param char: the character to encode
        @return: the encoded Character
        """
        raise NotImplementedError()
        
    def decode(self, encoded):
        """
        Decode a String that was encoded using the encode method in this Class

        @param encoded: the string to decode
        @return: the decoded string
        """
        buf = ''
        pbs = PushbackString(encoded)
        while pbs.has_next():
            char = self.decode_character(pbs)
            if char is not None:
                buf += char
            else:
                buf += pbs.next()
        return buf
        
    def decode_character(self, pbs):
        """
        Returns the decoded version of the next character from the input 
        string and advances the current character in the PushbackString.  
        If the current character is not encoded, this method MUST reset the 
        PushbackString.

        @param pbs: the PushBackString to decode a character from

        @return: the decoded Character
        """
        raise NotImplementedError()
