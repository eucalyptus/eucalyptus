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
            $(this.el).html(template);
            this.rview = rivets.bind(this.$el, scope);
           
            // compute values to make a valid model
            this.listenTo(this.model.get('policies'), 'change add', function(model) {
              var amount = model.get('amount');
              if(model.get('action') == 'SCALEDOWNBY') {
                amount *= -1;
              }
              model.set({'scaling_adjustment': amount}, {silent:true});
              
              if(model.get('measure') == 'percent') {
                model.set({'adjustment_type': 'PercentChangeInCapacity'}, {silent:true});
              } else {
                if(model.get('action') == 'SETSIZE') {
                  model.set({'adjustment_type': 'ExactCapacity'}, {silent: true});
                } else {
                  model.set({'adjustment_type': 'ChangeInCapacity'}, {silent: true});
                }
              }

              // get the alarm model for this policy
              if(model.get('alarm_model').hasChanged()) {
                self.model.get('alarms').add(model.get('alarm_model'));
                model.unset('alarm_model', {silent:true});
              }
            });
           },


          render: function() {
            this.rview.sync();
          },

       });
});
