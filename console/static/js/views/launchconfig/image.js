define([
    'app',
	'dataholder',
	//'text!./image_static.html!strip',
  'text!./image.html!strip',
  'rivets',
  'views/searches/images',
	], function( app, dataholder, template, rivets, imageSearch ) {
	return Backbone.View.extend({
            title: 'Image',
            count: 0,
            initialize : function() {
          imageTest = new imageSearch(app.data.images);
          var scope = {
              view: this,
              isOdd: function() {
                  return (this.view.count++ % 2) ? 'even' : 'odd';
              },

              setClass: function() {
                  var image = this.image;
                  return inferImage(image.attributes.location, image.attributes.description, image.attributes.platform);
              },

              search: new imageSearch(app.data.images),
              
              select: function(e, images) {
                $(e.currentTarget).parent().find('tr').removeClass('selected-row');
                $(e.currentTarget).addClass('selected-row');
                this.view.model.set('image_id', images.image.id);
                this.view.model.set('image_platform', images.image.attributes.platform ? images.image.attributes.platform : 'Linux');
                this.view.model.set('image_location', images.image.attributes.location);
                this.view.model.set('image_description', images.image.attributes.description);
                this.view.model.set('image_iconclass', this.setClass(images.image));
                console.log("SELECTED", this.view.model, images.image);
              }
          } 
          scope.images = scope.search.filtered;

         $(this.el).html(template)
         this.rView = rivets.bind(this.$el, scope);
         this.render();
        },

        render: function() {
          var searchCollection = this.images;
          this.rView.sync();
        }
    });
});
