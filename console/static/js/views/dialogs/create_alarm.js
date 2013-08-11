define([
   'underscore',
   'backbone',
   'app',
   './eucadialogview',
   'models/alarm',
   'text!./create_alarm.html!strip',
], function(_, Backbone, app, EucaDialogView, Alarm, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;

            var instanceTypes = new Backbone.Collection();
            _.each($.eucaData.g_session['instance_type'], function(val, key) {
                instanceTypes.add({name: key, cpu: val[0], ram: val[1], disk: val[2]});
            });

            var alarm = new Alarm();
            var error = new Backbone.Model();
            var scope = this.scope = new Backbone.Model({
                help: {title: null, content: help_alarm.dialog_content, url: help_alarm.dialog_content_url, pop_height: 600},

                title: null,
                selectedMetric: '',
                alarm: alarm,
                status: '',
                items: args.items, 
                scalingGroup: args.model.scalingGroup,
                instanceTypes: instanceTypes,
                error: error,

                availabilityZones: app.data.availabilityzone,
                loadBalancers: app.data.loadbalancer,

                scalingGroupAutoComplete: new Backbone.Model({
                    inputId: 'scalingGroupId',
                    available: app.data.scalinggroup
                }),

                volumeAutoComplete: new Backbone.Model({
                    inputId: 'volumeId',
                    available: app.data.volume
                }),

                imageAutoComplete: new Backbone.Model({
                    inputId: 'imageId',
                    available: app.data.image
                }),

                instanceAutoComplete: new Backbone.Model({
                    inputId: 'instanceId',
                    available: app.data.instance
                }),

                comparison: alarm.COMPARISON,

                statistic: alarm.STATISTIC,

                metrics: alarm.METRICS,

                cancelButton: new Backbone.Model({
                    id: 'button-dialog-createalarm-cancel',
                    click: function() {
                       self.close();
                    }
                }),

                submitButton: new Backbone.Model({
                  id: 'button-dialog-createalarm-save',
                  click: function() {
                      alarm.validate();
                      if (alarm.isValid()) {
                          alarm.save(null, {
                              success: function(model, response, options) {
			                      notifySuccess(null, $.i18n.prop('alarm_create_success', model.get('name')));
                              },
                              error: function(model, xhr, options) {
			                      notifyError($.i18n.prop('alarm_create_error') + ': ' + getErrorMessage(xhr));
                              }
                          });
                          self.close();
                      }
                  }
                }),

                createMetric: function() {
                    var newMetric = new Backbone.Model();
                    app.dialog('create_metric', {metric: newMetric});
                    newMetric.on('submit', function() {
                        console.log('NEW METRIC', newMetric);

                        scope.get('metrics').add({
                            id: newMetric.get('namespace') + '/' + newMetric.get('name') + 
                                ' - Custom Metric',
                            value: {
                                namespace: newMetric.get('namespace'), 
                                name: newMetric.get('name'), 
                                dimension: newMetric.get('dimensionKey'), 
                                dimension_value: newMetric.get('dimensionValue')
                            }
                        });

                        scope.set('selectedMetric', newMetric.get('namespace') + '/' + 
                            newMetric.get('name') + ' - Custom Metric');
                        /*
                        alarm.set({
                            namespace: newMetric.get('namespace'),
                            metric: newMetric.get('name'),
                            dimension: newMetric.get('dimensionKey'),
                            dimension_value: newMetric.get('dimensionValue')
                        });
                        */
                        self.render();
                    });
                },

                emptyNamespace: function() {
                    return alarm.get('namespace') == '';
                },

                emptyDimension: function() {
                    return alarm.get('dimension') == '';
                }
            });
            this.scope = scope;

            scope.get('metrics').on('add', function() {
                scope.get('metrics').trigger('change');
                console.log('CHANGE ', scope.get('metrics')); 
            });

            scope.get('scalingGroupAutoComplete').on('change:value', function() {
                alarm.set('dimension_value', scope.get('scalingGroupAutoComplete').get('value'));
            });

            scope.get('volumeAutoComplete').on('change:value', function() {
                alarm.set('dimension_value', scope.get('volumeAutoComplete').get('value'));
            });

            scope.get('imageAutoComplete').on('change:value', function() {
                alarm.set('dimension_value', scope.get('imageAutoComplete').get('value'));
            });

            scope.get('instanceAutoComplete').on('change:value', function() {
                alarm.set('dimension_value', scope.get('instanceAutoComplete').get('value'));
            });

            scope.on('change:selectedMetric', function() {
                var metric = scope.get('metrics').get(scope.get('selectedMetric'));
                var value = metric.get('value');
                alarm.set({
                    namespace: value.namespace,
                    metric: value.name,
                    //dimension: value.dimension, undefined!
                    //dimension_value: value.dimension_value  undefined!
                });

                switch(value.namespace) {
                  case 'AWS/AutoScaling':
                    alarm.set('dimension', 'ThisScalingGroupName');
                    break;
                  case 'AWS/EBS':
                    alarm.set('dimension', 'VolumeId');
                    break;
                  case 'AWS/EC2':
                    alarm.set('dimension', 'ThisScalingGroupName');
                    break;
                  case 'AWS/ELB':
                    alarm.set('dimension', 'AvailabilityZone');
                    break;
                }
            });

            alarm.on('change', function(model) {
                alarm.validate(model.changed);
            });

            alarm.on('validated', function(valid, model, errors) {
                _.each(_.keys(model.changed), function(key) { 
                    error.set(key, errors[key]); 
                });
                self.scope.get('submitButton').set('disabled', !alarm.isValid());
            });

            alarm.on('change:dimension', function() {
                if (scope.get('alarm').get('dimension') == 'ThisScalingGroupName')
                    scope.get('alarm').set('dimension_value', scope.get('scalingGroup').get('name'));
            });

            //set defaults
            if(!scope.get('selectedMetric') && scope.get('metrics') && scope.get('metrics').length > 0) {
              scope.set('selectedMetric', scope.get('metrics').at(0).get('id'));
            }
            if(!scope.get('alarm').get('statistic') && scope.get('statistic') && scope.get('statistic').length > 0) {
              scope.get('alarm').set('statistic', scope.get('statistic')[0].value);
            }
            if(!scope.get('alarm').get('comparison') && scope.get('comparison') && scope.get('comparison').length > 0) {
              scope.get('alarm').set('comparison', scope.get('comparison')[0].value);
            }


            this._do_init();

            self.scope.get('alarm').set('dimension', $('#alarm-dimension-0 option').first().val());
        },
	});
});
