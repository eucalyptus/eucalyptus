console.log('WIZARD:start');
define([
  'rivets',
  'app',
  'text!./page2.html',
], function(rivets, app, template) {
        return Backbone.View.extend({
          title: 'Membership', 

          initialize: function() {
            var self = this;
            $(this.el).html(template)

            var scope = new Backbone.Model({
                scalingGroup: this.model.get('scalingGroup'),
                scalingGroupErrors: this.model.get('scalingGroupErrors'),
                loadBalancers: new Backbone.Model({
                    name: 'loadBalancers',
                    available: app.data.loadbalancer,
                    selected: self.model.get('loadBalancers'),
                    getId: function(item) {
                        return item.get('name');
                    },
                    getValue: function(item) {
                        return item.get('name');
                    }
                }),
                zoneSelect: new Backbone.Model({
                    available: app.data.availabilityzone,
                    selected: self.model.get('availabilityZones'),
                    getId: function(item) {
                        return item.get('name');
                    },
                    getValue: function(item) {
                        return item.get('name');
                    }
                })
            });

            this.rview = rivets.bind(this.$el, scope);

            scope.get('zoneSelect').get('available').fetch();
          },

          render: function() {
            this.rview.sync();
            return this;
          },

          isValid: function() {
            // assert that this step is valid before "next" button works
            var sg = this.model.get('scalingGroup');
            var errors = new Backbone.Model(sg.validate());
            var valid = sg.isValid(['availability_zones', 'health_check_type']); 
            if(!valid)
                this.model.get('scalingGroupErrors').set(errors.pick('availability_zones', 'health_check_type'));
            return valid;
          }
        });
});
