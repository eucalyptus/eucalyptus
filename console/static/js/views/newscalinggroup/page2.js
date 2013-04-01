console.log('WIZARD:start');
define([
  'rivets',
  'dataholder',
  'text!./page2.html',
], function(rivets, dh, template) {
        return Backbone.View.extend({
          title: 'Membership', 

          loadBalancers: {
            name: 'loadBalancers',
            collection: dh.loadBalancers,
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
