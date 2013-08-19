// alarm model
//

define([
    './eucamodel'
], function(EucaModel) {
    var validateNumber = function(value, attr, computedState) {
        if (!($.isNumeric(value) && 
            value > 0 && 
            Math.round(value) == value)) {
            var attrU = attr.replace(/ /g, '_');
            return $.i18n.prop('alarm_field_' + attrU) + ' must be a whole, positive number';
        }
    }
    var model = EucaModel.extend({
        idAttribute: 'name',
        getMap: function(att_name) {
          if(this.get(att_name) == undefined) {
            return this.get(this.attmap[att_name]);
          }
          return this.get(att_name);
        },

        defaults: {
            timeunit: 'SECS'
        },

        attmap: {
          AlarmName: 'name',
          AlarmDescription: 'description',
          ComparisonOperator: 'comparison',
          Threshold: 'threshold',
          EvaluationPeriods: 'evaluation_periods',
          Namespace: 'namespace',
          MetricName: 'metric',
          Dimension: 'dimension',
          Period: 'period',
          Statistic: 'statistic',
          AlarmActions: 'alarm_actions'
        },

        validation: {
            name:   {
                rangeLength: [1, 128],
                required: true
            },
            dimension:   {
                required: true
            },
            dimension_value:   {
                required: true
            },
            period:   {
                required: true,
                pattern: 'number',
                min: 0,
                fn: function(value, attr, computedState) {
                    if (!($.isNumeric(value) && 
                        value > 0 && 
                        Math.round(value) == value)) {
                        return attr + ' must be a whole, positive multiple of 60';
                    }
                    if (this.get('timeunit') == 'SECS') {
                        if (value % 60) {
                            return attr + ' must be a multiple of 60';
                        }
                    }
                }
            },
            statistic:   {
                required: true
            },
            metric:   {
                required: true
            },
            threshold:   {
                required: true,
                fn: validateNumber
            },
            evaluation_periods:   {
                required: true,
                fn: validateNumber
            },
        },

        COMPARISON: [
            {value: 'GreaterThanThreshold', label: '>'}, 
            {value: 'GreaterThanOrEqualToThreshold', label: '> or ='},
            {value: 'LessThanThreshold', label: '<'}, 
            {value: 'LessThanOrEqualToThreshold', label: '< or ='}
        ],

        STATISTIC: [
            {value: 'Average', label: 'Average'},
            {value: 'Maximum', label: 'Maximum'},
            {value: 'Minimum', label: 'Minimum'},
            {value: 'SampleCount', label: 'Sample Count'},
            {value: 'Sum', label: 'Sum'}
        ],

        METRICS: new Backbone.Collection([
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


        isNew: function() {
            return this.get('alarm_arn') == null;
        },

        sync: function(method, model, options){
          if(method == 'create' || method == 'update'){
            var url = "/monitor?Action=PutMetricAlarm";
            var parameter = "_xsrf="+$.cookie('_xsrf');

            parameter += "&AlarmName="+encodeURIComponent(this.getMap('AlarmName'));

            if (this.getMap('AlarmDescription')) {
              parameter += "&AlarmDescription="+encodeURIComponent(this.getMap('AlarmDescription'));
            }

            if (this.getMap('ComparisonOperator')) {
              parameter += "&ComparisonOperator="+encodeURIComponent(this.getMap('ComparisonOperator'));
            }

            if (this.getMap('Threshold')) {
              parameter += "&Threshold="+encodeURIComponent(this.getMap('Threshold'));
            }

            if (this.getMap('EvaluationPeriods')) {
              parameter += "&EvaluationPeriods="+encodeURIComponent(this.getMap('EvaluationPeriods'));
            }
            
            if (this.getMap('Namespace')) {
              parameter += "&Namespace="+encodeURIComponent(this.getMap('Namespace'));
            }

            if (this.getMap('MetricName')) {
              parameter += "&MetricName="+encodeURIComponent(this.getMap('MetricName'));
            }

            if (this.getMap('Period')) {
              if (this.get('timeunit') == 'MINS') {
                  parameter += "&Period="+encodeURIComponent(this.getMap('Period') * 60);
              } else {
                  parameter += "&Period="+encodeURIComponent(this.getMap('Period'));
              }
            }

            if (this.getMap('Statistic')) {
              parameter += "&Statistic="+encodeURIComponent(this.getMap('Statistic'));
            }

            if (this.getMap('alarm_actions')) {
              $.each(this.getMap('alarm_actions'), function(idx, action) {
                parameter += "&AlarmActions.member." + (idx+1) + "=" + action;
              });
            } 

            this.makeAjaxCall(url, parameter, options);
          }
          else if(method == 'delete'){
            var url = "/monitor?Action=DeleteAlarms";
            var id = this.get('id');
            var parameter = "_xsrf="+$.cookie('_xsrf')+"&AlarmNames.member.1=" +
                encodeURIComponent(this.get('name'));
            this.makeAjaxCall(url, parameter, options);
          }
        },
        initialize: function() {
            var self = this;
        }
    });
    return model;
});
