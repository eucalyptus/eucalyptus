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
var MAX_PENDING_REQ = 16;
var ID_SEPARATOR = ',';

var DOM_BINDING = {header:'.euca-container .euca-header-container .inner-container',
                   main:'.euca-main-outercontainer .inner-container #euca-main-container',
                   notification:'.euca-container .euca-notification-container .inner-container #euca-notification',
                   explorer:'.euca-explorer-container .inner-container',
                   footer:'.euca-container .euca-footer-container .inner-container',
                   hidden:'.euca-hidden-container'
                  };

var SGROUP_NAME_PATTERN = new RegExp('^[ A-Za-z0-9_\-]{1,242}$');
var KEY_PATTERN = new RegExp('^[ A-Za-z0-9_\-]{1,242}$');
var VOL_ID_PATTERN = new RegExp('^vol-[A-Za-z0-9]{8}$');
var IP_PATTER = new RegExp('[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$');
var MAX_DESCRIPTION_LEN = 255;
var KEEP_VIEW = 'keep_view';

var IMG_OPT_PARAMS='';

function isValidIPv4Address(ipaddr) {
  ipaddr = ipaddr.replace( /\s/g, "")
  if (IP_PATTER.test(ipaddr)) {
    var parts = ipaddr.split(".");
    if (parseInt(parts[0]) == 0) {
      return false;
    }
    for (var i=0; i<parts.length; i++) {
      var j = parseInt(parts[i]);
      if ( j > 255 || j < 0){
        return false;
      }
    }
    return true;
  } else {
    return false;
  }
}

function ipv4AsInteger(ipaddr) {
  var count = 0;
  if (IP_PATTER.test(ipaddr)) {
    var parts = ipaddr.split(".");
    for (var i=0; i<4; i++) {
      var j = parseInt(parts[i]);
      count += Math.pow(256, 3-i)*j
    }
    return count;
  } else {
    return 0;
  }
}
function asText(input) {
  return input; /* we don't do any transformation at this point */
}

function asHTML(input) {
  return $('<div/>').append(input).html();
}

function toBase64(input) {
  return $.base64.encode(input);
}

function isFunction(obj) {
  return obj && {}.toString.call(obj) == '[object Function]';
}

function isInt(integer){
  var intRegex = /^\d+$/;
  if(intRegex.test(integer))
    return true;
  return false; 
};

/** Add Array.indexOf to IE  **/
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

function addEllipsis(input, maxLen){
  if (input == undefined)
    return input;
//  input = DefaultEncoder().encodeForHTML(input);
  if (input.length < maxLen)
    return input;
  input = input.substring(0, maxLen);
  i = input.lastIndexOf(" ");
  if ( i > 0)
    input = input.substring(0, i);
  return input + '...';
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

function notifyMulti(percent, desc, error){
  $('html body').find(DOM_BINDING['notification']).notification('multi', {percent:percent,desc:desc,error:error });
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

/////////  Codes for repeated execution of callbacks ////////
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
//////////////////////////////////////////////////////////////
 
function describe(resource, resourceId){
  var result= $('html body').eucadata('get', resource);
  if(!resourceId)
    return result;

  if (result){
    for(i in result){
      if(result[i].id && result[i].id.toUpperCase() === resourceId.toUpperCase())
        return result[i];
    }
  }
  return null;
}

var unescape_vector = {
  instance: {
    root_device_name: true,
    reason: true,
    launch_time: true
  },
  image: {
    root_device_name: true,
    location: true //can we?
  },
  volume: {
    create_time: true
  },
  snapshot: {
    start_time: true,
    progress: true
  },
  sgroup: {
    rules: { 
      grants: {
        cidr_ip: true 
      }
    }
  },
  keypair: {
    fingerprint: true
  },
  zone: {
    state: true
  }
}

function escapeResponse(resource, result){
  return escapeData(result, unescape_vector[resource]); 
}

function escapeData(data, escVector){
  if($.type(data) === 'array' || $.type(data) === 'object'){
    for (i in data){
      var vectorForward = null;
      if($.type(data)==='object'){
        if(escVector && escVector[i])
          vectorForward = escVector[i];
      }else{ // array
          vectorForward = escVector;
      }
      data[i] = escapeData(data[i],vectorForward);
    }
    return data;
  }else{
    if(escVector){
      if(!data)
        return data;
      var $esc = $('<div>');
      return $esc.html(data).text();
    }else
      return data;
  }
}

function refresh(resource){
  return $('html body').eucadata('refresh', resource);
}

function addKeypair(callback){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).keypair({hidden:true});
  $('html body').find(DOM_BINDING['hidden']).keypair('dialogAddKeypair',callback);
}

function addGroup(callback){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).sgroup({hidden:true});
  $('html body').find(DOM_BINDING['hidden']).sgroup('dialogAddGroup',callback);
}

function addSnapshot(volume){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).snapshot({hidden:true});
  $('html body').find(DOM_BINDING['hidden']).snapshot('dialogAddSnapshot', volume);
}

function addVolume(snapshot){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).volume({hidden:true});
  $('html body').find(DOM_BINDING['hidden']).volume('dialogAddVolume', snapshot);
}

function attachVolume(volume, instance){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).volume({hidden:true});
  $('html body').find(DOM_BINDING['hidden']).volume('dialogAttachVolume', volume, instance);
}

function associateIp(instance) {
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).eip({from_instance: true, hidden:true});
  $('html body').find(DOM_BINDING['hidden']).eip('dialogAssociateIp', null, instance);
}

function disassociateIp(address){
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).eip({from_instance: true, hidden:true});
  $('html body').find(DOM_BINDING['hidden']).eip('dialogDisassociateIp', [address]);
}

function allocateIP() {
  $('html body').find(DOM_BINDING['hidden']).children().detach();
  $('html body').find(DOM_BINDING['hidden']).eip({hidden:true});
  $('html body').find(DOM_BINDING['hidden']).eip('createAction');
}

var _logoutSent = false;
function logout(){
  if (_logoutSent)
    return;
  _logoutSent = true;
  $.when( // this is to synchronize a chain of ajax calls 
    $.ajax({
      type:"POST",
      data:"action=logout&_xsrf="+$.cookie('_xsrf'),
      dataType:"json",
      async:"false", // async option deprecated as of jQuery 1.8
      timeout: 10000 // shorter timeout for log-out
    })).always(function(out){
      var hostname = null;
      /*if (location.href && location.href.indexOf('hostname=') >= 0){
        hostname = location.href.substring(location.href.indexOf('hostname='));
        hostname = hostname.replace(/#.+?$/,'');
        hostname = hostname.replace('hostname=','');
        hostname = hostname.replace('#','');
        hostname = hostname.replace('/','');
      }*/
      if(!hostname)
        hostname = location.hostname;
      var href = '';
      if(location.port && location.port > 0)
        href = location.protocol + '//' + hostname + ':' + location.port + '/';
      else
        href = location.protocol + '//' + hostname + '/'; 
      location.href=href;
    });
}

function formatDateTime(data) {
 var d = new moment(data);
 return d.format(TIME_FORMAT);
}

function startLaunchWizard(filter) {
  var $container = $('html body').find(DOM_BINDING['main']);
  $container.maincontainer("changeSelected", null, { selected:'launcher', filter: filter});
}

function getErrorMessage(jqXHR) {
  if (jqXHR.status == 504) {
    return $.i18n.prop('general_timeout', "<a href='"+$.eucaData.g_session['admin_support_url']+"'>"+cloud_admin+"</a>");
  }
  if (jqXHR && jqXHR.responseText) {
    response = jQuery.parseJSON(jqXHR.responseText);
    return response.message ? response.message : undefined_error;
  } else {
    return undefined_error;
  }
}

function isRootVolume(instanceId, volumeId) {
  var instance = describe('instance', instanceId);
  if ( instance && instance.root_device_type && instance.root_device_type.toLowerCase() == 'ebs' ) {
    var rootDeviceName = getRootDeviceName(instance);
    var rootVolume = instance.block_device_mapping[rootDeviceName];
    if ( rootVolume && rootVolume.volume_id === volumeId ) {
      return true;
    }
  }
  return false;
}

function getRootDeviceName(resource){
  return resource.root_device_name ? resource.root_device_name.replace('&#x2f;','/').replace('&#x2f;','/') : '/dev/sda1';
}

function generateSnapshotToImageMap(){
  var images = describe('image');
  var snapToImageMap = {}
  $.each(images, function(idx, image){
    if(image.block_device_mapping){
      vol = image.block_device_mapping[getRootDeviceName(image)];
      if (vol) {
        snapshot = vol['snapshot_id'];
        if (snapToImageMap[snapshot])
          snapToImageMap[snapshot].push(image['id']);
        else {
          snapToImageMap[snapshot] = [image['id']];
        }
      }
    }
  });
  return snapToImageMap;
}

var tableRefreshCallback = null; // hacky..but callback name inside the table breaks with flippy help

function inferImage(manifest, desc, platform) {
  if(!platform)
    platform='linux';
  var name = platform;
 // Regex '$distro[seperator]$version' 
  var inferMap = 
    {'rhel5':new RegExp('(rhel|redhat).5','ig'),
     'rhel6':new RegExp('(rhel|redhat).6','ig'),
     'rhel':new RegExp('(rhel|redhat)','ig'),
     'centos5':new RegExp('centos.5','ig'),
     'centos6':new RegExp('centos.6','ig'),
     'centos':new RegExp('centos','ig'),
     'lucid': new RegExp('(lucid|ubuntu.10[\\W\\s]04)','ig'),
     'precise':new RegExp('(precise|ubuntu.12[\\W\\s]04)','ig'),
     'ubuntu':new RegExp('ubuntu','ig'),
     'debian' :new RegExp('debian','ig'), 
     'fedora' : new RegExp('fedora','ig'),
     'opensuse' : new RegExp('opensuse','ig'),
     'suse' : new RegExp('suse', 'ig'),
     'gentoo' : new RegExp('gentoo', 'ig'),
     'linux' : new RegExp('linux','ig'),
     'windows' :new RegExp('windows','ig')
    };
  for (key in inferMap){
    var reg = inferMap[key];
    if(reg.test(manifest) || reg.test(desc)){
      name = key;
      break;
    }
  }
  return name;
}
function getImageName(imgKey){
  if(!imgKey)
    imgKey = 'linux';

  var nameMap = {
    'rhel5' : 'Red Hat 5',
    'rhel6' : 'Red Hat 6',
    'rhel' : 'Red Hat',
    'centos5' : 'CENT OS 5',
    'centos6' : 'CENT OS 6',
    'centos' : 'CENT OS',
    'lucid' : 'Ubuntu Lucid(10.04)',
    'precise' : 'Ubuntu Precise(12.04)',
    'ubuntu' : 'Ubuntu',
    'debian' : 'Debian',
    'fedora' : 'Fedora',
    'opensuse' : 'Open Suse',
    'suse' : 'Suse Linux',
    'gentoo' : 'Gentoo',
    'linux' : 'Linux',
    'windows' : 'Windows'
  };
  return nameMap[imgKey];
}

function errorAndLogout(errorCode){
  if (_logoutSent)
    return;
  // turn off all eucaData requests
  $('html body').eucadata('disable');
  $('html body').find(DOM_BINDING['hidden']).login();
  var errorMsg = null;
  var bError = false;
  if (errorCode === 401 || errorCode === 403)
    errorMsg = $.i18n.prop('login_timeout', '<a href="#">'+cloud_admin+'</a>');
  else{
    errorMsg = $.i18n.prop('connection_failure', '<a href="#">'+cloud_admin+'</a>');
    bError = true;
  }

  $('html body').find(DOM_BINDING['hidden']).login('popupError', bError, errorMsg, function(){
    logout();
  });
}

function iamBusy(){
  $.ajax({
    type: 'POST',
    url: '/',
    data:"action=busy&_xsrf="+$.cookie('_xsrf'),
    dataType:"json",
      success: function(data, textStatus, jqXHR) {
        ;
      }
  });
}

function setDataInterest(resources){
  data = "&_xsrf="+$.cookie('_xsrf');
  for (res in resources) {
    data += "&Resources.member."+(parseInt(res)+1)+"="+resources[res];
  }
  $.ajax({
    type: 'POST',
    url: '/ec2?Action=SetDataInterest',
    data:data,
    dataType:"json",
      success: function(data, textStatus, jqXHR) {
        ;
      }
  });
}


function popOutDialogHelp(url, height){
  var width = 600;
  var height = height ? height: 400;
  var option = 'width='+width+',height='+height+',directories=no,fullscreen=no,location=no,menubar=no,resizable=yes,scrollbars=yes,status=yes,titlebar=yes,toobar=no';
  window.open(url, '_blank', option,true);
}

function popOutPageHelp(url, height){
  var width = 700;
  var height = height ? height: 600;
  var option = 'width='+width+',height='+height+',directories=no,fullscreen=no,location=no,menubar=no,resizable=yes,scrollbars=yes,status=yes,titlebar=yes,toobar=no';
  window.open(url, '_blank', option,true);
}

var PROXY_TIMEOUT = 120000;
function setupAjax(){
 $.ajaxSetup({
   type: "POST",
   timeout: 30000
 });
}

function doMultiAjax(array, callback, delayMs){
  if (!array || array.length <=0 || !callback)
    return;
  var i =0;
  var doNext = function(array, i){
    $.when( 
      (function(){ 
        var dfd = new $.Deferred();
        callback(array[i], dfd);
        return dfd.promise();
      })()
    ).always(function() {
       if( i+1 < array.length){
         if(delayMs && delayMs > 0)
           setTimeout(function(){return doNext(array, i+1)}, delayMs);
         else
           doNext(array, i+1);
       }
    });
  };

  doNext(array, 0);
}

function sortArray(array, comperator){
  if(!comperator){
    comperator = function(item1, item2){ return item1 < item2}
  }
  var mergeSort = function(arr, left, right){
    if(left < right){
      var leftArr = mergeSort(arr, left, Math.floor((left+right)/2));
      var rightArr = mergeSort(arr, Math.floor((left+right)/2) +1 , right);
      return merge(leftArr, rightArr); 
    }else if (left === right){
      return [arr[left]];
    }   
  };   
  var merge = function(arr1, arr2){
    var merged = [];
    var idx1 = 0;
    var idx2 = 0;
    while(idx1 < arr1.length  || idx2 < arr2.length){
      if(idx1 < arr1.length && idx2 < arr2.length){
        if(comperator(arr1[idx1], arr2[idx2]))
          merged.push(arr1[idx1++]);
        else
          merged.push(arr2[idx2++]);
      }else if(idx1 < arr1.length){
        merged.push(arr1[idx1++]);
      }else
        merged.push(arr2[idx2++]);
    }
    return merged;
  }
  if(! array || array.length <= 1)
    return array;
  return mergeSort(array, 0, array.length-1);
}

function trim (str) {
    if(str)
      return str.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
    else
      return str;
}

function getTagForResource(res_id) {
  var result= $('html body').eucadata('get', 'tag');
//  console.log("res_id="+res_id);
//  console.log("tag list"+result);
  return res_id;
}

//----------------------------------------------------------------------------
//
// Display Helper Functions for providing data types for eucatable rendering
//
//----------------------------------------------------------------------------


// For displaying the type 'twist', which is defined in CSS, in eucatable
function eucatableDisplayColumnTypeTwist (title, str, limit) {
	if( str == null ){
		return "ERROR!";
	}
	shortStr = addEllipsis(str, limit);
	$html = $('<a>').attr('title', title).addClass('twist').text(shortStr);
	return asHTML($html); 
}

// For displaying the type 'text' in eucatable
function eucatableDisplayColumnTypeText (title, str, limit){
	if( str == null ){
		return "";
	}
	shortStr = addEllipsis(str, limit); 
	$html = $('<span>').attr('title', title).text(shortStr);
        return asHTML($html);
}

// For displaying the instance status icon in eucatable
function eucatableDisplayColumnTypeInstanceStatus (iStatus){
        // '-' has an issue with Messages.properties; shutting-down -> shuttingdown
        iStatusProcessed = iStatus.replace('-','');
        $html = $('<div>').addClass('table-row-status').addClass('status-'+iStatusProcessed).append('&nbsp;');
        return asHTML($html);
}

// For displaying the volume status icon in eucatable
function eucatableDisplayColumnTypeVolumeStatus (vStatus){
	$html = $('<div>').addClass('table-row-status').addClass('status-'+vStatus).append('&nbsp;');
	return asHTML($html);
}

// For displaying the snapshot statuis icon in eucatable
function eucatableDisplayColumnTypeSnapshotStatus (snStatus, snProgress){
	$html = $('<div>').addClass('table-row-status').addClass('status-'+snStatus).append(snStatus=='pending' ?  snProgress : '&nbsp;');
        return asHTML($html);
}

// For displaying the launch instance button in eucatable
function eucatableDisplayColumnTypeLaunchInstanceButton (imageID){
	sanitizedID =  DefaultEncoder().encodeForJavaScript(imageID);
	onclick_code = 'startLaunchWizard({image:\'' + sanitizedID + '\'}); $(\'html body\').trigger(\'click\', \'create-new\'); return false;';
	// is image_launch_btn a global variable?
	$html = $('<a>').attr('onclick', onclick_code).addClass('button table-button').attr('href', '#').text(image_launch_btn);
	return asHTML($html);
};

