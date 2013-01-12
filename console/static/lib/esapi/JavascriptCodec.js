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


JavascriptCodec = function() {
    var _super = new Codec();

    return {
        encode: function(aImmune, sInput) {
            var out = '';
            for (var idx = 0; idx < sInput.length; idx ++) {
                var ch = sInput.charAt(idx);
                if (aImmune.indexOf(ch) != -1) {
                    out += ch;
                }
                else {
                    var hex = Codec.getHexForNonAlphanumeric(ch);
                    if (hex == null) {
                        out += ch;
                    }
                    else {
                        var tmp = ch.charCodeAt(0).toString(16);
                        if (ch.charCodeAt(0) < 256) {
                            var pad = "00".substr(tmp.length);
                            out += "\\x" + pad + tmp.toUpperCase();
                        }
                        else {
                            pad = "0000".substr(tmp.length);
                            out += "\\u" + pad + tmp.toUpperCase();
                        }
                    }
                }
            }
            return out;
        },

        decode: _super.decode,

        decodeCharacter: function(oPushbackString) {
            oPushbackString.mark();
            var first = oPushbackString.next();
            if (first == null) {
                oPushbackString.reset();
                return null;
            }

            if (first != '\\') {
                oPushbackString.reset();
                return null;
            }

            var second = oPushbackString.next();
            if (second == null) {
                oPushbackString.reset();
                return null;
            }

            // \0 collides with the octal decoder and is non-standard
            // if ( second.charValue() == '0' ) {
            //      return Character.valueOf( (char)0x00 );
            if (second == 'b') {
                return 0x08;
            } else if (second == 't') {
                return 0x09;
            } else if (second == 'n') {
                return 0x0a;
            } else if (second == 'v') {
                return 0x0b;
            } else if (second == 'f') {
                return 0x0c;
            } else if (second == 'r') {
                return 0x0d;
            } else if (second == '\"') {
                return 0x22;
            } else if (second == '\'') {
                return 0x27;
            } else if (second == '\\') {
                return 0x5c;
            } else if (second.toLowerCase() == 'x') {
                out = '';
                for (var i = 0; i < 2; i++) {
                    var c = oPushbackString.nextHex();
                    if (c != null) {
                        out += c;
                    } else {
                        input.reset();
                        return null;
                    }
                }
                try {
                    n = parseInt(out, 16);
                    return String.fromCharCode(n);
                } catch (e) {
                    oPushbackString.reset();
                    return null;
                }
            } else if (second.toLowerCase() == 'u') {
                out = '';
                for (i = 0; i < 4; i++) {
                    c = oPushbackString.nextHex();
                    if (c != null) {
                        out += c;
                    } else {
                        input.reset();
                        return null;
                    }
                }
                try {
                    var n = parseInt(out, 16);
                    return String.fromCharCode(n);
                } catch (e) {
                    oPushbackString.reset();
                    return null;
                }
            } else if (oPushbackString.isOctalDigit(second)) {
                var out = second;
                var c2 = oPushbackString.next();
                if (!oPushbackString.isOctalDigit(c2)) {
                    oPushbackString.pushback(c2);
                } else {
                    out += c2;
                    var c3 = oPushbackString.next();
                    if (!oPushbackString.isOctalDigit(c3)) {
                        oPushbackString.pushback(c3);
                    } else {
                        out += c3;
                    }
                }

                try {
                    n = parseInt(out, 8);
                    return String.fromCharCode(n);
                } catch (e) {
                    oPushbackString.reset();
                    return null;
                }
            }
            return second;
        }
    };
};
