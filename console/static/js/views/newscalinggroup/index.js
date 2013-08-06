define([
  'app',
  'underscore',
  'backbone',
  'wizard',
  'text!./template.html',
  './page1',
  './page2',
  './page3',
  'models/scalinggrp',
  'models/scalingpolicy',
  './summary',
], function(app, _, Backbone, Wizard, wizardTemplate, page1, page2, page3, ScalingGroup, ScalingPolicy, summary) {
  var config = function(options) {
      var wizard = new Wizard();

      var scope = new Backbone.Model({
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
                show_lc_selector: options.launchconfig ? false : true,
                health_check_type: 'EC2'
        }),
        change: function(e) {
            setTimeout(function() { $(e.target).change(); }, 0);
        }
      });

      function setPolicies(sg_name) {
        var myscope = scope;
        scope.get('policies').each( function(model, index) {
          var policy = new ScalingPolicy(model.toJSON());
          policy.set('as_name', sg_name);
          policy.save({}, {
            success: function(model, response, options){  
              if(model != null){
                var name = model.get('name');
                notifySuccess(null, $.i18n.prop('create_scaling_group_policy_run_success', name, sg_name)); 
                setAlarms(model); 
              }else{
                notifyError($.i18n.prop('create_scaling_group_policy_run_error'), undefined_error);
              }
            },
            error: function(model, jqXHR, options){  
              notifyError($.i18n.prop('create_scaling_group_policy run_error'), getErrorMessage(jqXHR));
            }
          });
        });
      }

      function setAlarms(model) {
        var arn = model.get('PolicyARN');
        if(alarm = scope.get('alarms').findWhere({name: model.get('alarm')})) {
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

      function canFinish(position, problems) {
        // VALIDATE THE MODEL HERE AND IF THERE ARE PROBLEMS,
        // ADD THEM INTO THE PASSED ARRAY
        return scope.get('scalingGroup').isValid() & position === 2;
      }

      function finish() {
          console.log('CREATING SCALING GROUP!');
          scope.get('scalingGroup').save({}, {
            success: function(model, response, options){  
              if(model != null){
                var name = model.get('name');
                notifySuccess(null, $.i18n.prop('create_scaling_group_run_success', name));  
                setPolicies(name);
              }else{
                notifyError($.i18n.prop('create_scaling_group_run_error'), undefined_error);
              }
            },
            error: function(model, jqXHR, options){  
              notifyError($.i18n.prop('create_scaling_group_run_error'), getErrorMessage(jqXHR));
            } 
          });
          window.location = '#scaling';
      }

      // Sync changes to the availability zones collection into the scaling group
      scope.get('availabilityZones').on('add remove', function() {
        scope.get('scalingGroup').set('availability_zones', 
            scope.get('availabilityZones').pluck('name'));

        var az = scope.get('scalingGroup').get('availability_zones');
        if(Array.isArray(az) && az.length == 0) {
          scope.get('scalingGroup').unset('availability_zones');
        }
      });

      // Sync changes to the load balancers collection into the scaling group
      scope.get('loadBalancers').on('add remove', function() {
        scope.get('scalingGroup').set('load_balancers', 
            scope.get('loadBalancers').pluck('name'));
      });

      // make the selected launch config model available for the summary
      scope.get('scalingGroup').on('change:launch_config_name', function() {
        var sg = scope.get('scalingGroup');
        sg.set('launchConfig', app.data.launchconfig.findWhere({name: sg.get('launch_config_name')}));
      });
      
      var p1 = new page1({model: scope});
      var p2 = new page2({model: scope});
      var p3 = new page3({model: scope});

      var viewBuilder = wizard.viewBuilder(wizardTemplate)
              .add(p1).add(p2).add(p3).setHideDisabledButtons(true)
              .setFinishText('Create scaling group').setFinishChecker(canFinish)
              .finisher(finish)
              .summary(new summary( {model: scope} ));

      var ViewType = viewBuilder.build()
      return ViewType;
  }
  return config;
});
