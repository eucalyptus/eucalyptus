(function($, eucalyptus) {
  $.widget('eucalyptus.dashboard', $.eucalyptus.eucawidget, {
    options : { },

    _init : function() {
      $div = $('html body div.templates').find('#dashboard').clone();       
      var $inst = $div.find('.dashboard.instances');
      $.each(this._getInstances(), function (idx, val) { val.appendTo($inst);});
      var $storage = $div.find('.dashboard.storage');
      $.each(this._getStorage(), function (idx, val) { val.appendTo($storage);});
      var $netsec = $div.find('.dashboard.netsec');
      $.each(this._getNetSec(), function (idx, val) { val.appendTo($netsec);});

      $div.appendTo(this.element); 
    },

    _create : function() { 
    },

    _destroy : function() {
    },
    
    // return jQuery object for instances area
    _getInstances : function() {
      return [
        $('<div>').attr('id', 'dashboard-instance-img').append(
          $('<img>').attr('src','images/dashboard_instance.png')),
        $('<div>').attr('id', 'dashboard-instance-dropbox').append(
          $('<p>').text('In all availability zones')),
        $('<div>').attr('id', 'dashboard-instance-running').append(
          $('<div>').attr('id','dashboard-instance-running-img').append(
            $('<img>').attr('src','images/dashboard_instance_running.png')),
          $('<div>').attr('id','dashboard-instance-running-text').append(
            $('<p>').html('#INST<br> Running'))),
        $('<div>').attr('id', 'dashboard-instance-stopped').append(
          $('<div>').attr('id','dashboard-instance-stopped-img').append(
            $('<img>').attr('src','images/dashboard_instance_stopped.png')),
          $('<div>').attr('id','dashboard-instance-stopped-text').append(
            $('<p>').html('#INST<br> Stopped'))),
        $('<div>').attr('id','dashboard-instance-launch').append(
          $('<a>').attr('id','dashboard-instance-launch-button').attr('href','#').append(
            $('<img>').attr('src','images/dashboard_instance_launch.png')))
      ];
    },

    _getStorage : function() {
      return [
        $('<div>').attr('id','dashboard-storage-img').append(
          $('<img>').attr('src','images/dashboard_storage.png')),
        $('<div>').attr('id','dashboard-storage-volume').append(
          $('<p>').html('#VOL<br> EBS<br> Volumes')),
        $('<div>').attr('id','dashboard-storage-snapshot').append(
          $('<p>').html('#SNAP<br> EBS<br> Snapshots')),
        $('<div>').attr('id','dashboard-storage-buckets').append(
          $('<p>').html('#BUK<br> Buckets'))
      ]; 
    },
  
    _getNetSec : function() {
      return [
        $('<div>').attr('id', 'dashboard-netsec-img').append(
          $('<img>').attr('src','images/dashboard_netsec.png')),
        $('<div>').attr('id','dashboard-netsec-sgroup').append(
          $('<p>').html('#SGRP<br> Security<br> Groups')),
        $('<div>').attr('id','dashboard-netsec-eip').append(
          $('<p>').html('#EIP<br> Elastic IPs')),
        $('<div>').attr('id','dashboard-netsec-keypair').append(
          $('<p>').html('#KP<br> Keypairs'))
      ];
    },

    close: function() {
      this._super('close');
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
