define([
  'views/searches/generic',
], function(Search) {
  return function(images) {
    return new Search(images, ['all_text', 'availability_zones', 'launch_config_name', 'name', 'health_check_type'], {}, null);
  }
});
