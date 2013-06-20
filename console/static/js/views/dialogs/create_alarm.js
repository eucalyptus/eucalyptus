define([
   'app',
   './eucadialogview',
   'text!./create_alarm.html!strip',
], function(app, EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;

            var scope = {
                status: '',
                items: args.items, 

                comparison: [
                    '>', 
                    '> or =', 
                    '<', 
                    '< or ='
                ],

                statistic: [
                    'Average',
                    'Maximum',
                    'Minimum',
                    'Sample count',
                    'Sum'
                ],

                metrics: [
                    'AWS/AutoScaling - Group desired capacity',
                    'AWS/AutoScaling - Group in-service instances',
                    'AWS/AutoScaling - Group max size',
                    'AWS/AutoScaling - Group min size',
                    'AWS/AutoScaling - Group pending instances',
                    'AWS/AutoScaling - Group terminated instances',
                    'AWS/AutoScaling - Group total instances',
                    'AWS/EBS - Volume idle time',
                    'AWS/EBS - Volume queue length',
                    'AWS/EBS - Volume read bytes',
                    'AWS/EBS - Volume read ops',
                    'AWS/EBS - Volume total read time',
                    'AWS/EBS - Volume write ops',
                    'AWS/EBS - Volume total write time',
                    'AWS/EC2 - CPU utilization',
                    'AWS/EC2 - Disk read bytes',
                    'AWS/EC2 - Disk read ops',
                    'AWS/EC2 - Disk write bytes',
                    'AWS/EC2 - Disk write ops',
                    'AWS/EC2 - Network in',
                    'AWS/EC2 - Network out',
                    'AWS/ELB - HTTP code (back end) 2XX',
                    'AWS/ELB - HTTP code (back end) 3XX',
                    'AWS/ELB - HTTP code (back end) 4XX',
                    'AWS/ELB - HTTP code (back end) 5XX',
                    'AWS/ELB - HTTP code (LB) 4XX',
                    'AWS/ELB - HTTP code (LB) 5XX',
                    'AWS/ELB - Latency',
                    'AWS/ELB - Request count',
                    'AWS/ELB - Healthy host count',
                    'AWS/ELB - Unhealthy host count'
                ],

                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },

                submitButton: {
                  click: function() {
                      self.close();
                  }
                },

                createMetric: function() {
                    app.dialog('create_metric');
                }
            }
            this.scope = scope;

            this._do_init();
        },
	});
});
