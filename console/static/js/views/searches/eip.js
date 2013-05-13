define([
  'app',
  'views/searches/generic',
  'views/searches/tagsearch'
], function(app, Search, TagSearch) {
  return function(addresses) {

    var config = {
      facets: ['all_text', 'assignment'],
      match: {
        assignment: function(search, item, add) {
          if (!item['instance_id']) {
            add('unassigned');
          } else {
            // FIXME - According to David K., there is a 
            // simple algorithm to compute whatever the
            // value should be
            add('assigned')
          }
        }
      },
      localize: {
        assigned : app.msg('search_facet_eip_assigned'), //'Assigned',
        unassigned: app.msg('search_facet_eip_unassigned'), //'Unassigned'
        all_text: app.msg('search_facet_alltext'),
        assignment: app.msg('search_facet_eio_assignment')
      },
      search: {
        assignment: function(search, facetSearch, item, itemsFacetValue, hit) {
          if (facetSearch === 'unassigned' && !item.instanceId) {
            hit();
          } else if (facetSearch === 'assigned' && item.instanceId) {
            hit();
          }
          // Return true so the standard search code 
          // does not run
          return true;
        }
      }
    }

    return new Search(addresses, new TagSearch(config, addresses));
  }
});
