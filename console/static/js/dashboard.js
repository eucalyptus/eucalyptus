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
  $.widget('eucalyptus.dashboard', $.eucalyptus.eucawidget, {
    options : { },
    _init : function() {
      var $tmpl = $('html body div.templates').find('#dashboardTmpl').clone();       
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_dashboard)));
      var $dashboard = $wrapper.children().first();
      var $help = $wrapper.children().last();
      this._setInstSummary($dashboard.find('#dashboard-content .instances'));
      this._setStorageSummary($dashboard.find('#dashboard-content .storage'));
      this._setNetSecSummary($dashboard.find('#dashboard-content .netsec')); 
      var $wrapper = $('<div>').addClass('dashboard-wrapper');
      $dashboard.appendTo($wrapper);
      $wrapper.appendTo(this.element);
      this._addHelp($help);
    },

    _create : function() { 
    },

    _destroy : function() { },
    
    // initiate ajax call-- describe-instances
    // attach spinning wheel until we refresh the content with the ajax response
    _setInstSummary : function($instObj) {
      var thisObj = this;
      var $az=$instObj.find('#dashboard-instance-az select');

      $('html body').eucadata('addCallback', 'zone', 'dashboard-summary', function(){
         var results = describe('zone');
         var arrayAz = [];
         for( res in results) {
              var azName = results[res].name;
              arrayAz.push(azName);
         }
         var sorted = sortArray(arrayAz);
         $.each(sorted, function(idx, azName){
              $az.append($('<option>').attr('value', azName).text(azName));
         });
         $('html body').eucadata('removeCallback','zone','dashboard-summary');
      });
      $('html body').eucadata('refresh', 'zone');

            // update the display
      $az.change( function (e) {
        thisObj._reloadInstSummary($instObj);
      }); 

      $instObj.find('#dashboard-instance-running div').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $instObj.find('#dashboard-instance-stopped div').prepend(
        $('<img>').attr('src','images/dots32.gif'));

      $instObj.find('#dashboard-instance-launch a').click( function(e) {
        var $container = $('html body').find(DOM_BINDING['main']);
        $container.maincontainer("changeSelected", e, {selected:'launcher'});
        $('html body').trigger('click', 'navigator:launcher');
        return false;
      });
      thisObj._reloadInstSummary($instObj);
    },
    _addHelp : function(help){
      var thisObj = this;
      var $header = this.element.find('.box-header');
      $header.find('span').append(
          $('<div>').addClass('help-link').append(
            $('<a>').attr('href','#').text('?').click( function(evt){
              thisObj._flipToHelp(evt, {content:help, url: help_dashboard.landing_content_url} ); 
            })));
      return $header;
    },

    _reloadInstSummary : function($instObj){
      var thisObj = this;
      $('html body').eucadata('addCallback', 'instance', 'dashboard-summary', function(){
        // selector is different for these two because of extra div
        $instObj.find('#dashboard-instance-running div img').remove();
        $instObj.find('#dashboard-instance-stopped div img').remove();
        var numRunning = 0;
        var numStopped = 0;
        var az=$instObj.find('#dashboard-instance-az select').val();
        var results = describe('instance');
        $.each(results, function (idx, instance){
          if (az==='all' || instance.placement === az ){
             var state = instance.state;
             if (state == undefined) {
               state = instance._state.name;
             }
            if (state === 'running')
              numRunning++;
            else if (state === 'stopped')
              numStopped++;
          }
        });
        $instObj.find('#dashboard-instance-running span').text(numRunning);
        $instObj.find('#dashboard-instance-stopped span').text(numStopped);
      }); 
      $('html body').eucadata('refresh','instance');

      $('html body').eucadata('addCallback', 'scalinginst', 'dashboard-summary', function(){
        var results = describe('scalinginst');
        var numScaling = results ? results.length : 0;
        $instObj.find('#dashboard-scaling-groups div img').remove();
        $instObj.find('#dashboard-scaling-groups span').text(numScaling);
      });
      $('html body').eucadata('refresh','scalinginst');

      $instObj.find('#dashboard-instance-running').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'instance', filter:'running'});
          $('html body').trigger('click', 'navigator:instance');
          return false;
        }));
      $instObj.find('#dashboard-instance-stopped').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
            thisObj._trigger('select', evt, {selected:'instance', filter:'stopped'});
            $('html body').trigger('click', 'navigator:instance');
            return false;
      }));
      $instObj.find('#dashboard-scaling-groups').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
            thisObj._trigger('select', evt, {selected:'scaling', filter:'stopped'});
            $('html body').trigger('click', 'navigator:scaling');
            return false;
      }));
    },

    _setStorageSummary : function($storageObj) {
      var thisObj = this;

      $('html body').eucadata('addCallback', 'volume', 'dashboard-summary', function(){
        var results = describe('volume');
        var numVol = results ? results.length : 0;
        $storageObj.find('#dashboard-storage-volume img').remove();
        $storageObj.find('#dashboard-storage-volume span').text(numVol);
      });
      $('html body').eucadata('refresh', 'volume');
 
      $storageObj.find('#dashboard-storage-volume').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'volume'});
          $('html body').trigger('click', 'navigator:volume');
          return false;
      }));

      $('html body').eucadata('addCallback', 'snapshot', 'dashboard-summary', function(){
        var results = describe('snapshot');
        var numSnapshots = results ? results.length : 0;
        $storageObj.find('#dashboard-storage-snapshot img').remove();
        $storageObj.find('#dashboard-storage-snapshot span').text(numSnapshots);
      }); 
      $('html body').eucadata('refresh', 'snapshot');

      $storageObj.find('#dashboard-storage-snapshot').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'snapshot'});
          $('html body').trigger('click', 'navigator:snapshot');
          return false;
      }));

      $('html body').eucadata('addCallback', 'bucket', 'dashboard-summary', function(){
        var results = describe('bucket');
        var numBuckets = results ? results.length : 0;
        $storageObj.find('#dashboard-storage-buckets img').remove();
        $storageObj.find('#dashboard-storage-buckets span').text(numBuckets);
      }); 
      $('html body').eucadata('refresh', 'bucket');
      $storageObj.find('#dashboard-storage-buckets').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'bucket'});
          $('html body').trigger('click', 'navigator:bucket');
          return false;
      }));

      //az = $instObj.find('#dashboard-instance-dropbox').value();
      $storageObj.find('#dashboard-storage-volume').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $storageObj.find('#dashboard-storage-snapshot').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $storageObj.find('#dashboard-storage-buckets').prepend(
        $('<img>').attr('src','images/dots32.gif'));
    },
  
    _setNetSecSummary : function($netsecObj) {
      var thisObj = this;
      $('html body').eucadata('addCallback', 'balancer', 'dashboard-summary', function(){
        var results = describe('balancer');
        var numBalancers = results ? results.length : 0;
        $netsecObj.find('#dashboard-netsec-load-balancer img').remove();
        $netsecObj.find('#dashboard-netsec-load-balancer span').text(numBalancers);
      });
      $netsecObj.find('#dashboard-netsec-load-balancer').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'balancer'});
          $('html body').trigger('click', 'navigator:balancer');
          return false;
      }));
      $('html body').eucadata('refresh', 'balancer'); 

      $('html body').eucadata('addCallback', 'sgroup', 'dashboard-summary', function(){
        var results = describe('sgroup');
        var numGroups = results ? results.length : 0;
        $netsecObj.find('#dashboard-netsec-sgroup img').remove();
        $netsecObj.find('#dashboard-netsec-sgroup span').text(numGroups);
      });
      $netsecObj.find('#dashboard-netsec-sgroup').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'sgroup'});
          $('html body').trigger('click', 'navigator:sgroup');
          return false;
      }));
      $('html body').eucadata('refresh', 'sgroup'); 

      $('html body').eucadata('addCallback', 'eip', 'dashboard-summary', function(){
        var results = describe('eip');
        var numAddr = results ? results.length : 0;
        $netsecObj.find('#dashboard-netsec-eip img').remove();
        $netsecObj.find('#dashboard-netsec-eip span').text(numAddr);
      });
      $netsecObj.find('#dashboard-netsec-eip').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'eip'});
          $('html body').trigger('click', 'navigator:eip');
          return false;
      }));
      $('html body').eucadata('refresh', 'eip');

      $('html body').eucadata('addCallback', 'keypair', 'dashboard-summary', function(){
        var results = describe('keypair');
        var numKeypair = results ? results.length : 0;
        $netsecObj.find('#dashboard-netsec-keypair img').remove();
        $netsecObj.find('#dashboard-netsec-keypair span').text(numKeypair);
      });
      $netsecObj.find('#dashboard-netsec-keypair').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'keypair'});
          $('html body').trigger('click', 'navigator:keypair');
          return false;
      }));
      $('html body').eucadata('refresh', 'keypair');

      $netsecObj.find('#dashboard-netsec-sgroup').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $netsecObj.find('#dashboard-netsec-eip').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $netsecObj.find('#dashboard-netsec-keypair').prepend(
        $('<img>').attr('src','images/dots32.gif'));
    },

    close: function() {
      $('html body').eucadata('removeCallback', 'instance', 'dashboard-summary');
      $('html body').eucadata('removeCallback', 'volume', 'dashboard-summary');
      $('html body').eucadata('removeCallback', 'snapshot', 'dashboard-summary');
      $('html body').eucadata('removeCallback', 'sgroup', 'dashboard-summary');
      $('html body').eucadata('removeCallback', 'eip', 'dashboard-summary');
      $('html body').eucadata('removeCallback', 'keypair', 'dashboard-summary');
      $('html body').eucadata('removeCallback','zone','dashboard-summary');
      this._super('close');
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
