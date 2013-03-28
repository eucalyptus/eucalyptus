define([
  'text!./template.html',
  'rivets',
  ], function( template, rivets ) {
    return Backbone.View.extend({
      
      initialize: function() {
        this.collection = this.options.model.collection;
        this.pairs = this.collection;
        this.$el.html(template);
        this.rView = rivets.bind(this.$el, {pairs: this.pairs});    
      },

      add: function() {

      },

      remove: function() {

      },
  
      render: function() {
        this.rView.sync();
        return this;
      }

    });
 });
