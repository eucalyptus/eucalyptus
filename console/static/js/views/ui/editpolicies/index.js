define([
   'underscore',
   'text!./template.html!strip',
   'backbone',
   'app',
   'models/scalingpolicy'
], function(_, template, Backbone, app, Policy) {
    return Backbone.View.extend({
        initialize : function(args) {
            var self = this;

            this.template = template;
            var model = args.model;

            var scalingGroup = model.get('scalingGroup')
            var available = model.get('available');
            var selected = model.get('selected');
            var getId = model.get('getId')
            var getValue = model.get('getValue')

            var scope;
            scope = new Backbone.Model({
                alarms: app.data.alarm,
                available: available,
                selected: selected,
                error: model.get('error'),
                toAdd: new Policy(),

                getId: function() {
                    return getId(this.item);
                },

                getValue: function() {
                    return getValue(this.item);
                },

                add: function(element, scope) {
                    var toAdd = scope.get('toAdd');
                    toAdd.set('as_name', self.model.get('as_name')); // TODO: set in already added ones too.
                    toAdd.set('alarm_model', scope.get('alarms').findWhere({name: toAdd.get('alarm')}));
                    if(!toAdd.isValid(true)) return;
                    selected.push(toAdd);
                    scope.set('toAdd', new Policy());
                    scope.get('toAdd').on('change:amount change:action change:measure change:alarm_model', self.compute);
                    scope.get('toAdd').on('validated', self.setErrors, scope);
                    self.render();
                    console.log('add - selected:', selected);
                },

                delete: function(element, scope) {
                    selected.remove(this.item);
                    self.render();
                    console.log('delete - selected:', selected);
                },

                createAlarm: function(element, scope) {
                    app.dialog('create_alarm', { scalingGroup: scalingGroup });
                }
            }); // end of scope

            scope.get('toAdd').on('change:amount change:action change:measure change:alarm_model', self.compute);
            scope.get('toAdd').on('validated', self.setErrors, scope);

            this.$el.html(template);
            this.rview = rivets.bind(this.$el, scope);

            scope.get('available').on('sync', function() {
                console.log('SYNC');
                self.render();
            });

            //app.data.alarm.on('sync', function() { self.render(); });
            app.data.alarm.fetch();

            
        },

        // compute values to make a valid model
       // cope.get('toAdd').on('change:amount change:action change:measure change:alarm_model', 
       compute: function(policy) {
                var amount = +policy.get('amount');
                if(policy.get('action') == 'SCALEDOWNBY') {
                  amount *= -1;
                }
                policy.set('scaling_adjustment', amount);
                
                if(policy.get('measure') == 'percent') {
                  policy.set('adjustment_type', 'PercentChangeInCapacity');
                } else {
                  if(policy.get('action') == 'SETSIZE') {
                    policy.set('adjustment_type', 'ExactCapacity');
                  } else {
                    policy.set('adjustment_type', 'ChangeInCapacity');
                  }
                }

                // get the alarm model for this policy
                if(policy.get('alarm_model') && policy.get('alarm_model').hasChanged()) {
                  self.model.get('alarms').add(policy.get('alarm_model'));
                  policy.unset('alarm_model', {silent:true});
                }
        }, 

        setErrors: function(valid, model, errors) {
              var scope = this;
              scope.get('error').clear();
              _.each(errors, function(val, key) {
                scope.get('error').set(key, val);
              });
        },


        render : function() {
          this.rview.sync();
          return this;
        }
    });
});
