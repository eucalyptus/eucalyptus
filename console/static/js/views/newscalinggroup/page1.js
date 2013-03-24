define([
  'rivets',
  'dataholder',
  'text!./page1.html',
], function(rivets, dh, template) {
        return Backbone.View.extend({
          title: 'General', 

          launchConfigs: {
            name: 'launchConfig',
            label: 'Launch Configuration',
            collection: dh.launchConfigs,
            itrLabel: function() {
              console.log('ITR', this);
              return 'LABEL';
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
