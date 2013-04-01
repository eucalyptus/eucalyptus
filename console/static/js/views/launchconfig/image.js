define([
    'underscore',
    'app',
	'dataholder',
  'text!./image.html!strip',
  'rivets',
  'views/searches/image',
	], function( _, app, dataholder, template, rivets, imageSearch ) {
	return Backbone.View.extend({
            title: 'Image',
            count: 0,
            initialize : function() {
          var self = this;
          var scope = {
              view: this,

              isOdd: function() {
                  var selected = this.isSelected(arguments);
                  return ((this.view.count++ % 2) ? 'even' : 'odd') + selected;
              },

              isSelected: function(image) {
                var image = this.image;
                if (self.model.get('image_selected') == image.get('id')) {
                    return ' selected-row';
                } 
                return '';
              },

              setClass: function(image) {
                  var image = this.image;
                  return inferImage(image.get('location'), image.get('description'), image.get('platform'));
              },

              search: new imageSearch(app.data.images),
              
              select: function(e, images) {
                $(e.currentTarget).parent().find('tr').removeClass('selected-row');
                $(e.currentTarget).addClass('selected-row');
                self.model.set('image_id', images.image.get('id'));
                self.model.set('image_platform', images.image.get('platform') ? images.image.get('platform') : 'Linux');
                self.model.set('image_location', images.image.get('location'));
                self.model.set('image_description', images.image.get('description'));
                self.model.set('image_iconclass', this.setClass(images.image));
                self.model.set('image_selected', images.image.get('id'));
              },

              
              launchConfigErrors: {
                image_id: ''    
              }
          };
          self.model.on('validated:invalid', function(model, errors) {
              scope.launchConfigErrors.image_id = errors.image_id; 
              self.render(); 
          });

          scope.images = scope.search.filtered;
          this.scope = scope;
          scope.search.filtered.on('change reset', function() {
              self.render();
          });

         $(this.el).html(template)
         this.rView = rivets.bind(this.$el, this.scope);
         this.render();
        },

        render: function() {
          this.rView.sync();
        },
    
        isValid: function() {
          this.model.validate(_.pick(this.model.toJSON(),'image_id'));
          var error = this.model.isValid();
          return error;
        },
  });
});
