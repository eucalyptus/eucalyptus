define([
  'views/searches/generic',
], function(Search) {
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
          console.log("Search " + search + " ITEM " + item + " fv ", itemsFacetValue);
          if (facetValue === USER_ID) {
            hit();
          }
        }
      }
      /*
      ,facetsCustomizer: function(add, append) {
        add('dogs');
      }
      ,match: {dogs: function(searchTerm, img, add) {
          switch (facet) {
            case 'dogs' :
              {
                add(facet, 'poodle')
                add(facet, 'chihuahua')
                add(facet, 'schnauzer')
              }
              return false;
          }
        }
      }
      */
    }

    return new Search(sgroups, config);
  }
});
