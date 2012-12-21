/*
 * OWASP Enterprise Security API (ESAPI)
 *
 * This file is part of the Open Web Application Security Project (OWASP)
 * Enterprise Security API (ESAPI) project. For details, please see
 * <a href="http://www.owasp.org/index.php/ESAPI">http://www.owasp.org/index.php/ESAPI</a>.
 *
 * Copyright (c) 2008 - The OWASP Foundation
 *
 * The ESAPI is published by OWASP under the BSD license. You should read and accept the
 * LICENSE before you use, modify, and/or redistribute this software.
$namespace('org.owasp.esapi.codecs');
 */


PushbackString = function(sInput) {
    var _input = sInput,
            _pushback = '',
            _temp = '',
            _index = 0,
            _mark = 0;

    return {
        pushback: function(c) {
            _pushback = c;
        },

        index: function() {
            return _index;
        },

        hasNext: function() {
            if (_pushback != null) return true;
            return !(_input == null || _input.length == 0 || _index >= _input.length);

        },

        next: function() {
            if (_pushback != null) {
                var save = _pushback;
                _pushback = null;
                return save;
            }
            if (_input == null || _input.length == 0 || _index >= _input.length) {
                return null;
            }
            return _input.charAt(_index++);
        },

        nextHex: function() {
            var c = this.next();
            if (this.isHexDigit(c)) return c;
            return null;
        },

        nextOctal: function() {
            var c = this.next();
            if (this.isOctalDigit(c)) return c;
            return null;
        },

        isHexDigit: function(c) {
            return c != null && ( ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'f' ) || ( c >= 'A' && c <= 'F' ) );
        },

        isOctalDigit: function(c) {
            return c != null && ( c >= '0' && c <= '7' );
        },

        peek: function(c) {
            if (!c) {
                if (_pushback != null) return _pushback;
                if (_input == null || _input.length == 0 || _index >= _input.length) return null;
                return _input.charAt(_index);
            } else {
                if (_pushback != null && _pushback == c) return true;
                if (_input == null || _input.length == 0 || _index >= _input.length) return false;
                return _input.charAt(_index) == c;
            }
        },

        mark: function() {
            _temp = _pushback;
            _mark = _index;
        },

        reset: function() {
            _pushback = _temp;
            _index = _mark;
        },

        remainder: function() {
            var out = _input.substr(_index);
            if (_pushback != null) {
                out = _pushback + out;
            }
            return out;
        }
    };
};
