define([
  'rivets',
  'dataholder',
  'text!./page1.html',
], function(rivets, dh, template) {
        return Backbone.View.extend({
          title: 'General', 

          launchConfigs: {
            name: 'launchConfig',
            collection: dh.launchConfigs,
            itrLabel: function() {
              return this.itr.get('name');
            } 
          },

          initialize: function() {
            $(this.el).html(template)
            this.rView = rivets.bind(this.$el, this);
            this.render();
          },
          render: function() {
            this.rView.sync();
          }
        });
});
