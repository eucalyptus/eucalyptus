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

    // Security params
    security_keyname: '',
    security_group: '',

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
    },

  });
});
