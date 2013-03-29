define(['app'], function(app) {
  var self = this;
  return function(images, allowedFacetNames, localizer, explicitFacets, searchers) {
    var self = this;
    searchContext = self;
    
    // Make sure if the argument isn't there, that at least
    // something works
    if (!allowedFacetNames) {
      allowedFacetNames = [];
      images.forEach(function(img) {
        for (var key in img) {
          if (!isIgnored(key, img, allowedFacetNames)) {
            allowedFacetNames.push(key);
          }
        }
      })
    }
    
    var sortKeyList = function(list, keyName) {
      return _.chain(list)
              .sort()
              .uniq()
              .value()
    }

    function localize(name) {
      var result = name;
      if (localizer) {
        if (typeof localizer === 'function') {
          var localized = localizer(name);
          if (localized) {
            return localized;
          }
        } else if (localizer[name]) {
          return localizer[name];
        }
      }
      function capitalize(words) {
        for (var i = 0; i < words.length; i++) {
          words[i] = words[i].charAt(0).toUpperCase() + words[i].slice(1);
        }
        return words;
      }
      result = capitalize(name.split(/_/g)).join(' ');
      return result;
    }

    var siftKeyList = function(list, search) {
      console.log(list, search);
      return _.chain(list).filter(function(item) {
        return new RegExp('.*' + search + '.*').test(item);
      }).map(function(item) {
        return item === '' ? {value: item, label: 'None'} : item
      }).value();
    }

    function isIgnored(key, on, done) {
      return /^_.*/.test(key) 
              || typeof on[key] === 'function'
              || done[key];
    }

    function deriveFacets() {
      var derivedFacets = [];
      var done = {};
      images.toJSON().forEach(function(img) {
        for (var i=0; i < allowedFacetNames.length; i++) {
          var key = allowedFacetNames[i];
          if (isIgnored(key, img, done)) {
            continue;
          }
          derivedFacets.push({value: key, label: localize(key)});
          done[key] = true;
        }
      });
      return sortKeyList(derivedFacets, 'label');
    }

    function deriveMatches(facet, searchTerm) {
      if (searchers && searchers[facet]) {
        return searchers[facet].apply(self, [facet, searchTerm]);
      }
      console.log('FACET: ' + JSON.stringify(facet));
      if (explicitFacets && explicitFacets[facet]) {
        console.log('RETURNING EXPLICIT ' + JSON.stringify(explicitFacets[facet]));
        return explicitFacets[facet];
      }
      var result = [];
      var found = [];
      images.toJSON().forEach(function(img) {
        var val = img[facet];
        if (val && typeof val !== 'object' && typeof val !== 'function') {
          if (found.indexOf(val) < 0) {
            found.push(val);
            result.push({name: val, label: localize(val)});
          }
        }
      });
      result = sortKeyList(result, 'label')
      return siftKeyList(result, searchTerm);
    }

    this.images = images;
    this.filtered = images.clone();
    this.lastSearch = '';
    this.lastFacets = new Backbone.Model({});
    this.search = function(search, facets) {
      console.log("SEARCH", arguments);
      var jfacets = facets.toJSON();
      var results = self.images.filter(function(model) {
        return _.every(jfacets, function(facet) {
          if (searchers && searchers[facet.category]) {
            return searchers[facet.category].apply(self, [facet, search, images]);
          }
          var test = new RegExp('.*' + facet.value + '.*').test(model.get(facet.category));
          return test;
        });
      }).map(function(model) {
        return model.toJSON();
      });
      console.log(results);
      self.filtered.reset(results);
    }
    
    this.facetMatches = function(callback) {
      callback(deriveFacets());
    }
    
    this.valueMatches = function(facet, searchTerm, callback) {
      callback(deriveMatches(facet, searchTerm))
    }

//    images.on('change reset', updateKeyLists);
  }
});
