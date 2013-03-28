define([
  'text!./summary.html',
  'rivets'
], function(template, rivets) {
  return Backbone.View.extend({
    initialize: function() {

      this.model.bind('change:image_iconclass', this.swapIconClass, this);

      var scope = {
        view: this,
        model: this.model,
        title: 'Summary',
        summary: this.model,
      };


      this.$el.html(template);
      this.riv = rivets.bind(this.$el, scope);
      this.render();
    },

    render: function() {
      this.riv.sync();
      this.model.set('image', 'EucaImage');
    },

    swapIconClass: function() {
        
        console.log("SWAPCLASS", arguments);
      var target = this.$el.find('#summary-icon');
      target.removeClass();
      target.addClass('image-type').addClass('summary').addClass(this.model.get('image_iconclass'));
    }
  });
});
