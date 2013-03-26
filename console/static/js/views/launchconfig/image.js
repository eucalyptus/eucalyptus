define([
	'dataholder',
	//'text!./image_static.html!strip',
  'text!./image.html!strip',
  'rivets',
	], function( dataholder, template, rivets ) {
	return Backbone.View.extend({
    title: 'Image',
		initialize : function() {
  
      var scope = {
          view: this,
          images: dataholder.images,
          imageNames: {},
          isOdd: function() {
              return true;
          },
          setClass: function() {
              console.log('ROW',this);
              var image = this.image;
              return inferImage(image.attributes.location, image.attributes.description, image.attributes.platform);
          },
      } 

     $(this.el).html(template)
     this.rView = rivets.bind(this.$el, scope);
     this.render();
		},

    setImageNames: function() {
        var view = this;
        _.each(this.images.models, function(image) {
          view.imageNames[image.id] = inferImage(image.attributes.location, image.attributes.description, image.attributes.platform);
        });
    },

  render: function() {
    this.rView.sync();
  }
});
});
