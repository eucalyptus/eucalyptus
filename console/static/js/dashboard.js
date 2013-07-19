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
      this._setZoneSelection($dashboard.find('#dashboard-content .instances'));
      this._setTotals($dashboard.find('#dashboard-content .instances'),
                         $dashboard.find('#dashboard-content .storage'),
                         $dashboard.find('#dashboard-content .netsec'));
      var $wrapper = $('<div>').addClass('dashboard-wrapper');
      $dashboard.appendTo($wrapper);
      $wrapper.appendTo(this.element);
      this._addHelp($help);
      $('html body').eucadata('setDataNeeds', ['dash', 'zones']);
    },

    _create : function() { 
    },

    _destroy : function() { },
    
    // initiate ajax call-- describe-instances
    // attach spinning wheel until we refresh the content with the ajax response
    _setZoneSelection : function($instObj) {
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
        thisObj._resetInstances($instObj);
      }); 
    },

    _resetInstances : function($instObj){
      $instObj.find('#dashboard-instance-running div span').text('');
      $instObj.find('#dashboard-instance-stopped div span').text('');
      $instObj.find('#dashboard-scaling-groups div span').text('');
      $instObj.find('#dashboard-instance-running div').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $instObj.find('#dashboard-instance-stopped div').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $instObj.find('#dashboard-scaling-groups div').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      // set filter on eucadata for summary
      var az=$instObj.find('#dashboard-instance-az select').val();
      require(['app'], function(app) {
        if (az == 'all') {
          app.data.summary.params = undefined;
        } else {
          app.data.summary.params = {zone: az};
        }
      });
    },

    _setTotals : function($instObj, $storageObj, $netsecObj){
      var thisObj = this;
      // set busy indicators while loading
      $instObj.find('#dashboard-instance-running div').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $instObj.find('#dashboard-instance-stopped div').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $instObj.find('#dashboard-scaling-groups div').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $storageObj.find('#dashboard-storage-volume').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $storageObj.find('#dashboard-storage-snapshot').prepend(
        $('<img>').attr('src','images/dots32.gif'));
//      $storageObj.find('#dashboard-storage-buckets').prepend(
//        $('<img>').attr('src','images/dots32.gif'));
//      $netsecObj.find('#dashboard-netsec-load-balancer').prepend(
//        $('<img>').attr('src','images/dots32.gif'));
      $netsecObj.find('#dashboard-netsec-sgroup').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $netsecObj.find('#dashboard-netsec-eip').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $netsecObj.find('#dashboard-netsec-keypair').prepend(
        $('<img>').attr('src','images/dots32.gif'));


      // configure navigation links
      $instObj.find('#dashboard-instance-launch a').click( function(e) {
        var $container = $('html body').find(DOM_BINDING['main']);
        $container.maincontainer("changeSelected", e, {selected:'launcher'});
        $('html body').trigger('click', 'navigator:launcher');
        return false;
      });
      $instObj.find('#dashboard-instance-running').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          var az=$instObj.find('#dashboard-instance-az select').val();
          var filter = {state_filter:'running'};
          if (az != 'all') filter.az_filter = az;
          thisObj._trigger('select', evt, {selected:'instance', filter:filter});
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
            thisObj._trigger('select', evt, {selected:'scaling'});
            $('html body').trigger('click', 'navigator:scaling');
            return false;
      }));
      $storageObj.find('#dashboard-storage-volume').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'volume'});
          $('html body').trigger('click', 'navigator:volume');
          return false;
      }));
      $storageObj.find('#dashboard-storage-snapshot').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'snapshot'});
          $('html body').trigger('click', 'navigator:snapshot');
          return false;
      }));
/*
      $('html body').eucadata('refresh', 'bucket');
      $storageObj.find('#dashboard-storage-buckets').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'bucket'});
          $('html body').trigger('click', 'navigator:bucket');
          return false;
      }));
      $netsecObj.find('#dashboard-netsec-load-balancer').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'balancing'});
          $('html body').trigger('click', 'navigator:balancing');
          return false;
      }));
*/
      $netsecObj.find('#dashboard-netsec-sgroup').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'sgroup'});
          $('html body').trigger('click', 'navigator:sgroup');
          return false;
      }));
      $netsecObj.find('#dashboard-netsec-eip').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'eip'});
          $('html body').trigger('click', 'navigator:eip');
          return false;
      }));
      $netsecObj.find('#dashboard-netsec-keypair').wrapAll(
        $('<a>').attr('href','#').click( function(evt){
          thisObj._trigger('select', evt, {selected:'keypair'});
          $('html body').trigger('click', 'navigator:keypair');
          return false;
      }));
      thisObj._reloadSummaries($instObj, $storageObj, $netsecObj);
    },

    _reloadSummaries : function($instObj, $storageObj, $netsecObj){
      var thisObj = this;
      $('html body').eucadata('addCallback', 'summary', 'dashboard-summary', function(){

        // remove busy indicators when data arrives
        $instObj.find('#dashboard-instance-running div img').remove();
        $instObj.find('#dashboard-instance-stopped div img').remove();
        $instObj.find('#dashboard-scaling-groups div img').remove();
        $storageObj.find('#dashboard-storage-volume img').remove();
        $storageObj.find('#dashboard-storage-snapshot img').remove();
//      $storageObj.find('#dashboard-storage-buckets img').remove();
//      $netsecObj.find('#dashboard-netsec-load-balancer img').remove();
        $netsecObj.find('#dashboard-netsec-sgroup img').remove();
        $netsecObj.find('#dashboard-netsec-eip img').remove();
        $netsecObj.find('#dashboard-netsec-keypair img').remove();

        var az=$instObj.find('#dashboard-instance-az select').val();

        var results = describe('summary')[0];
        inst_running_count = results.inst_running;
	    inst_stopped_count = results.inst_stopped;

        $instObj.find('#dashboard-instance-running span').text(inst_running_count);
        $instObj.find('#dashboard-instance-stopped span').text(inst_stopped_count);
        $instObj.find('#dashboard-scaling-groups span').text(results.scalinginst);
        $storageObj.find('#dashboard-storage-volume span').text(results.volume);
        $storageObj.find('#dashboard-storage-snapshot span').text(results.snapshot);
//      $storageObj.find('#dashboard-storage-buckets span').text(0);
//      $netsecObj.find('#dashboard-netsec-load-balancer span').text(0);
        $netsecObj.find('#dashboard-netsec-sgroup span').text(results.sgroup);
        $netsecObj.find('#dashboard-netsec-eip span').text(results.eip);
        $netsecObj.find('#dashboard-netsec-keypair span').text(results.keypair);
	  });
 
      $('html body').eucadata('refresh','summary');// pass zone?
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

    close: function() {
      $('html body').eucadata('removeCallback', 'zone','dashboard-summary');
      $('html body').eucadata('removeCallback', 'summary', 'dashboard-summary');
      this._super('close');
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
