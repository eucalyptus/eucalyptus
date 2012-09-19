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
var DEPRECATE = false; 
var RETURN_KEY_CODE = 13;
var RETURN_MAC_KEY_CODE = 10;
var BACKSPACE_KEY_CODE = 8;

var RERFRESH_INTERVAL_SEC = 10;

var ID_SEPARATOR = ',';

var DOM_BINDING = {header:'.euca-container .euca-header-container .inner-container',
                   main:'.euca-main-outercontainer .inner-container #euca-main-container',
                   notification:'.euca-container .euca-notification-container .inner-container #euca-notification',
                   explorer:'.euca-explorer-container .inner-container',
                   footer:'.euca-container .euca-footer-container .inner-container',
                   hidden:'.euca-hidden-container',
                  };

var KEY_PATTERN = new RegExp('^[A-Za-z0-9_\s-]{1,256}$');
var VOL_ID_PATTERN = new RegExp('^vol-[A-Za-z0-9]{8}$');

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

function getRandomInt (min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
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

function allInArray(val_arr, larger_arr){
   var ret = true;
   for (i in val_arr){
     if($.inArray(val_arr[i], larger_arr)<0){
       ret = false;
       break;
     }
   }
   return ret;
}

function onlyInArray(val, array){
  for (i in array) { 
    if (array[i] !== val )
      return false;
  }
  return true; 
}

var _rqueue = {};
function _delayedExec(cbName, callback){
  if(cbName in _rqueue){  
     setTimeout(function() { return _delayedExec(cbName, callback);}, _rqueue[cbName]);
     callback.apply(this);
  }
}

function runRepeat(callback, millisec, startNow){
  var cbName = null; 
  do{
    cbName = 'q-'+S4()+'-'+S4();
  }while(cbName in _rqueue);
  _rqueue[cbName] = millisec;
  if(startNow)
    callback.apply(this);
  setTimeout(function(){ return _delayedExec(cbName, callback);}, millisec);
  return cbName;
}

function cancelRepeat (cbName){
  delete _rqueue[cbName];
}
    
function clearRepeat() {
  _rqueue = {}; 
}
    
function describe(resource){
  return $('html body').eucadata('get', resource);
}

function addKeypair(){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).keypair();
  $('html body').find(DOM_BINDING['hidden']).keypair('dialogAddKeypair');
}

function addGroup(){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).sgroup();
  $('html body').find(DOM_BINDING['hidden']).sgroup('dialogAddGroup');
}

function addSnapshot(volume){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).snapshot();
  $('html body').find(DOM_BINDING['hidden']).snapshot('dialogAddSnapshot', volume);
}

function addVolume(snapshot){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).volume();
  $('html body').find(DOM_BINDING['hidden']).volume('dialogAddVolume', snapshot);
}

function attachVolume(volume, instance){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).volume();
  $('html body').find(DOM_BINDING['hidden']).volume('dialogAttachVolume', volume, instance);
}

function associateIp(instance) {
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).eip({'from_instance' : true});
  $('html body').find(DOM_BINDING['hidden']).eip('dialogAssociateIp', null, instance);
}

function disassociateIp(address){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).eip({'from_instance' : true});
  $('html body').find(DOM_BINDING['hidden']).eip('dialogDisassociateIp', [address]);
}

function logout(){
  $.cookie('session-id',''); 
  location.href='/';
}

function formatDateTime(data) {
 d = new moment(data);
 return d.format(TIME_FORMAT);
}
function startLaunchWizard(filter) {
  var $container = $('html body').find(DOM_BINDING['main']);
  $container.maincontainer("changeSelected", null, { selected:'launcher', filter: filter});
}
