define(['app'], function(app) {
  var self = this;
  return function(images, excludedKeys, localizer, explicitFacets) {
    var self = this;
    searchContext = self;

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
              || (excludedKeys && excludedKeys.indexOf(key) > 0)
              || done[key];
    }

    function deriveFacets() {
      var derivedFacets = [];
      var done = {};
      images.toJSON().forEach(function(img) {
        for (var key in img) {
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
      if (explicitFacets && explicitFacets[facet]) {
        return explicitFacets[facet];
      }
      var result = [];
      var found = [];
      images.toJSON().forEach(function(img) {
        var val = img[facet];
        console.log(facet + ' VAL ' + val + " for " + JSON.stringify(img));
        if (val && typeof val !== 'object' && typeof val !== 'function') {
          if (found.indexOf(val) < 0) {
            found.push(val);
            result.push({name: val, label: localize(val)});
          }
        }
      });
      console.log('DERIVE MATCHES FOR ' + facet + " and " + searchTerm + " gets " + JSON.stringify(result));
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
        return _.every(jfacets, function(item) {
          var test = new RegExp('.*' + item.value + '.*').test(model.get(item.category));
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
