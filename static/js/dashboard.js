/*************************************************************************
 * Copyright 2011-2012 Eucalyptus Systems, Inc.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
      //$('html body').find(DOM_BINDING['notification']).notification('success', 'dashboard (testing)', 'dashboard loaded successfully');
    },

    _create : function() { },

    _destroy : function() { },
    
    // initiate ajax call-- describe-instances
    // attach spinning wheel until we refresh the content with the ajax response
    _setInstSummary : function($instObj) {
      var thisObj = this;
      var az=$instObj.find('#dashboard-instance-az select').val();
      // TODO: this is probably not the right place to call describe-instances. instances page should receive the data from server
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
            $.each(data.results, function (idx, res){
              $.each(res.instances, function(ix, instance){
                // TODO: check if placement is the right identifier of availability zones
                if (az==='all' || instance.placement === az ){
                  if (instance.state === 'running')
                    numRunning++;
                  else if (instance.state === 'stopped')
                    numStopped++;
                }
              });
            });
            $instObj.find('#dashboard-instance-running img').remove();
            $instObj.find('#dashboard-instance-stopped img').remove();
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
            $instObj.find('#dashboard-instance-img').wrapAll(
              $('<a>').attr('href','#').click( function(evt){
                  thisObj._trigger('select', evt, {selected:'instance'});
                }));
          } else {
            //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve instances due to server failure');
          }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            $('html body').find(DOM_BINDING['notification']).notification('error', 'dashboard', 'can\'t retrieve instances due to server failure');
        }
      });

      //az = $instObj.find('#dashboard-instance-dropbox').value();
      $instObj.find('#dashboard-instance-running').prepend(
        $('<img>').attr('src','images/dots32.gif'));
      $instObj.find('#dashboard-instance-stopped').prepend(
        $('<img>').attr('src','images/dots32.gif'));
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
            
            $storageObj.find('#dashboard-storage-img').wrapAll(
              $('<a>').attr('href','#').click( function(evt){
                  thisObj._trigger('select', evt, {selected:'volume'});
                }));
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

            $netsecObj.find('#dashboard-netsec-img').wrapAll(
              $('<a>').attr('href','#').click( function(evt){
                  thisObj._trigger('select', evt, {selected:'sgroup'});
            }));

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
