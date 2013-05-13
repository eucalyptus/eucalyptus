define([
  'views/searches/generic',
  'views/searches/tagsearch',
  'app'
], function(Search, TagSearch, app) {
  return function(sgroups) {
    
    var USER_ID = '072279894205';
    
    app.data.sgroup.each(function(securityGroup) {
      securityGroup = securityGroup.toJSON();
      if ('default' === securityGroup.name) {
        USER_ID = securityGroup.owner_id;
      }
    });
    
    var config = {
      facets: ['all_text', 'owner_id']
      
      ,localize: {
        all_text: all_text_facet,
        owner_id: owner_facet
      }
      ,match : {
        owner_id : function(search, item, add) {
          add('me')
        }
      }
      ,search : {
        owner_id : function(search, facetSearch, item, itemsFacetValue, hit) {
          if (itemsFacetValue === USER_ID) {
            hit();
          }
        }
      }
    }

    return new Search(sgroups, new TagSearch(config, sgroups));
  }
});
