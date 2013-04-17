define([
  './eucadialogview',
  'text!./detach_volume_dialog.html!strip',
  'models/volume',
  'app',
  'backbone',
], function(EucaDialogView, template, Volume, App, Backbone) {
   return EucaDialogView.extend({
     initialize : function(args) {
       var self = this;
       this.template = template;

       var volume_list = new Backbone.Collection();
       _.each(args.volume_ids, function(vid){
         var instance_id = App.data.volume.get(vid).get('attach_data').instance_id;
         volume_list.push(new Volume({volume_id: vid, instance_id: instance_id}));
         console.log("Volume: " + vid + " Instance: " + instance_id);
       });

       this.scope = {
         status: '',
         items: volume_list,
        
         cancelButton: {
           click: function() {
             self.close();
           }
         },

         detachButton: {
           click: function() {

              doMultiAction(args.volume_ids, App.data.volumes,
                            function(model, options) {
                              options['wait'] = true;
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
