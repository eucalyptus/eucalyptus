define([], function() {
  return Backbone.Model.extend({
      status: "", 
      __obj_name__: "Snapshot", 
      description: "", 
      owner_alias: null, 
      start_time: "", 
      tags: {}, 
      volume_size: null, 
      connection: [], 
      item: "", 
      volume_id: "", 
      progress: "", 
      region: [], 
      id: "", 
      owner_id: "" 
  });
});
