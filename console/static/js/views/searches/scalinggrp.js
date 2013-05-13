define([
  'views/searches/generic',
  'views/searches/tagsearch'
], function(Search, TagSearch) {
  return function(sgroups) {
    var config = {
      facets: ['all_text', 'launch_config_name', 'health_check_type', 'availability_zones'],
      localize: {
        all_text: all_text_facet,
        launch_config_name: launch_config_name_facet,
        health_check_type: health_check_type_facet,
        EC2: EC2_facet,
        availability_zones: availability_zones_facet
      }
    }
    return new Search(sgroups, new TagSearch(config, sgroups));
  }
});
