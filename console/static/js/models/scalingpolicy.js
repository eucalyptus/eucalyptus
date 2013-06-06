// scaling policy model
//

define([
  './eucamodel',
], function(EucaModel, app) {
  var model = EucaModel.extend({
    idAttribute: 'name',
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
    },
    get: function(attribute) {
      if(attribute == 'action'){
        if (this.attributes['adjustment_type'] == 'ExactCapacity') {
          return $.i18n.prop('create_scaling_group_policy_action_set_size');
        }
        else if (new String(this.attributes['scaling_adjustment']).charAt(0) == '-') {
          return $.i18n.prop('create_scaling_group_policy_action_scale_down');
        }
        else {
          return $.i18n.prop('create_scaling_group_policy_action_scale_up');
        }
      }
      else if(attribute == 'amount'){
        return Math.abs(parseInt(this.attributes['scaling_adjustment']));
      }
      else if(attribute == 'measure'){
        if (this.attributes['adjustment_type'] == 'PercentChangeInCapacity') {
          return $.i18n.prop('create_scaling_group_policy_measure_percent');
        }
        else {
          return $.i18n.prop('create_scaling_group_policy_measure_instance');
        }
      }
      else {
        return this.attributes[attribute];
      }
    }
  });
  return model;
});
