define([
  'text!./template.html',
  'rivets',
  ], function( template, rivets ) {
    return Backbone.View.extend({
      
      initialize: function(args) {
        var self = this;
        this.$el.html(template);
        this.rView = rivets.bind(this.$el, args.model);
        console.log("ROWLIST", args);    
      },

      add: function() {

      },

      remove: function() {
      },
  
      render: function() {
        console.log('list render');
        this.rView.sync();
        return this;
      }

    });
 });
