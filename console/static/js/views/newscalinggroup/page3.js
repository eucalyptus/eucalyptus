define([
  'app',
  'backbone',
  'rivets',
  'text!./page3.html',
], function(app, Backbone, rivets, template) {
        return Backbone.View.extend({
          title: 'Policies', 

          initialize: function() {
            var self = this;

            var scope = new Backbone.Model({
                scalingGroup: this.model.get('scalingGroup'),
                alarms: this.model.get('alarms'),
                policies: new Backbone.Model({
                    scalingGroup: this.model.get('scalingGroup'),
                    alarms: this.model.get('alarms'),
                    error: new Backbone.Model(),
                    available: new Backbone.Collection(),
                    selected: self.model.get('policies'),
                    getId: function(item) {
                        return item;
                    },
                    getValue: function(item) {
                        return item;
                    }
                })
            });

            // adjust parameters in passed in policy models to match input form
            // reversing what happens in ui-editpolicies when models are set
            self.model.get('policies').each(function(p) {
              if(p.get('adjustment_type') == 'PercentChangeInCapacity') {
                p.set('measure', 'percent');
              } else {
                p.set('measure', 'instance');
              }
              if(p.get('adjustment_type') == 'ExactCapacity') {
                p.set('action', 'SETSIZE');
                p.set('amount', p.get('scaling_adjustment'));
              } else {
                if(p.get('scaling_adjustment') < 0) {
                  p.set('action', 'SCALEDOWNBY');
                  p.set('amount', p.get('scaling_adjustment') * -1);
                } else {
                  p.set('action', 'SCALEUPBY');
                  p.set('amount', p.get('scaling_adjustment'));
                }
              }

            });

            //ensure as_name is set for edits
            if(self.model.get('scalingGroup')) {
              scope.get('policies').set('as_name', self.model.get('scalingGroup').get('name'));
            }

            $(this.el).html(template);
            this.rview = rivets.bind(this.$el, scope);
           
          this.listenTo(this.model.get('scalingGroup'), 'change:name', function(model, value) {
            scope.get('policies').set('as_name', value);
            // change all existing policies
            scope.get('policies').get('selected').each(function(pol) {
              pol.set('as_name', value);
            });
          });
            
          },


          render: function() {
            this.rview.sync();
            return this;
          },

       });
});
