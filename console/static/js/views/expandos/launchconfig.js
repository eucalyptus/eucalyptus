define([
  'app',
  './eucaexpandoview',
  'text!./launchconfig.html!strip',
], function(app, EucaExpandoView, template) {
  return EucaExpandoView.extend({
    initialize : function(args) {
      this.template = template;
      var tmp = this.model ? this.model : new Backbone.Model();
      this.model = new Backbone.Model();
      this.model.set('config', tmp);
      this.model.set('image', app.data.images.get(tmp.get('image_id')));
      var mappings = [];
      var bdm = tmp.get('block_device_mapping');
      if (bdm) {
        if (bdm['/dev/sda']) {
          this.model.set('snapshot_id', bdm['/dev/sda'].snapshot_id);
        }
        for (var device in bdm) {
          var tmp = bdm[device];
          mappings.push({device:device, snapshot_id:tmp.snapshot_id, size:tmp.size, delete_on_terminate:tmp.delete_on_termination});
        }
      }
      this.model.set('bdm', mappings);
      this.scope = this.model;
      var sec_group = tmp.get('security_groups');
      if (sec_group) sec_group = sec_group[0];
      if (sec_group) this.scope.set('group_name', app.data.sgroups.findWhere({id:sec_group}).get('name'));
      this._do_init();
    },
  });
});
