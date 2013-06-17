console.log('WIZARD:start');
define([
  'rivets',
  'app',
  'text!./page2.html',
], function(rivets, app, template) {
        return Backbone.View.extend({
          title: 'Membership', 

          loadBalancers: {
            name: 'loadBalancers',
            collection: app.data.loadBalancers,
            itrLabel: function() {
              return this.itr.get('name');
            } 
          },

          initialize: function() {
            $(this.el).html(template)

            var scope = new Backbone.Model({
                zoneSelect: new Backbone.Model({
                    available: app.data.availabilityzone,
                    selected: new Backbone.Collection(),
                    getId: function(item) {
                        console.log('VALUE', arguments);
                        return item.get('name');
                    },
                    getValue: function(item) {
                        console.log('VALUE', arguments);
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
