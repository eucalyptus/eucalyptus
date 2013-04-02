define([
	'dataholder',
  'text!./advanced.html!strip',
  'rivets',
  './model/blockmap'
	], function( dataholder, template, rivets, blockmap ) {
	return Backbone.View.extend({
    title: 'Advanced',
    launchConfigErrors: new Backbone.Model({size: ''}),
    
     

		initialize : function() {

      var self = this;
      var scope = {
        userData: '',
        userFilename: '',
        kernels: '',
        ramdisks: '',
        enableMonitoring: true,
        privateNetwork: false,
        volumes: '',
        snapshots: dataholder.snapshot,
        blockDeviceMappings: self.options.blockMaps,
        enableStorageVolume: false,
        volumeName: '',
        enableMapping: false,
        deviceName: '',
        enableSnapshot: false,
        snapshot: '',
        size: '',
        deleteOnTerm: true,

        setKernel: function() {
          self.model.set('ramdisk_id', e.target.value);
        },

        setDelOnTerm: function() {

        },

        setPrivateNetwork: function() {

        },

        setMonitoring: function(e, item) {
            self.model.set('instance_monitoring', e.target.value);
        },

        setRamDisk: function() {

        },

        setStorageVolume: function(e, obj) {
          var m = new blockmap();
          self.launchConfigErrors.clear();
          m.on('validated:invalid', function(o, errors) { self.launchConfigErrors.set('size', errors.size)});

          var tr = $(e.target).closest('tr');
          var vol = tr.find('.launch-wizard-advanced-storage-volume-selector').val();
          var dev = "/dev/" + tr.find('.launch-wizard-advanced-storage-mapping-input').val();
          var snap = tr.find('.launch-wizard-advanced-storage-snapshot-input').val();
          var size = tr.find('.launch-wizard-advanced-storage-size-input').val();
          var del = tr.find('.launch-wizard-advanced-storage-delOnTerm').prop('checked');

          m.set({
              map_name: dev,
              status: null,
              __obj_name__: "BlockDeviceType",
              attach_time: null,
              no_device: false,
              volume_id: null,
              connection: null,
              snapshot_id: snap,
              size: size,
              ebs: "",
              delete_on_termination: del,
              ephemeral_name: null    
          });

          m.validate();
          if (m.isValid()) {
            this.blockDeviceMappings.add(m);
          }
        },

        delStorageVol: function(e, obj) {
          console.log('DEL', obj);
          this.blockDeviceMappings.remove(obj.volume); 
        },

        getVolLabel: function(obj) {
          if (obj.volume.get('map_name') == '/dev/sda1') {
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
          if(undefined !== model) {
            var drive = model.get('map_name').replace(/\/dev\/([a-z]*)([0-9]{1,2})/, '$1');
            var partition = model.get('map_name').replace(/\/dev\/([a-z]*)([0-9]{1,2})/, '$2');
            return drive + (++partition);
          }
        },

        launchConfigErrors: self.launchConfigErrors

      };

      scope.blockDeviceMappings.on('change add reset remove', function() {self.render();});
      $(this.el).html(template)
      this.rView = rivets.bind(this.$el, scope);
      this.render();
		},

    render: function() {
      this.rView.sync();
    }
});
});
