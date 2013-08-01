// alarm model
//

define([
    './eucamodel'
], function(EucaModel) {
    var model = EucaModel.extend({
        getMap: function(att_name) {
          if(this.get(att_name) == undefined) {
            return this.get(this.attmap[att_name]);
          }
          return this.get(att_name);
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
                required: true
            },
            statistic:   {
                required: true
            },
            metric:   {
                required: true
            },
            evaluation_periods:   {
                required: true
            },
        },

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
              parameter += "&Period="+encodeURIComponent(this.getMap('Period'));
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
    });
    return model;
});
