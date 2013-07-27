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
            var scope = new Backbone.Model({
                title: null,
                selectedMetric: '',
                timeunit: 'SECS',
                alarm: alarm,
                status: '',
                items: args.items, 
                scalingGroup: args.model.scalingGroup,
                instanceTypes: instanceTypes,

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
                    {label: 'AWS/AutoScaling - Group desired capacity', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupDesiredCapacity'}},
                    {label: 'AWS/AutoScaling - Group in-service instances', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupInServiceInstances'}}, 
                    {label: 'AWS/AutoScaling - Group max size', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupMaxSize'}},
                    {label: 'AWS/AutoScaling - Group min size', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupMinSize'}},
                    {label: 'AWS/AutoScaling - Group pending instances', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupPendingInstances'}},
                    {label: 'AWS/AutoScaling - Group terminated instances', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupTerminatedInstances'}},
                    {label: 'AWS/AutoScaling - Group total instances', 
                        value: {namespace: 'AWS/AutoScaling', name: 'GroupTotalInstances'}},
                    {label: 'AWS/EBS - Volume idle time', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeIdleTime'}},
                    {label: 'AWS/EBS - Volume queue length', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeQueueLength'}},
                    {label: 'AWS/EBS - Volume read bytes', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeReadBytes'}},
                    {label: 'AWS/EBS - Volume read ops', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeReadOps'}},
                    {label: 'AWS/EBS - Volume total read time', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeTotalReaTime'}},
                    {label: 'AWS/EBS - Volume write ops', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeWriteOps'}},
                    {label: 'AWS/EBS - Volume total write time', 
                        value: {namespace: 'AWS/EBS', name: 'VolumeTotalWriteTime'}},
                    {label: 'AWS/EC2 - CPU utilization', 
                        value: {namespace: 'AWS/EC2', name: 'CPUUtilization'}},
                    {label: 'AWS/EC2 - Disk read bytes', 
                        value: {namespace: 'AWS/EC2', name: 'DiskReadBytes'}},
                    {label: 'AWS/EC2 - Disk read ops', 
                        value: {namespace: 'AWS/EC2', name: 'DiskReadOps'}},
                    {label: 'AWS/EC2 - Disk write bytes', 
                        value: {namespace: 'AWS/EC2', name: 'DiskWriteBytes'}},
                    {label: 'AWS/EC2 - Disk write ops', 
                        value: {namespace: 'AWS/EC2', name: 'DiskWriteOps'}},
                    {label: 'AWS/EC2 - Network in', 
                        value: {namespace: 'AWS/EC2', name: 'NetworkBytesIn'}},
                    {label: 'AWS/EC2 - Network out', 
                        value: {namespace: 'AWS/EC2', name: 'NetworkBytesOut'}},
                    {label: 'AWS/ELB - HTTP code (back end) 2XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_Backend_2XX'}},
                    {label: 'AWS/ELB - HTTP code (back end) 3XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_Backend_3XX'}},
                    {label: 'AWS/ELB - HTTP code (back end) 4XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_Backend_4XX'}},
                    {label: 'AWS/ELB - HTTP code (back end) 5XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_Backend_5XX'}},
                    {label: 'AWS/ELB - HTTP code (LB) 4XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_ELB_4XX'}},
                    {label: 'AWS/ELB - HTTP code (LB) 5XX', 
                        value: {namespace: 'AWS/ELB', name: 'HTTPCode_ELB_5XX'}},
                    {label: 'AWS/ELB - Latency', 
                        value: {namespace: 'AWS/ELB', name: 'Latency'}},
                    {label: 'AWS/ELB - Request count', 
                        value: {namespace: 'AWS/ELB', name: 'RequestCount'}},
                    {label: 'AWS/ELB - Healthy host count', 
                        value: {namespace: 'AWS/ELB', name: 'HealthyHostCount'}},
                    {label: 'AWS/ELB - Unhealthy host count', 
                        value: {namespace: 'AWS/ELB', name: 'UnhealthyHostCount'}}
                ]),

                cancelButton: {
                    id: 'button-dialog-createalarm-cancel',
                    click: function() {
                       self.close();
                    }
                },

                submitButton: {
                  id: 'button-dialog-createalarm-save',
                  click: function() {
                      console.log('Time to create the alarm!');
                      alarm.save(null, {
                          success: function(model, response, options) {
                              console.log('success', arguments);
                          },
                          error: function(model, xhr, options) {
                              console.log('error', arguments);
                          }
                      });
                      self.close();
                  }
                },

                createMetric: function() {
                    app.dialog('create_metric');
                }
            });
            this.scope = scope;

            scope.on('change:selectedMetric', function() {
                var metric = scope.get('metrics').findWhere({ label: scope.get('selectedMetric') });
                alarm.set({
                    Namespace: metric.get('value').namespace,
                    MetricName: metric.get('value').name,
                    Dimension: null,
                    DimensionValue: null
                });
            });

            scope.on('change:timeunit', function(model) {
                if (model.get('timeunit') == 'SECS') {
                    alarm.set('Period', Math.round(alarm.get('Period') * 60));
                }
                if (model.get('timeunit') == 'MINS') {
                    alarm.set('Period', Math.round(alarm.get('Period') / 60));
                }
            });

            this._do_init();
        },
	});
});
