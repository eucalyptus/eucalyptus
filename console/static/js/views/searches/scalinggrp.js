define([
    'views/searches/generic',
], function(Search) {
    return function(images) {
      return new Search(images, ['launch_config_name', 'health_check_type', 'availability_zones'], {}, null);
    }
});
