define([
  './eucadialogview',
  'text!./delete_volume_dialog.html!strip',
  'models/volume',
  'app',
  'backbone',
], function(EucaDialogView, template, Volume, App, Backbone) {
  return EucaDialogView.extend({

    createIdNameTagString: function(resource_id, name_tag){
      var this_string = resource_id;
      if( name_tag != null ){
        this_string += " (" + name_tag + ")";
      }
      return this_string;
    },

    findNameTag: function(model){
      var nameTag = null;
      model.get('tags').each(function(tag){
        if( tag.get('name').toLowerCase() == 'name' ){
          nameTag = tag.get('value');
        };
      });
      return nameTag;
    },

    initialize : function(args) {
      var self = this;
      this.template = template;

      var volume_list = [];
      _.each(args.items, function(vid){
        var nameTag = self.findNameTag(App.data.volume.get(vid));
        console.log("Volume: " + vid + " Name Tag: " + nameTag);
        volume_list.push(self.createIdNameTagString(vid, addEllipsis(nameTag, 15)));   // DISPLAY ONLY
      });

      this.scope = {
        status: '',
        volumes: volume_list, 
		help: {title: null, content: help_volume.dialog_delete_content, url: help_volume.dialog_delete_content_url, pop_height: 600},

        cancelButton: {
          id: 'button-dialog-deletevolume-cancel',
          click: function() {
            self.close();
          }
        },
        deleteButton: {
          id: 'button-dialog-deletevolume-delete',
          click: function() {
              doMultiAction(args.items, App.data.volumes,
                            function(model, options) {
                              options['wait'] = true;
                              model.destroy(options);
                            },
                            'volume_delete_progress', 'volume_delete_done', 'volume_delete_fail');
              self.close();
          }
        },
      }
      this._do_init();
    },
  });
});
