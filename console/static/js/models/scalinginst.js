// scalinginst model
//

define([
    './eucamodel'
], function(EucaModel) {
  var model = EucaModel.extend({
    setInstanceHealth: function (instance_id, health_status, should_respect_grace_period) {
      var data = "_xsrf="+$.cookie('_xsrf')+"&InstanceId="+instance_id+
                 "&AutoScalingGroupName="+this.get('name')+
                 "&DesiredCapacity="+desired_capacity;
      if (honor_cooldown != undefined)
        data += "&HonorCooldown=true";
      $.ajax({
        type:"POST",
        url:"/autoscaling?Action=SetInstanceHealth",
        data:data,
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
          },
        error:
          function(jqXHR, textStatus, errorThrown){
          },
      });
    },
    terminateInstance: function (instance_id, decrement_capacity) {
      var data = "_xsrf="+$.cookie('_xsrf')+"&InstanceId="+instance_id+
                 "&AutoScalingGroupName="+this.get('name')+
                 "&DesiredCapacity="+desired_capacity;
      if (honor_cooldown != undefined)
        data += "&HonorCooldown=true";
      $.ajax({
        type:"POST",
        url:"/autoscaling?Action=SetInstanceHealth",
        data:data,
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
          },
        error:
          function(jqXHR, textStatus, errorThrown){
          },
      });
    }
  });
  return model;
});
