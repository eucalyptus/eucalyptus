define([
  'views/searches/generic',
  'views/searches/tagsearch',
], function(Search, TagSearch) {
  return function(sgroups) {
    
    var USER_ID = '072279894205'; // XXX where do I get this?
    
    var config = {
      facets: ['all_text', 'owner_id']
      
      ,localize: {
        owner_id: 'Owner'
      }
      ,match : {
        owner_id : function(search, item, add) {
          add('me')
        }
      }
      ,search : {
        owner_id : function(search, facetSearch, item, itemsFacetValue, hit) {
          if (facetValue === USER_ID) {
            hit();
          }
        }
      }
    }

    return new Search(sgroups, new TagSearch(config, sgroups));
  }
});
