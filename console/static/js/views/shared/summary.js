define([
  'text!./summary.html',
  'rivets'
], function(template, rivets) {
  return Backbone.View.extend({
    tpl: template,
    initialize: function() {

      var scope = {
        view: this,
        imageModel: this.options.imageModel,
        typeModel: this.options.typeModel,
        securityModel: this.options.securityModel,
        keymodel: this.options.keymodel,
        advancedModel: this.options.advancedModel,
        title: 'Summary',
      };

      scope.imageModel.bind('change:image_iconclass', this.swapIconClass, this);
      this.$el.html(this.tpl);
      this.riv = rivets.bind(this.$el, scope);
      this.render();
      this.swapIconClass();  
    },

    render: function() {
      this.riv.sync();
    },

    swapIconClass: function() {
      var target = this.$el.find('#summary-icon');
      target.removeClass();
      target.addClass('image-type').addClass('summary').addClass(this.options.imageModel.get('image_iconclass'));
    },

  });
});
