define([
  './eucadialogview',
  'text!./detach_volume_dialog.html!strip',
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

    initialize: function(args) {
       var self = this;
       this.template = template;

       var volume_list = new Backbone.Collection();
       // INTERATE THROUGH THE INPUT VOLUME ID LIST
       _.each(args.volume_ids, function(vid){
         // FIND THE INSTANCE ID FOR EACH VOLUME ATTACHED
         var instance_id = App.data.volume.get(vid).get('attach_data').instance_id;
         // FIND THE NAME TAG FOR THE VOLUME
         var volNameTag = self.findNameTag(App.data.volume.get(vid));
         volNameTag = self.createIdNameTagString(vid, addEllipsis(volNameTag, 15));
         // FIND THE NAME TAG FOR THE INSTANCE
         var instanceNameTag = self.findNameTag(App.data.instance.get(instance_id));
         instanceNameTag = self.createIdNameTagString(instance_id, addEllipsis(instanceNameTag, 15));
         // CREATE A LIST WITH THE NAME TAGS
         volume_list.push(new Volume({volume_id: volNameTag, instance_id: instanceNameTag}));
         // volume_id and instance_id are not in the ID format above, however it ok since they are display only   --- KYO 050313 
       });

       this.scope = {
         status: '',
         items: volume_list,
        
         cancelButton: {
           id: 'button-dialog-detachvolume-cancel',
           click: function() {
             self.close();
           }
         },

         detachButton: {
           id: 'button-dialog-detachvolume-detach',
           click: function() {
              doMultiAction(args.volume_ids, App.data.volumes,
                            function(model, options) {
                              model.detach(options);
                            },
                            'volume_detach_progress', 'volume_detach_done', 'volume_detach_fail',
                            function(response) {
                              if (response.results && response.results == 'detaching') {
                                return; // all good
                              } else {
                                return undefined_error;
                              }
                            });
             self.close();
           }  
         }
       }
       this._do_init();
     },
  });
});
