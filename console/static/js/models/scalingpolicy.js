// scaling policy model
//

define([
  './eucamodel'
], function(EucaModel) {
  var model = EucaModel.extend({
    idAttribute: 'name'
    sync: function(method, model, options){
      if(method == 'create'){
        var url = "/autoscale?Action=PutScalingPolicy";
        var name = model.get('name');
        var adustment_type = model.get('adjustment_type');
        var as_name = model.get('as_name');
        var scaling_adjustment = model.get('scaling_adjustment');
        var cooldown = model.get('cooldown');
        var parameter = "_xsrf="+$.cookie('_xsrf');
        parameter += "&PolicyName="+name+"&AdjustmentType="+adjustment_type+"&AutoScalingGroupName="+as_name+"&ScalingAdjustment="+scaling_adjustment;
        if(cooldown != undefined){
          parameter += "&Cooldown="+cooldown;
        }
        return this.makeAjaxCall(url, parameter, options);
      }else if(method == 'delete'){
        var url = "/autoscale?Action=DeletePolicy";
        var name = model.get('name');
        var parameter = "_xsrf="+$.cookie('_xsrf');
        parameter += "&AutoScalingGroupName="+name;
        return this.makeAjaxCall(url, parameter, options);
      }
    },
    execute: function(as_group, honor_cooldown, options){
      var url = "/autoscale?Action=ExecutePolicy";
      var parameter = "_xsrf="+$.cookie('_xsrf');
      if(as_group != undefined){
        parameter += "&AutoScalingGropuName="+as_group;
      }
      if(honor_cooldown != undefined){
        parameter += "&HonorCooldown="+honor_cooldown;
      }
      return this.makeAjaxCall(url, parameter, options);
    },
    // not sure if this returns things in a useful way. Might be nice to return a string array
    getAdjustmentTypes: function(options){
      var url = "/autoscale?Action=DescribeAdjustmentTypes";
      var parameter = "_xsrf="+$.cookie('_xsrf');
      return this.makeAjaxCall(url, parameter, options);
    }
  });
  return model;
});
