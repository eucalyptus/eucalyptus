define([
  'text!./template.html',
  'rivets',
  ], function( template, rivets ) {
    return Backbone.View.extend({
      
      initialize: function(args) {
        this.$el.html(template);
        this.list = args.model.list;
        this.rView = rivets.bind(this.$el, args.model);    
      },

      add: function() {
        console.log("ADDTAG", arguments);
      },

      render: function() {
        this.rView.sync();
        return this;
      }

    });
 });
