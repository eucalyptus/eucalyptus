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
            this.rview = rivets.bind(this.$el, scope);

            scope.get('zoneSelect').get('available').fetch();
          },

          render: function() {
            this.rview.sync();
          }
        });
});
