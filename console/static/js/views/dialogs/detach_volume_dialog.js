define([
  './eucadialogview',
  'text!./detach_volume_dialog.html!strip',
  'app',
  'backbone',
], function(EucaDialogView, template, App, Backbone) {
   return EucaDialogView.extend({
     initialize : function(args) {
       var self = this;
       this.template = template;

       this.scope = {
         status: '',
         items: function(){                   // Feels very hacky -- Kyo 040813
           var volume_list = [];
           _.each(args.volume_ids, function(v){
             var match = App.data.volume.find(function(model){ return model.get('id') == v; });
             var instance_id = match.toJSON().attach_data.instance_id;
             volume_list.push({volume_id: v, instance_id: instance_id});
             console.log("Volume: " + v + " Instance: " + instance_id);
           });
           return volume_list;
         },

         cancelButton: {
           click: function() {
             self.close();
           }
         },

         detachButton: {
           click: function() {
              doMultiAction(self.scope.items, App.data.volumes,
                            function(model, options) {
                              model.detach(options);
                            },
                            'volume_detach_progress', 'volume_detach_done', 'volume_detach_fail');
             self.close();
           }  
         }
       }
       this._do_init();
     },
  });
});
