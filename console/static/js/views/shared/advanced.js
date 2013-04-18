define([
  'app',
	'dataholder',
  'text!./advanced.html!strip',
  'rivets',
  './model/blockmap'
	], function( app, dataholder, template, rivets, blockmap ) {
	return Backbone.View.extend({
    tpl: template,
    title: 'Advanced',
    launchConfigErrors: new Backbone.Model({volume_size: ''}),
    
		initialize : function() {

      var self = this;
      var scope = {
        advancedModel: self.model,
        kernels: new Backbone.Collection(dataholder.images.where({type: 'kernel'})), 
        ramdisks: new Backbone.Collection(dataholder.images.where({type: 'ramdisk'})),
        user_data: self.model.get('user_data'),
        enableMonitoring: true,
        privateNetwork: false,
        snapshots: dataholder.snapshot,
        blockDeviceMappings: self.options.blockMaps,
        enableStorageVolume: true,
        enableMapping: true,
        enableSnapshot: true,
        deleteOnTerm: true,

        setKernel: function(e, obj) {
          self.model.set('kernel_id', e.target.value);
        },

        setPrivateNetwork: function(e, item) {
          self.model.set('private_network', e.target.value);
        },

        setMonitoring: function(e, item) {
            self.model.set('instance_monitoring', e.target.value);
        },

        setRamDisk: function(e, item) {
          self.model.set('ramdisk_id', e.target.value);
        },

        setStorageVolume: function(e, obj) {
          var m = new blockmap();
          self.launchConfigErrors.clear();
          m.on('validated:invalid', function(o, errors) { self.launchConfigErrors.set('volume_size', errors.volume_size)});

          var tr = $(e.target).closest('tr');
          var vol = tr.find('.launch-wizard-advanced-storage-volume-selector').val();
          var dev = "/dev/" + tr.find('.launch-wizard-advanced-storage-mapping-input').val();
          var snap = tr.find('.launch-wizard-advanced-storage-snapshot-input').val();
          var size = tr.find('.launch-wizard-advanced-storage-size-input').val();
          var del = tr.find('.launch-wizard-advanced-storage-delOnTerm').prop('checked');

          m.set({
              device_name: dev,
              status: null,
              __obj_name__: "BlockDeviceType",
              attach_time: null,
              no_device: false,
              volume_id: null,
              connection: null,
              snapshot_id: snap,
              volume_size: size,
              ebs: {
                snapshot_id: snap,
                volume_size: size,
                delete_on_termination: del,
                volume_type: null,
                iopts: null 
              },
              delete_on_termination: del,
              ephemeral_name: null    
          });

          m.validate();
          if (m.isValid()) {
            this.blockDeviceMappings.add(m);
          }

        },

        delStorageVol: function(e, obj) {
          this.blockDeviceMappings.remove(obj.volume); 
        },

        getVolLabel: function(obj) {
          if (obj.volume.get('device_name') == '/dev/sda1') {
              return 'Root';
          } else {
              return 'EBS';
          }
        },

        deleteButtonIf: function(obj) {
          if(this.getVolLabel(obj) != 'Root') {
            return 'icon_delete';
          }
        },

        checkDisabledIf: function(obj) {
          if(this.getVolLabel(obj) != 'Root') {
            return false;
          }
          return true;
        },

        mapName: function() {
          var model = this.blockDeviceMappings.at(this.blockDeviceMappings.length - 1);
          if(undefined !== model && undefined != model.get('device_name')) {
            var drive = model.get('device_name').replace(/\/dev\/([a-z]*)([0-9]{1,2})/, '$1');
            var partition = model.get('device_name').replace(/\/dev\/([a-z]*)([0-9]{1,2})/, '$2');
            return drive + (++partition);
          } else {
            return 'sda1';
          }
        },

        userdata: function(e,obj) {
          self.model.set('user_data', $.base64.encode(e.target.value));
        },

        launchConfigErrors: self.launchConfigErrors

      };

      scope.blockDeviceMappings.on('change add reset remove', function() {
          self.model.set('bdmaps_configured', true);
          self.render();
      });

      dataholder.images.on('reset', function() {
        scope.kernels.add(dataholder.images.where({type: 'kernel'}));
        scope.ramdisks.add(dataholder.images.where({type: 'ramdisk'}));
      });

      this.model.on('change', function() {
        self.model.set('advanced_show', true);
      });

      scope.kernels.on('reset change', self.render);
      scope.ramdisks.on('reset change', self.render);
      

      $(this.el).html(this.tpl)
      this.rView = rivets.bind(this.$el, scope);
      this.render();
		},

    render: function() {
      this.rView.sync();
    }
});
});
