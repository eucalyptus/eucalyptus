define([
  'views/searches/generic',
  'views/searches/tagsearch'
], function(Search, TagSearch) {
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
        all_text: all_text_facet,
        assignment: assignment_facet,
        assigned: assigned_facet,
        unassigned: unassigned_facet
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
