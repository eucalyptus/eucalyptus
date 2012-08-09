(function($, eucalyptus) {
  $.widget('eucalyptus.dashboard', $.eucalyptus.eucawidget, {
    options : { },

    _init : function() {
      var $tmpl = $('html body div.templates').find('#dashboardTmpl').clone();       
      var $div = $($tmpl.render($.i18n.map));

      this._setInstSummary($div.find('.instances'));
      this._setStorageSummary($div.find('.storage'));
      this._setNetSecSummary($div.find('.netsec'));  
      $div.appendTo(this.element); 
    },

    _create : function() { 
    },

    _destroy : function() {
    },
    
    // initiate ajax call-- describe-instances
    // attach spinning wheel until we refresh the content with the ajax response
    _setInstSummary : function($instObj) {
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
          } else {
            //TODO: need to call notification subsystem
            alert('can\'t describe instances');
          }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            alert('can\'t describe instances');
        }
      });

      //az = $instObj.find('#dashboard-instance-dropbox').value();
      $instObj.find('#dashboard-instance-running').prepend(
        $('<img>').attr('src','images/loading.gif'));
      $instObj.find('#dashboard-instance-stopped').prepend(
        $('<img>').attr('src','images/loading.gif'));
    },

    _setStorageSummary : function($storageObj) {
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
          } else {
            //TODO: need to call notification subsystem
            alert('can\'t describe volumes');
          }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            alert('can\'t describe volumes');
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
          } else {
            //TODO: need to call notification subsystem
            alert('can\'t describe snapshots');
          }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            alert('can\'t describe snapshots');
        }
      });

      //az = $instObj.find('#dashboard-instance-dropbox').value();
      $storageObj.find('#dashboard-storage-volume').prepend(
        $('<img>').attr('src','images/loading.gif'));
      $storageObj.find('#dashboard-storage-snapshot').prepend(
        $('<img>').attr('src','images/loading.gif'));
      $storageObj.find('#dashboard-storage-buckets').prepend(
        $('<img>').attr('src','images/loading.gif'));
    },
  
    _setNetSecSummary : function($netsecObj) {
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
          } else {
            //TODO: need to call notification subsystem
            alert('can\'t describe security groups');
          }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            alert('can\'t describe security groups');
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
          } else {
            //TODO: need to call notification subsystem
            alert('can\'t describe addresses');
          } 
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            alert('can\'t describe addresses');
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
          } else {
            //TODO: need to call notification subsystem
            alert('can\'t describe keypair');
          }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
            alert('can\'t describe keypair');
        }
      });

      //az = $instObj.find('#dashboard-instance-dropbox').value();
      $netsecObj.find('#dashboard-netsec-sgroup').prepend(
        $('<img>').attr('src','images/loading.gif'));
      $netsecObj.find('#dashboard-netsec-eip').prepend(
        $('<img>').attr('src','images/loading.gif'));
      $netsecObj.find('#dashboard-netsec-keypair').prepend(
        $('<img>').attr('src','images/loading.gif'));
    },

    close: function() {
      this._super('close');
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
