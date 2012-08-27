/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

/* Constants */
var RETURN_KEY_CODE = 13;
var RETURN_MAC_KEY_CODE = 10;
var BACKSPACE_KEY_CODE = 8;

var ID_SEPARATOR = ',';

var DOM_BINDING = {header:'.euca-container .euca-header-container .inner-container',
                   main:'.euca-main-outercontainer .inner-container #euca-main-container',
                   notification:'.euca-container .euca-notification-container .inner-container #euca-notification',
                   explorer:'.euca-explorer-container .inner-container',
                   footer:'.euca-container .euca-footer-container .inner-container',
                  };

function escapeHTML(input) {
  return $('<div/>').text(input).html();
}

function asHTML(input) {
  return $('<div/>').append(input).html();
}


function isFunction(obj) {
  return obj && {}.toString.call(obj) == '[object Function]';
}


/** Add Array.indexOf to IE **/
if( !Array.prototype.indexOf ) {
  Array.prototype.indexOf = function(needle) {
    for(var i = 0; i < this.length; i++) {
      if(this[i] === needle) {
        return i;
      }
    }
    return -1;
  };
}

function S4() {
  return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
}

function notifySuccess(title, message) {
  $('html body').find(DOM_BINDING['notification']).notification('success', title, message);
}

function notifyError(title, message, proxyMessage, code) {
  var desc = message;
  if (proxyMessage)
    desc += "("+proxyMessage+")";
  
  $('html body').find(DOM_BINDING['notification']).notification('error', title, desc, code);
}
