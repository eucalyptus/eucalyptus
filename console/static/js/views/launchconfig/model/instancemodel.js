define([], function() {
  return Backbone.Model.extend({
    // Image ID
    image_id: '',
    image_location: '',
    image_description: '',
    image_platform: '',


    // Type definition
    type_number: '',
    type_names: [],
    type_size: '',
    type_zone: '',
    type_balancer: '',
    type_tags: {},
    type_show: '',

    // Security params
    security_keyname: '',
    security_group: '',
    security_group_name: '',
    security_group_rules: '',

    // Advanced
    user_data: '',
    user_file: '',
    kernel_id: '',
    ramdisk_id: '',
    monitoring: '',
    network: '',

    storage_volume: '',
    storage_mapping: '',
    storage_snapshot: '',
    storage_size: '',
    storage_delete: '',

    initialize: function() {
        this.type_show = '';
        this.type_hasTags = 'false';
        this.type_hasNames = 'false';
        this.image_iconclass = 'linux';
        this.type_tags = new Backbone.Collection();
        this.security_group = new Backbone.Model();
    },

  });
});
