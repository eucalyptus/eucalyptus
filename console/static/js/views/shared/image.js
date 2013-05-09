define([
    'underscore',
    'backbone',
    'app',
	'dataholder',
  'text!./image.html!strip',
  'rivets',
  'views/searches/image',
  './model/blockmap'
	], function( _, Backbone, app, dataholder, template, rivets, imageSearch, BlockMap ) {
	return Backbone.View.extend({
            title: app.msg('launch_instance_section_header_image'),
            count: 0,
            image_selected: null,

            initialize : function(args) {
              var self = this;
              var scope = {
                view: this,
                blockmaps: self.options.blockMaps,

                formatName: function(image){
                  return DefaultEncoder().encodeForHTML(this.image.get('name'));
                },
 
                formatDescription: function(image){
                  return DefaultEncoder().encodeForHTML(this.image.get('description'));
                },

                isOdd: function() {
                    var selected = this.isSelected(arguments);
                    return ((this.view.count++ % 2) ? 'even' : 'odd') + selected;
                },

                isSelected: function(image) {
                  var image = this.image;
                  if (self.model.get('id') == image.get('id')) {
                      return ' selected-row';
                  } 
                  return '';
                },

                setClass: function(image) {
                    var image = this.image;
                    return inferImage(image.get('location'), image.get('description'), image.get('platform'));
                },

                search: new imageSearch(new Backbone.Collection(app.data.images.where({type: 'machine', state: 'available'}))),
                //search: new imageSearch(app.data.images),
                
                select: function(e, images) {
                  $(e.currentTarget).parent().find('tr').removeClass('selected-row');
                  $(e.currentTarget).addClass('selected-row');
                  self.model.set('image_iconclass', this.setClass(images.image));
                  self.model.set('id', images.image.get('id'));
                  //images.image.unset('tags'); // workaround - nested objects break the next line
                  self.model.set(images.image.toJSON());
                  self.model.set('platform', this.setClass(self.model));

                  //block device maps
                  var maps = images.image.get('block_device_mapping');
                  var keys = _.keys(maps);
                  for(i=0; i<keys.length; i++) {
                    var key = keys[i];
                    var map = {
                      device_name: key,
                      volume_size: maps[key].size
                    };
                    
                    var subkeys = _.keys(maps[key]);
                    for(j=0; j<subkeys.length; j++) {
                      map[subkeys[j]] = maps[key][subkeys[j]];
                    }
                  }
                  if(map !== undefined) {
                    self.options.blockMaps.reset(new BlockMap(map));
                  } else {
                    self.options.blockMaps.reset();
                  }
                },

                
                launchConfigErrors: {
                  image_id: ''    
                }
          };
          self.model.on('validated:invalid', function(model, errors) {
              scope.launchConfigErrors.image_id = errors.id; 
              self.render(); 
          });

          self.model.on('validated:valid', function()  {
            scope.launchConfigErrors.image_id = null;
            self.render();
          });

          scope.images = scope.search.filtered;
          this.scope = scope;
          scope.search.filtered.on('add remove sync change reset', function() {
              self.render();
          });

         $(this.el).html(template)
         this.rView = rivets.bind(this.$el, this.scope);
         this.render();

         if(this.model.get('image') != undefined) {
            this.$el.find('span:contains("' + this.model.get('image') + '")').closest('tr').click();
         }

        },

        render: function() {
          this.rView.sync();
        },
    
        isValid: function() {
          this.model.validate(_.pick(this.model.toJSON(),'id'));
          var error = this.model.isValid();

          return error;
        },
  });
});
