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

(function($, eucalyptus) {
  $.widget('eucalyptus.eucadata', $.eucalyptus.eucawidget, {
    options : {  
      refresh_interval_sec : REFRESH_INTERVAL_SEC,
      max_refresh_attempt : 3,
      endpoints: [{name:'summary', type:'dash', url: '/ec2?Action=GetDashSummary'},
                  {name:'instance', type:'instances', url: '/ec2?Action=DescribeInstances'},
                  //{name:'image', type:'images', url: '/ec2?Action=DescribeImages&Owner=self'},
                  {name:'image', type:'images', url: '/ec2?Action=DescribeImages'},
                  {name:'volume', type:'volumes', url: '/ec2?Action=DescribeVolumes'},
                  {name:'snapshot', type:'snapshots', url: '/ec2?Action=DescribeSnapshots'},
                  {name:'eip', type:'addresses', url: '/ec2?Action=DescribeAddresses'},
                  {name:'keypair', type:'keypairs', url: '/ec2?Action=DescribeKeyPairs'},
                  {name:'sgroup', type:'groups', url: '/ec2?Action=DescribeSecurityGroups'},
                  {name:'zone', type:'zones', url: '/ec2?Action=DescribeAvailabilityZones'},
                  {name:'tag', type:'tags', url: '/ec2?Action=DescribeTags'},
                  {name:'bucket', type:'buckets', url: '/s3?Action=DescribeBuckets'},
                  {name:'balancer', type:'balancers', url: '/elb?Action=DescribeLoadBalancers'},
                  {name:'scalinggrp', type:'scalinggrps', url: '/autoscaling?Action=DescribeAutoScalingGroups'},
                  {name:'scalinginst', type:'scalinginsts', url: '/autoscaling?Action=DescribeAutoScalingInstances'}
      ], 
    },
//    _data : {summary:[], instance:null, image:null, volume:null, snapshot:null, eip:null, keypair: null, sgroup: null, zone: null, tag: null, bucket: null, balancer: null, scalinggrp: null, scalinginst: null},
    _data : {summary:[], instance:null, image:null, volume:null, snapshot:null, eip:null, keypair: null, sgroup: null, zone: null, tag: null, bucket:null},
    _callbacks : {}, 
    _listeners : {},
    _init : function(){ },
    _numPending : 0,
    _enabled : true,
    _errorCode : null, // the http status code of the latest response
    _data_needs : null, // if this is set, only resources listed will be fetched from the proxy
    _create : function(){
      var thisObj = this;
      
      $.each(thisObj.options.endpoints, function(idx, ep){
        var name = ep.name;
        var url = ep.url;
        thisObj._callbacks[name] = {callback: function(){
         if(!thisObj._enabled || thisObj.countPendingReq() > MAX_PENDING_REQ ) {
           return;
         }
         if (thisObj._data_needs && thisObj._data_needs.indexOf(ep.type) == -1) {
           return;
         }
         // don't skip the read if data cache is empty
         if(thisObj._data[name] != null) {
            // however, if we have data, should we read it again?

            // if summary doesn't contain this resource name, skip the update
             if (name != 'summary') {
//               console.log('should pull '+name+": "+thisObj._data['summary'].results[name]);
//               if (thisObj._data['summary'].results[name] == undefined) {
                 // still need to hit the listeners
//                 if(thisObj._listeners[name] && thisObj._listeners[name].length >0) {
//                   $.each(thisObj._listeners[name], function (idx, callback){
//                     callback['callback'].apply(thisObj);
//                   });
//                 }
//                 return;
//               }
             }
         }
         $.ajax({
           type:"POST",
           url: url,
           data:"_xsrf="+$.cookie('_xsrf'),
           dataType:"json",
           async:"false",
           beforeSend: function(jqXHR, settings){
             thisObj._numPending++;
           },
           success: function(data, textStatus, jqXHR){
             thisObj._numPending--;
             if (data.results) {
               //delete thisObj._data[name];
               thisObj._data[name] = {
                 lastupdated: new Date(),
                 results: data.results,
               }
               //console.log(data.results.length+" "+name+" returned, data.length:"+thisObj._data[name].results.length);
               if(thisObj._listeners[name] && thisObj._listeners[name].length >0) {
                 $.each(thisObj._listeners[name], function (idx, callback){
                   callback['callback'].apply(thisObj);
                 });
               }
             }else {
               ;
             }
           },
           error: function(jqXHR, textStatus, errorThrown){ 
             thisObj._errorCode = jqXHR.status;
             thisObj._numPending--;
             if(thisObj._data[name]){
               var last = thisObj._data[name]['lastupdated'];
               var now = new Date();
               var elapsedSec = Math.round((now-last)/1000);             
               if((jqXHR.status === 401 || jqXHR === 403)  ||
                  (elapsedSec > thisObj.options.refresh_interval_sec*thisObj.options.max_refresh_attempt)){
                 delete thisObj._data[name];
                 thisObj._data[name] = null;
               }
               if(thisObj.getStatus() !== 'online'){
                 errorAndLogout(thisObj._errorCode);
               }
               if (jqXHR.status === 504) {
                 notifyError($.i18n.prop('data_load_timeout'));
               }
            }
          }});
        }, repeat: null};
      });
      this._describe();
    }, 
    _destroy : function(){
    },
    _describe : function(){
      var thisObj = this;
      $.each(thisObj.options.endpoints, function(idx, ep){
        var name = ep.name;
        // probobly don't need randomness when we're requesting a lot less data in general
        //var interval = getRandomInt((thisObj.options.refresh_interval_sec*1000)/2,(thisObj.options.refresh_interval_sec*1000)*2);
        var interval = thisObj.options.refresh_interval_sec*1000;
        thisObj._callbacks[name].repeat = runRepeat(thisObj._callbacks[name].callback, interval, true); // random ms is added to distribute sync interval
      });
    },
     
/***** Public Methods *****/
    // e.g., get(instance, id);
    get : function(resource){
      var thisObj = this;
      if(thisObj._data[resource])
        return thisObj._data[resource].results;
      else
        return null;
    },

    countPendingReq : function(){
      var thisObj = this;
      return thisObj._numPending;
    }, 

    addCallback : function(resource, source, callback){
      var thisObj = this;
      if (typeof(callback) === "function"){
        if(!thisObj._listeners[resource])
          thisObj._listeners[resource] = [];
        var duplicate = false;
        for (i=0; i<thisObj._listeners[resource].length; i++){
          var cb = thisObj._listeners[resource][i];
          if(cb['src'] === source){ 
            duplicate = true; 
            break; 
          }
        }
        if(!duplicate)
          thisObj._listeners[resource].push({src:source, callback: callback});
      }
    },
    
    removeCallback : function(resource, source){
      var thisObj = this;
      if (! thisObj._listeners[resource] || thisObj._listeners[resource].length <=0)
        return;
      var toDelete = -1;
      for (i=0; i< thisObj._listeners[resource].length; i++){
        var cb = thisObj._listeners[resource][i];
        if (cb['src'] === source) {
          toDelete = i;
          break;
        }
      }
      if(toDelete>=0)
        thisObj._listeners[resource].splice(toDelete, 1);
    },

    setDataNeeds : function(resources){
        this._data_needs = resources;
    },

    refresh : function(resource){
      var thisObj = this;
      if(thisObj._callbacks[resource])
        thisObj._callbacks[resource].callback.apply(this); 
    },

    // status: online, offline, error
    getStatus : function(){
      var thisObj = this;
      var status = 'online';
      var numOff = 0;
      $.each(thisObj._data, function(resource, data){
        if(!data)
          numOff++;
      });
      if(numOff >= 2) 
        status='offline';

      return status;
    },

    disable : function(){
      this._enabled = false;
    },

    enable : function(){
      this._enabled = true;
    },

    isEnabled : function(){
      return this._enabled;
    }
/**************************/ 
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
