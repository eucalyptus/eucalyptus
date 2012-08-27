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
      var $div = $($tmpl.render($.i18n.map));

      this._setInstSummary($div.find('#dashboard-content .instances'));
      this._setStorageSummary($div.find('#dashboard-content .storage'));
      this._setNetSecSummary($div.find('#dashboard-content .netsec'));  
      $div.appendTo(this.element); 
    },

    _create : function() { },

    _destroy : function() { },
    
    // initiate ajax call-- describe-instances
    // attach spinning wheel until we refresh the content with the ajax response
    _setInstSummary : function($instObj) {
      var thisObj = this;
      // retrieve az 
      $.ajax({
        type:"GET",
        url:"/ec2?Action=DescribeAvailabilityZones",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:false,
        success: function(data, textStatus, jqXHR){
          if ( data.results ) {
            var $az=$instObj.find('#dashboard-instance-az select');
            for( res in data.results) {
              azName = data.results[res].name;
              $az.append($('<option>').attr('value', azName).text(azName));
            }
            // update the display
            $az.change( function (e) {
              thisObj._reloadInstSummary($instObj);
            }); 
          } else {
            ;
          }
        },
        error: function(jqXHR, textStatus, errorThrown){
          $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve zones due to server failure');
        }
      });

      // TODO: this is probably not the right place to call describe-instances. instances page should receive the data from server
      // selector is different for these two because of extra div
      $instObj.find('#dashboard-instance-running div').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $instObj.find('#dashboard-instance-stopped div').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      thisObj._reloadInstSummary($instObj);
    },

    _reloadInstSummary : function($instObj){
      $.ajax({
        type:"GET",
        url:"/ec2?Action=DescribeInstances",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:"false",
        success: function(data, textStatus, jqXHR){
          if (data.results) {
            var numRunning = 0;
            var numStopped = 0;
            var az=$instObj.find('#dashboard-instance-az select').val();
            $.each(data.results, function (idx, instance){
                // TODO: check if placement is the right identifier of availability zones
                if (az==='all' || instance.placement === az ){
                  if (instance.state === 'running')
                    numRunning++;
                  else if (instance.state === 'stopped')
                    numStopped++;
                }
             // });
            });
            // selector is different for these two because of extra div
            $instObj.find('#dashboard-instance-running div img').remove();
            $instObj.find('#dashboard-instance-stopped div img').remove();
            $instObj.find('#dashboard-instance-running span').text(numRunning);
            $instObj.find('#dashboard-instance-stopped span').text(numStopped);
            $instObj.find('#dashboard-instance-running').wrapAll(
              $('<a>').attr('href','#').click( function(evt){
                  thisObj._trigger('select', evt, {selected:'instance'});
                }));
            $instObj.find('#dashboard-instance-stopped').wrapAll(
              $('<a>').attr('href','#').click( function(evt){
                  thisObj._trigger('select', evt, {selected:'instance'});
                }));
       /*     $instObj.find('#dashboard-instance-img').wrapAll(
              $('<a>').attr('href','#').click( function(evt){
                  thisObj._trigger('select', evt, {selected:'instance'});
                }));*/
          } else {
            //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve instances due to server failure');
          }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve instances due to server failure');
        }
      });
    },

    _setStorageSummary : function($storageObj) {
      var thisObj = this;
     // TODO: this is probably not the right place to call describe-volumes
      $.ajax({
        type:"GET",
        url:"/ec2?Action=DescribeVolumes",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:"false",
        success: function(data, textStatus, jqXHR){
          if (data.results) {
            var numVol = data.results.length;
            $storageObj.find('#dashboard-storage-volume img').remove();
            $storageObj.find('#dashboard-storage-volume span').text(numVol);
            
            $storageObj.find('#dashboard-storage-volume').wrapAll(
              $('<a>').attr('href','#').click( function(evt){
                  thisObj._trigger('select', evt, {selected:'volume'});
              }));
          } else {
            //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve volumes due to server failure');

          }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve volumes due to server failure');
        }
      });

     // TODO: this is probably not the right place to call describe-snapshot. 
      $.ajax({
        type:"GET",
        url:"/ec2?Action=DescribeSnapshots",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:"false",
        success: function(data, textStatus, jqXHR){
          if (data.results) {
            var numSnapshots = data.results.length;
            $storageObj.find('#dashboard-storage-snapshot img').remove();
            $storageObj.find('#dashboard-storage-snapshot span').text(numSnapshots);
 
            $storageObj.find('#dashboard-storage-snapshot').wrapAll(
              $('<a>').attr('href','#').click( function(evt){
                  thisObj._trigger('select', evt, {selected:'snapshot'});
              }));

          } else {
            //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve snapshots due to server failure');
          }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve snapshots due to server failure');
        }
      });

      //$('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve buckets due to server failure');


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
      $.ajax({
        type:"GET",
        url:"/ec2?Action=DescribeSecurityGroups",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:"false",
        success: function(data, textStatus, jqXHR){
          if (data.results) {
            var numGroups = data.results.length;
            $netsecObj.find('#dashboard-netsec-sgroup img').remove();
            $netsecObj.find('#dashboard-netsec-sgroup span').text(numGroups);

            $netsecObj.find('#dashboard-netsec-sgroup').wrapAll(
              $('<a>').attr('href','#').click( function(evt){
                  thisObj._trigger('select', evt, {selected:'sgroup'});
            }));
          } else {
            //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve security groups due to server failure');
          }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
          $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve security groups due to server failure');
        }
      });

      $.ajax({
        type:"GET",
        url:"/ec2?Action=DescribeAddresses",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:"false",
        success: function(data, textStatus, jqXHR){
          if (data.results) {
            var numAddr = data.results.length;
            $netsecObj.find('#dashboard-netsec-eip img').remove();
            $netsecObj.find('#dashboard-netsec-eip span').text(numAddr);

            $netsecObj.find('#dashboard-netsec-eip').wrapAll(
              $('<a>').attr('href','#').click( function(evt){
                  thisObj._trigger('select', evt, {selected:'eip'});
            }));
          } else {
            //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve addresses due to server failure');
          } 
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve addresses due to server failure');
        } 
      });

      $.ajax({
        type:"GET",
        url:"/ec2?type=key&Action=DescribeKeyPairs",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:"false",
        success: function(data, textStatus, jqXHR){
          if (data.results) {
            var numKeypair = data.results.length;
            $netsecObj.find('#dashboard-netsec-keypair img').remove();
            $netsecObj.find('#dashboard-netsec-keypair span').text(numKeypair);
            $netsecObj.find('#dashboard-netsec-keypair').wrapAll(
              $('<a>').attr('href','#').click( function(evt){
                  thisObj._trigger('select', evt, {selected:'keypair'});
            }));
          } else {
            //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve key pairs due to server failure');
          }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve key pairs due to server failure');
        }
      });

      //az = $instObj.find('#dashboard-instance-dropbox').value();
      $netsecObj.find('#dashboard-netsec-sgroup').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $netsecObj.find('#dashboard-netsec-eip').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $netsecObj.find('#dashboard-netsec-keypair').prepend(
        $('<img>').attr('src','images/dots32.gif'));
    },

    close: function() {
      this._super('close');
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
