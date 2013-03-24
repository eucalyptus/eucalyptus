define([
	'dataholder',
	'text!./image_static.html!strip',
  'text!./image.html!strip',
  'rivets',
	], function( dataholder, template, rivets ) {
	return Backbone.View.extend({
		initialize : function() {
			this.view = this;
			this.images = dataholder.images;
      this.images.on('all', this.render, this);
      this.imageNames = {};
			this.render();
		},

    setImageNames: function() {
        var view = this;
        _.each(this.images.models, function(image) {
            console.log("IMAGES - model: ", image.attributes);
          view.imageNames[image.id] = inferImage(image.attributes.location, image.attributes.description, image.attributes.platform);
          console.log("IMAGES - setname: ", view.imageNames[image.id]);
        });
    },


		render : function() {
      this.setImageNames();
      this.$el.html(_.template(template,{images: this.images, imageNames: this.imageNames}));
      //this.$el.html(template);

			rivets.bind(this.$el, {images: this.images});
			//$(':input[data-value]', this.$el).keyup(function() { $(this).trigger('change'); });
			return this;
		}
	});
});
