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
            this.render();
          },

          render: function() {
            $(this.el).html(template)
            rivets.bind(this.$el, this);
          }
        });
});
