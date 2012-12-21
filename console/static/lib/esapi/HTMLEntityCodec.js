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


HTMLEntityCodec = function() {
    var _super = new Codec();

    var getNumericEntity = function(input) {
        var first = input.peek();
        if (first == null) {
            return null;
        }

        if (first == 'x' || first == 'X') {
            input.next();
            return parseHex(input);
        }
        return parseNumber(input);
    };

    var parseNumber = function(input) {
        var out = '';
        while (input.hasNext()) {
            var c = input.peek();
            if (c.match(/[0-9]/)) {
                out += c;
                input.next();
            } else if (c == ';') {
                input.next();
                break;
            } else {
                break;
            }
        }

        try {
            return parseInt(out);
        } catch (e) {
            return null;
        }
    };

    var parseHex = function(input) {
        var out = '';
        while (input.hasNext()) {
            var c = input.peek();
            if (c.match(/[0-9A-Fa-f]/)) {
                out += c;
                input.next();
            } else if (c == ';') {
                input.next();
                break;
            } else {
                break;
            }
        }
        try {
            return parseInt(out, 16);
        } catch (e) {
            return null;
        }
    };

    var getNamedEntity = function(input) {
        var entity = '';
        while (input.hasNext()) {
            var c = input.peek();
            if (c.match(/[A-Za-z]/)) {
                entity += c;
                input.next();
                if (entityToCharacterMap['&' + entity] != undefined) {
                    if (input.peek(';')) input.next();
                    break;
                }
            } else if (c == ';') {
                input.next();
            } else {
                break;
            }
        }

        return String.fromCharCode(entityToCharacterMap['&' + entity.toLowerCase()]);
    };

    return {
        encode: _super.encode,

        decode: _super.decode,

        encodeCharacter: function(aImmune, c) {
            if (aImmune.indexOf(c) != -1) {
                return c;
            }

            var hex = Codec.getHexForNonAlphanumeric(c);
            if (hex == null) {
                return c;
            }

            var cc = c.charCodeAt(0);
            if (( cc <= 0x1f && c != '\t' && c != '\n' && c != '\r' ) || ( cc >= 0x7f && cc <= 0x9f ) || c == ' ') {
                return " ";
            }

            var entityName = characterToEntityMap[cc];
            if (entityName != null) {
                return entityName + ";";
            }

            return "&#x" + hex + ";";
        },

        decodeCharacter: function(oPushbackString) {
            //noinspection UnnecessaryLocalVariableJS
            var input = oPushbackString;
            input.mark();
            var first = input.next();
            if (first == null || first != '&') {
                input.reset();
                return null;
            }

            var second = input.next();
            if (second == null) {
                input.reset();
                return null;
            }

            if (second == '#') {
                var c = getNumericEntity(input);
                if (c != null) {
                    return c;
                }
            } else if (second.match(/[A-Za-z]/)) {
                input.pushback(second);
                c = getNamedEntity(input);
                if (c != null) {
                    return c;
                }
            }
            input.reset();
            return null;
        }
    };
};
