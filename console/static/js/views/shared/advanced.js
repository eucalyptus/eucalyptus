define([
  'app',
	'dataholder',
  'text!./advanced.html!strip',
  'rivets',
  './model/blockmap'
], function( app, dataholder, template, rivets, blockmap ) {
	return Backbone.View.extend({
    tpl: template,
    title: app.msg("launch_instance_section_header_advanced"),
    isOptional: true,
    optionLinkText: app.msg('launch_instance_btn_next_advanced'),

    launchConfigErrors: new Backbone.Model({volume_size: ''}),
    theImage: null,
    
    initialize : function() {

      var self = this;
      self.theImage = self.options.image;
      var scope = {
        advancedModel: self.model,
        kernels: new Backbone.Collection(dataholder.images.where({type: 'kernel'})), 
        ramdisks: new Backbone.Collection(dataholder.images.where({type: 'ramdisk'})),
        enableMonitoring: true,
        privateNetwork: false,
        blockDeviceMappings: self.options.blockMaps,
        enableStorageVolume: true,
        enableMapping: true,
        enableSnapshot: true,
        deleteOnTerm: true,

        snapshots: function() {
            var ret = [{name:'None', id:null}];
            dataholder.snapshots.each(function(s) {
              ret.push({id:s.get('id'), name:s.get('id')+' ('+s.get('volume_size')+' GB)'});
            });
            return ret;
        },

        setKernel: function(e, obj) {
          self.model.set('kernel_id', e.target.value);
        },

        isKernelSelected: function(obj) { 
          if (self.model.get('kernel_id') == obj.kernel.get('id')) {
            return true;
          } 
          return false;
        },

        isRamdiskSelected: function(obj) { 
          if (self.model.get('ramdisk_id') == obj.ramdisk.get('id')) {
            return true;
          } 
          return false;
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
          if (obj.volume.get('device_name') == '/dev/sda') {
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
            // not sure what I was thinking here - partitions are not involved! Mock data have them?
            //var drive = model.get('device_name').replace(/\/dev\/([a-z]*)([0-9]{1,2})/, '$1');
            //var partition = model.get('device_name').replace(/\/dev\/([a-z]*)([0-9]{1,2})/, '$2');
            //return drive + (++partition);
            
            var device = model.get('device_name');
            var suffix = device.substring(device.length-1).charCodeAt(0);
            var newdev = device.substring(5, device.length-1) + String.fromCharCode(suffix+1);
            return newdev;
          } else {
            return 'sda';
          }
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
//        self.model.set('advanced_show', true);
      });

      this.model.on('change:user_data_text', function(e) {
          self.model.set('user_data', $.base64.encode(e.get('user_data_text')));
      });

      scope.kernels.on('reset change', self.render);
      scope.ramdisks.on('reset change', self.render);
      

      $(this.el).html(this.tpl)
      this.rView = rivets.bind(this.$el, scope);
      this.render();
      // this.model.set('fileinput', this.$el.find('#launch-wizard-advanced-input-userfile'));
       var fileinputel = this.$el.find('#launch-wizard-advanced-input-userfile');
       $(fileinputel).change(function(e) {
        self.model.set('files', this.files);
       });
       this.model.set('fileinput', function() { return fileinputel; });
    },

    render: function() {
      this.$el.find('#launch-wizard-advanced-storage').hide();
      if (this.theImage != null) {
        if (this.theImage.get('root_device_type') != undefined) {
          root_device_type = this.theImage.get('root_device_type');
          if (root_device_type == 'ebs') {
            this.$el.find('#launch-wizard-advanced-storage').show();
          }
        }
      }

      this.rView.sync();
    },

    focus: function() {
      this.model.set('advanced_show', true);
    }
  });
});
