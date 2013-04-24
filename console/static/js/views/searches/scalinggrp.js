define([
  'views/searches/generic',
  'views/searches/tagsearch'
], function(Search, TagSearch) {
  return function(sgroups) {
    var config = {
      facets: ['all_text', 'launch_config_name', 'health_check_type', 'availability_zones'],
      localize: {
        launch_config_name: 'Launch Config',
        EC2 : 'Compute'
      }
    }
    return new Search(sgroups, new TagSearch(config, sgroups));
  }
});
