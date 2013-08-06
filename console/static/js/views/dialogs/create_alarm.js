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
                timeunit: 'SECS',
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

                comparison: [
                    {value: 'GreaterThanThreshold', label: '>'}, 
                    {value: 'GreaterThanOrEqualToThreshold', label: '> or ='},
                    {value: 'LessThanThreshold', label: '<'}, 
                    {value: 'LessThanOrEqualToThreshold', label: '< or ='}
                ],

                statistic: [
                    {value: 'Average', label: 'Average'},
                    {value: 'Maximum', label: 'Maximum'},
                    {value: 'Minimum', label: 'Minimum'},
                    {value: 'SampleCount', label: 'Sample Count'},
                    {value: 'Sum', label: 'Sum'}
                ],

                metrics: new Backbone.Collection([
                    {id: 'AWS/AutoScaling - Group desired capacity', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupDesiredCapacity'}},
                    {id: 'AWS/AutoScaling - Group in-service instances', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupInServiceInstances'}}, 
                    {id: 'AWS/AutoScaling - Group max size', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupMaxSize'}},
                    {id: 'AWS/AutoScaling - Group min size', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupMinSize'}},
                    {id: 'AWS/AutoScaling - Group pending instances', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupPendingInstances'}},
                    {id: 'AWS/AutoScaling - Group terminated instances', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupTerminatedInstances'}},
                    {id: 'AWS/AutoScaling - Group total instances', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupTotalInstances'}},
                    {id: 'AWS/EBS - Volume idle time', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeIdleTime'}},
                    {id: 'AWS/EBS - Volume queue length', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeQueueLength'}},
                    {id: 'AWS/EBS - Volume read bytes', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeReadBytes'}},
                    {id: 'AWS/EBS - Volume read ops', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeReadOps'}},
                    {id: 'AWS/EBS - Volume total read time', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeTotalReaTime'}},
                    {id: 'AWS/EBS - Volume write ops', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeWriteOps'}},
                    {id: 'AWS/EBS - Volume total write time', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeTotalWriteTime'}},
                    {id: 'AWS/EC2 - CPU utilization', 
                        value: {namespace: 'AWS/EC2', name: 'CPUUtilization'}},
                    {id: 'AWS/EC2 - Disk read bytes', 
                        value: {namespace: 'AWS/EC2', name: 'DiskReadBytes'}},
                    {id: 'AWS/EC2 - Disk read ops', 
                        value: {namespace: 'AWS/EC2', name: 'DiskReadOps'}},
                    {id: 'AWS/EC2 - Disk write bytes', 
                        value: {namespace: 'AWS/EC2', name: 'DiskWriteBytes'}},
                    {id: 'AWS/EC2 - Disk write ops', 
                        value: {namespace: 'AWS/EC2', name: 'DiskWriteOps'}},
                    {id: 'AWS/EC2 - Network in', 
                        value: {namespace: 'AWS/EC2', name: 'NetworkBytesIn'}},
                    {id: 'AWS/EC2 - Network out', 
                        value: {namespace: 'AWS/EC2', name: 'NetworkBytesOut'}},
                    {id: 'AWS/ELB - HTTP code (back end) 2XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_Backend_2XX'}},
                    {id: 'AWS/ELB - HTTP code (back end) 3XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_Backend_3XX'}},
                    {id: 'AWS/ELB - HTTP code (back end) 4XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_Backend_4XX'}},
                    {id: 'AWS/ELB - HTTP code (back end) 5XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_Backend_5XX'}},
                    {id: 'AWS/ELB - HTTP code (LB) 4XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_ELB_4XX'}},
                    {id: 'AWS/ELB - HTTP code (LB) 5XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_ELB_5XX'}},
                    {id: 'AWS/ELB - Latency', 
                        value: {namespace: 'AWS/ELB', name: 'Latency'}},
                    {id: 'AWS/ELB - Request count', 
                        value: {namespace: 'AWS/ELB', name: 'RequestCount'}},
                    {id: 'AWS/ELB - Healthy host count', 
                        value: {namespace: 'AWS/ELB', name: 'HealthyHostCount'}},
                    {id: 'AWS/ELB - Unhealthy host count', 
                        value: {namespace: 'AWS/ELB', name: 'UnhealthyHostCount'}}
                ]),

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
			                      notifyError($.i18n.prop('alarm_create_error'));
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
                    dimension: value.dimension,
                    dimension_value: value.dimension_value
                });
            });

            scope.on('change:timeunit', function(model) {
                if (model.get('timeunit') == 'SECS') {
                    alarm.set('period', Math.round(alarm.get('period') * 60));
                }
                if (model.get('timeunit') == 'MINS') {
                    alarm.set('period', Math.round(alarm.get('period') / 60));
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

            this._do_init();
        },
	});
});
