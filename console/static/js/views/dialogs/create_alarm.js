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

            var alarm = new Alarm();
            var scope = new Backbone.Model({
                selectedMetric: '',
                alarm: alarm,
                status: '',
                items: args.items, 

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
                    {label: 'AWS/AutoScaling - Group desired capacity', value: {namespace: 'AWS/AutoScaling', name: 'Group desired capacity'}},
                    {label: 'AWS/AutoScaling - Group in-service instances', value: {namespace: 'AWS/AutoScaling', name: 'Group in-service instances'}}, 
                    {label: 'AWS/AutoScaling - Group max size', value: {namespace: 'AWS/AutoScaling', name: 'Group max size'}},
                    {label: 'AWS/AutoScaling - Group min size', value: {namespace: 'AWS/AutoScaling', name: 'Group min size'}},
                    {label: 'AWS/AutoScaling - Group pending instances', value: {namespace: 'AWS/AutoScaling', name: 'Group pending instances'}},
                    {label: 'AWS/AutoScaling - Group terminated instances', value: {namespace: 'AWS/AutoScaling', name: 'Group terminated instances'}},
                    {label: 'AWS/AutoScaling - Group total instances', value: {namespace: 'AWS/AutoScaling', name: 'Group total instances'}},
                    {label: 'AWS/EBS - Volume idle time', value: {namespace: 'AWS/EBS', name: 'Volume idle time'}},
                    {label: 'AWS/EBS - Volume queue length', value: {namespace: 'AWS/EBS', name: 'Volume queue length'}},
                    {label: 'AWS/EBS - Volume read bytes', value: {namespace: 'AWS/EBS', name: 'Volume read bytes'}},
                    {label: 'AWS/EBS - Volume read ops', value: {namespace: 'AWS/EBS', name: 'Volume read ops'}},
                    {label: 'AWS/EBS - Volume total read time', value: {namespace: 'AWS/EBS', name: 'Volume total read time'}},
                    {label: 'AWS/EBS - Volume write ops', value: {namespace: 'AWS/EBS', name: 'Volume write ops'}},
                    {label: 'AWS/EBS - Volume total write time', value: {namespace: 'AWS/EBS', name: 'Volume total write time'}},
                    {label: 'AWS/EC2 - CPU utilization', value: {namespace: 'AWS/EC2', name: 'CPU utilization'}},
                    {label: 'AWS/EC2 - Disk read bytes', value: {namespace: 'AWS/EC2', name: 'Disk read bytes'}},
                    {label: 'AWS/EC2 - Disk read ops', value: {namespace: 'AWS/EC2', name: 'Disk read ops'}},
                    {label: 'AWS/EC2 - Disk write bytes', value: {namespace: 'AWS/EC2', name: 'Disk write bytes'}},
                    {label: 'AWS/EC2 - Disk write ops', value: {namespace: 'AWS/EC2', name: 'Disk write ops'}},
                    {label: 'AWS/EC2 - Network in', value: {namespace: 'AWS/EC2', name: 'Network in'}},
                    {label: 'AWS/EC2 - Network out', value: {namespace: 'AWS/EC2', name: 'Network out'}},
                    {label: 'AWS/ELB - HTTP code (back end) 2XX', value: {namespace: 'AWS/ELB', name: 'HTTP code (back end) 2XX'}},
                    {label: 'AWS/ELB - HTTP code (back end) 3XX', value: {namespace: 'AWS/ELB', name: 'HTTP code (back end) 3XX'}},
                    {label: 'AWS/ELB - HTTP code (back end) 4XX', value: {namespace: 'AWS/ELB', name: 'HTTP code (back end) 4XX'}},
                    {label: 'AWS/ELB - HTTP code (back end) 5XX', value: {namespace: 'AWS/ELB', name: 'HTTP code (back end) 5XX'}},
                    {label: 'AWS/ELB - HTTP code (LB) 4XX', value: {namespace: 'AWS/ELB', name: 'HTTP code (LB) 4XX'}},
                    {label: 'AWS/ELB - HTTP code (LB) 5XX', value: {namespace: 'AWS/ELB', name: 'HTTP code (LB) 5XX'}},
                    {label: 'AWS/ELB - Latency', value: {namespace: 'AWS/ELB', name: 'Latency'}},
                    {label: 'AWS/ELB - Request count', value: {namespace: 'AWS/ELB', name: 'Request count'}},
                    {label: 'AWS/ELB - Healthy host count', value: {namespace: 'AWS/ELB', name: 'Healthy host count'}},
                    {label: 'AWS/ELB - Unhealthy host count', value: {namespace: 'AWS/ELB', name: 'Unhealthy host count'}}
                ]),

                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },

                submitButton: {
                  click: function() {
                      console.log('Time to create the alarm!');
                      alarm.save({
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
                alarm.set('Namespace', metric.get('value').namespace);
                alarm.set('MetricName', metric.get('value').name);
            });

            scope.on('change:timeunit', function(model) {
                if (model.get('timeunit') == 'SECS') alarm.set('Period', Math.round(alarm.get('Period') * 60));
                if (model.get('timeunit') == 'MINS') alarm.set('Period', Math.round(alarm.get('Period') / 60));
            });

            this._do_init();
        },
	});
});
