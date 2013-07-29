define([
  './eucadialogview',
  'app',
  'models/scalinggrp',
  'models/scalingpolicy',
  'models/alarm',
  'views/newscalinggroup/page1',
  'views/newscalinggroup/page2',
  'views/newscalinggroup/page3',
  'text!./scalinggroupeditproperties.html'
], function(EucaDialog, app, ScalingGroup, ScalingPolicy, Alarm, tab1,tab2,tab3, tpl) {
  return EucaDialog.extend({
    initialize: function(options) {
      var self = this;
      this.template = tpl;

      this.scope = new Backbone.Model({
        cancelButton: {
          id: 'button-dialog-editscalinggroup-cancel',
          click: function() {
            self.close();
          }
        },

        createButton: {
          id: 'button-dialog-editscalinggroup-save',
          click: function() {
            self.save();
            self.close();
          }
        },

        availabilityZones: new Backbone.Collection(),
        loadBalancers: new Backbone.Collection(),
        alarms: new Backbone.Collection(),
        policies: new Backbone.Collection(),
        toggletest: new Backbone.Model({value: false}),
        scalingGroup: new ScalingGroup({
                min_size: 0,
                desired_capacity: 0,
                max_size: 0,
                launch_config_name: options.launchconfig ? options.launchconfig : null,
                show_lc_selector: options.launchconfig ? false : true
        }),
        change: function(e) {
            setTimeout(function() { $(e.target).change(); }, 0);
        }
      });

      //init from options
      if(options && options.model && options.model.length > 0) {
        var sg = options.model.at(0);
        this.scope.set('scalingGroup', sg.clone());
        
        if(sg.get('availability_zones') && sg.get('availability_zones').length > 0) {
          _.each(sg.get('availability_zones'), function(az) {
            self.scope.get('availabilityZones').add( app.data.zones.findWhere({name: az}).clone() );
          });
        }
        
        if(sg.get('load_balancers') && sg.get('load_balancers').length > 0) {
          _.each(sg.get('load_balancers'), function(lb) {
            self.scope.get('loadBalancers').add( app.data.loadbalancer.findWhere({name: lb}).clone() );
          });
        }
        
        _.each(app.data.scalingpolicy.where({as_name: sg.get('name')}), function(sp) {
          self.scope.get('policies').add( sp.clone() );
        });

        if(this.scope.get('policies')) {
          this.scope.get('policies').each( function(pol) {
            _.each(pol.get('alarms'), function(al) {
              var almodel = new Alarm(al);
              self.scope.get('alarms').add(app.data.alarms.findWhere({alarm_arn: almodel.get('alarm_arn')}));
              pol.set('alarm_model', almodel.clone());
              pol.set('alarm', almodel.get('name'));
            });
          });
        }
      }



      var t1 = new tab1({model:this.scope, bind: false});
      var t2 = new tab2({model:this.scope});
      var t3 = new tab3({model:this.scope});

      this._do_init( function(view) {
        setTimeout( function() {
          view.$el.find('#tabs-1').append(t1.render().el);
          view.$el.find('#tabs-2').append(t2.render().el);
          view.$el.find('#tabs-3').append(t3.render().el);
        }, 1000);
      });
    },

    save: function() {
      var self = this;
      self.scope.get('scalingGroup').save({}, {
        success: function(model, response, options){  
          if(model != null){
            var name = model.get('name');
            notifySuccess(null, $.i18n.prop('create_scaling_group_run_success', name));  
            self.setPolicies(name);
          }else{
            notifyError($.i18n.prop('create_scaling_group_run_error'), undefined_error);
          }
        },
        error: function(model, jqXHR, options){  
          notifyError($.i18n.prop('create_scaling_group_run_error'), getErrorMessage(jqXHR));
        } 
      });
    },

    setPolicies: function(sg_name) {
      var self = this;
      self.scope.get('policies').each( function(model, index) {
        var policy = new ScalingPolicy(model.toJSON());
        policy.set('as_name', sg_name);
        policy.save({}, {
          success: function(model, response, options){  
            if(model != null){
              var name = model.get('name');
              notifySuccess(null, $.i18n.prop('create_scaling_group_policy_run_success', name, sg_name)); 
              self.setAlarms(model); 
            }else{
              notifyError($.i18n.prop('create_scaling_group_policy_run_error'), undefined_error);
            }
          },
          error: function(model, jqXHR, options){  
            notifyError($.i18n.prop('create_scaling_group_policy run_error'), getErrorMessage(jqXHR));
          }
        });
      });
    },

    setAlarms: function(model) {
      var self = this;
      var arn = model.get('PolicyARN');
      if(alarm = self.scope.get('alarms').findWhere({name: model.get('alarm')})) {
        var actions = alarm.get('alarm_actions') ? alarm.get('alarm_actions') : new Array();
        actions.push(arn);
        alarm.set('alarm_actions', actions);
        alarm.save({}, {
          success: function(model, response, options){  
            if(model != null){
              var name = model.get('name');
              notifySuccess(null, $.i18n.prop('create_scaling_group_policy_alarm_run_success', name, arn)); 
            }else{
              notifyError($.i18n.prop('create_scaling_group_policy_alarm_run_error'), undefined_error);
            }
          },
          error: function(model, jqXHR, options){  
            notifyError($.i18n.prop('create_scaling_group_policy_alarm_run_error'), getErrorMessage(jqXHR));
          }
        });
      }
    }

  });
});
