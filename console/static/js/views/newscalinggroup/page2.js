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

            this.model.set('scalingGroupErrors', new Backbone.Model());
            scope.set('scalingGroupErrors', this.model.get('scalingGroupErrors'));

            scope.get('scalingGroup').on('change', function(model) {
                scope.get('scalingGroup').validate(model.changed);
            });

            scope.get('scalingGroup').on('validated', function(valid, model, errors) {
                _.each(_.keys(model.changed), function(key) { 
                    scope.get('scalingGroupErrors').set(key, errors[key]); 
                });
            });

            this.rview = rivets.bind(this.$el, scope);

            scope.get('zoneSelect').get('available').fetch();
          },

          render: function() {
            this.rview.sync();
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
