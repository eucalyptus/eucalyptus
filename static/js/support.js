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

/* CONFIGURABLE */
var REFRESH_INTERVAL_SEC = 10;
var TABLE_REFRESH_INTERVAL_SEC = 20;
var GLOW_DURATION_SEC = 10;

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

var KEEP_VIEW = 'keep_view';

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

function getResource(resourceType, resourceId){
  res = $('html body').eucadata('get', resourceType, resourceId);
  return res ? res[0] : res;
}

function refresh(resource){
  return $('html body').eucadata('refresh', resource);
}

function addKeypair(callback){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).keypair();
  $('html body').find(DOM_BINDING['hidden']).keypair('dialogAddKeypair',callback);
}

function addGroup(callback){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).sgroup();
  $('html body').find(DOM_BINDING['hidden']).sgroup('dialogAddGroup',callback);
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

function allocateIP() {
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).eip();
  $('html body').find(DOM_BINDING['hidden']).eip('createAction');
}

function logout(){
  $.cookie('session-id',''); 
  var hostname = null;
  if (location.href && location.href.indexOf('hostname=') >= 0){
    hostname = location.href.substring(location.href.indexOf('hostname=')); 
    hostname= hostname.replace('hostname=','');
    hostname= hostname.replace('#','');
    hostname= hostname.replace('/','');
  }
  if(!hostname)
    hostname = location.hostname;
  var href = '';
  if(location.port && location.port > 0)
    href = location.protocol + '//' + hostname + ':' + location.port + '/';
  else
    href = location.protocol + '//' + hostname + '/'; 
  location.href=href;
}

function formatDateTime(data) {
 d = new moment(data);
 return d.format(TIME_FORMAT);
}

function startLaunchWizard(filter) {
  var $container = $('html body').find(DOM_BINDING['main']);
  $container.maincontainer("changeSelected", null, { selected:'launcher', filter: filter});
}

function getErrorMessage(jqXHR) {
  if (jqXHR && jqXHR.responseText) {
    response = jQuery.parseJSON(jqXHR.responseText);
    return response.message ? response.message[1] : undefined_error;
  } else {
    return undefined_error;
  }
}

function isRootVolume(instanceId, volumeId) {
  var instance = getResource('instance', instanceId);
  if ( instance.root_device_type && instance.root_device_type.toLowerCase() == 'ebs' ) {
    var rootVolume = instance.block_device_mapping[instance.root_device_name];
    if ( rootVolume.volume_id == volumeId ) {
      return true;
    }
  }
  return false;
}

var tableRefreshCallback = null; // hacky..but callback name inside the table breaks with flippy help

function isValidIp(s) {
  var arr = s.split('.');
  if(!arr || arr.length!==4)
    return false;
  for(i in arr){
    var n = parseInt(arr[i]);
    if (!(n >=0 && n<=255)){
      return false;
    }
  }
  return true;
}

