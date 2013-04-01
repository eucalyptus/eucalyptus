define([], function() {
// Wizard (Constructor)
// ====================
// Creates a new wizard.  Pages is an optional pre-populated array of views.
// navigationController is an optional function which determines whether
// navigation is premitted.
//
// A wizard can have pages added to it.  A "page" is either a backbone view
// or a function which will create one on demand.
  function Wizard(pages, navigationController, closedViewFactory) {
    var self = this;
    if (typeof pages === 'function') {
      closedViewFactory = navigationController;
      navigationController = pages;
      pages = [];
    }
    var pages = pages || [];
    var position = -1;

    if (closedViewFactory && typeof closedViewFactory !== 'function') {
      throw new Error("closedViewFactory must be undefined or a function: "
              + closedViewFactory + " - " + typeof closedViewFactory);
    }

    // size (property)
    // ---------------
    // The number of pages
    self.__defineGetter__('size', function() {
      return pages.length;
    });

    function doAdd(viewOrFactoryForView, viewWhenClosed) {
      switch (typeof viewOrFactoryForView) {
        case 'function' :
        case 'object' :
          break;
        default :
          throw new Error("viewOrFactoryForView must be an object or function")
      }
      switch (typeof viewWhenClosed) {
        case 'function' :
        case 'undefined' :
        case 'object' :
          break;
        default :
          throw new Error('viewWhenClosed must be an object, function or undefined');
      }
      if (typeof viewOrFactoryForView !== 'function' &&
              typeof viewOrFactoryForView !== 'object') {
        throw new Error("Must be a function or an object");
      }
      var toAdd = {};

      toAdd.__defineGetter__("view", function() {
        return viewOrFactoryForView;
      });
      toAdd.__defineGetter__('closedView', function() {
        return viewWhenClosed ? viewWhenClosed : closedViewFactory;
      });
      pages.push(toAdd);
      return self;
    }

    // add
    // ---
    // Add a page to this view, including an optional view that should
    // be used when the page is not active
    self.add = function(viewOrFactoryForView, viewWhenClosed) {
      doAdd(viewOrFactoryForView, viewWhenClosed)
    }


    // hasNext (property)
    // ------------------
    // Determine if there is a next page
    self.__defineGetter__("hasNext", function() {
      return position < pages.length - 1;
    });

    // hasPrev (property)
    // ------------------
    // Determine if there is a previous page
    self.__defineGetter__("hasPrev", function() {
      return position > 0;
    });

    // current (property)
    // ------------------
    // Get the current active page.  Will be null if the
    // wizard has never been shown
    self.__defineGetter__("current", function() {
      if (position === -1) {
        return null;
      }
      if (typeof pages[position] === 'undefined') {
        return null;
      }
      return pages[position].view;
    });

    // position (property)
    // ------------------
    // Get the current integer position
    self.__defineGetter__('position', function() {
      return position;
    });

    // currentView (property)
    // ----------------------
    // Get the view that should currently be used for an index.  If it is
    // the current  position, returns the view;  if not, returns the closed
    // view (if any)
    self.currentView = function(index) {
      return index === position ? self.view(index) : self.closedView(index);
    }

    // view (property)
    // ---------------
    // Get the view for a given index
    self.view = function(index) {
      if (index < 0) {
        throw new Error("Index " + index + " is less than 0")
      }
      if (index >= pages.length) {
        throw new Error("Index " + index + " larger than size " + pages.size)
      }
      return pages[index].view;
    };

    // closedView (property)
    // ---------------
    // Get the closed view for a given index
    self.closedView = function(index) {
      if (index < 0) {
        throw new Error("Index " + index + " is less than 0")
      }
      if (index >= pages.length) {
        throw new Error("Index " + index + " larger than size " + pages.size)
      }
      return pages[index].closedView;
    }

    // views (property)
    // ---------------
    // Get the list of views for this wizard.  This will be "closed" views for
    // all but the current index
    self.__defineGetter__('views', function() {
      var res = [];
      for (var i = 0; i < pages.length; i++) {
        res.push(i === position ? pages[i].view : pages[i].closedView);
      }
      return res;
    })

    // show
    // ----
    // Initialize this wizard to zero if necessary and return the current view
    self.show = function() {
      if (position === -1) {
        position = 0;
      }
      return self.current;
    }

    // goTo
    // ----
    // Navigate to a specific page.  Throws an error
    // if out of range.
    self.goTo = function(index) {
      if (index < 0 || index >= pages.length) {
        throw new Error("Out of range: " + index + " max " + pages.length);
      }
      var offset = -(position - index);
      if (!navigationController || navigationController(offset)) {
        if (typeof self.current === 'object' && typeof self.current.isValid === 'function') {
            if (! self.current.isValid()) {
              return self.current;
            }
        }
        position = index;
      }
      return self.current;
    }

    // next
    // ----
    // Navigate to the next page and return its view.  Throws an error
    // if out of range.
    self.next = function() {
      if (position === pages.length - 1) {
        throw new Error("Nav past end: " + pages.length);
      }
      if (!navigationController || navigationController(1)) {
        position++;
      }
      return self.current;
    }

    // prev
    // ----
    // Navigate to the preceding page and return its view.  Throws an error
    // if out of range.
    self.prev = function() {
      if (position <= 0) {
        throw new Error("Nav before start: " + pages.length);
      }
      if (!navigationController || navigationController(-1)) {
        position--;
      }
      return self.current;
    }

    self.scrapeTitle = function(tpl) {
      var rex = /<h[1234][a-zA-Z0-9=\'\"\-\s]*>([^<>]*?)<\/h[1234]>/i;
      if (rex.test(tpl)) {
        return rex.exec(tpl)[1];
      }
      return "[failed to scrape title]";
    }

    // Creates a mapping of { a : '#a'} and so forth based on the arguments array
    function trivialMapping() {
      var result = {};
      for (var i = 0; i < arguments.length; i++) {
        result[arguments[i]] = '#' + arguments[i];

      }
      return result;
    }

    // Merge b's properties into a
    function merge(a, b) {

      if (b) {
        for (var key in b) {
          // avoid immediately resolving b's contents
          a.__defineGetter__(key, function() {
            return b[key];
          });
        }
      }
      return a;
    }

    // Append this to generated HTML IDs to avoid name clashes
    var uid = Math.floor(Math.random() * 65536) + '_' + new Date().getTime();

    self.makeView = function(options, template, mapping, scope) {
      // Assume default component names, but let mapping override them if it wants
      mapping = merge(trivialMapping('nextButton', 'prevButton', 'finishButton', 'problems', 'wizardContent', 'wizardAbove', 'wizardBelow', 'wizardSummary'), mapping || {});
      // Make sure options is defined
      options = options || {};
      // If there is no canFinish function, provide a default one that only
      // enables the Finish button on the last page
      if (typeof options.canFinish !== 'function') {
        options.canFinish = function(arr) {
          return position === pages.length - 1;
        };
      }
      if (typeof options.canAnimate === 'undefined') {
        options.canAnimate = false;
      }
      // Predefine click handlers, using the selectors provided in the mapping
      // (if any)
      var events = {};
      events['click ' + mapping.nextButton] = function() {
        //self.next();
        //this.render();
      };
      events['click ' + mapping.prevButton] = function() {
        self.prev();
        this.render();
      };
      events['click ' + mapping.finishButton] = function() {
        if (options.finish) {
          options.finish()
        }
        this.render();
      };

      function closedViewName(i) {
        return 'closedView' + uid + '_' + i;
      }

      function addClickHandler(i) {
        events['click ' + '#' + closedViewName(i)] = function() {
          self.goTo(i);
          this.render();
        };
      }

      for (var i = 0; i < pages.length; i++) {
        addClickHandler(i);
      }

      var Result = Backbone.View.extend({
        animate: true,
        lastRendered: -2,
        initialize: function() {
          self.show();
          this.animate = true;
          this.$el.html(template);
          this.nextButton = this.$el.find(mapping.nextButton);
          this.prevButton = this.$el.find(mapping.prevButton);
          this.finishButton = this.$el.find(mapping.finishButton);
          this.problems = this.$el.find(mapping.problems);
          this.wizardContent = this.$el.find(mapping.wizardContent);
          this.wizardAbove = this.$el.find(mapping.wizardAbove);
          this.wizardBelow = this.$el.find(mapping.wizardBelow);
          this.summary = this.$el.find(mapping.wizardSummary);
          this.render();
        },
        render: function() {
          // Make sure there is something there
          var shouldAnimate = this.animate && self.position !== this.lastRendered;
          this.lastRendered = self.position;
          if (shouldAnimate) {
            this.wizardContent.slideUp(0);
          }
          var Type = self.current;
// console.log('TYPE', Type, typeof Type);
//          Type.el = this.wizardContent;
//          var page = new Type($(this.wizardContent));
          var page = Type;
          this.wizardContent.children().detach();
          this.wizardContent.append(page.$el);
          page.render();
          if (shouldAnimate) {
            this.wizardContent.slideDown("fast");
          }
          this.nextButton.attr('disabled', !self.hasNext);
          this.prevButton.attr('disabled', !self.hasPrev);

          var problems = [];
          var finishable = options.canFinish(self.position, problems);
          this.finishButton.attr('disabled', !finishable);
          this.problems.empty();
          for (var i = 0; i < problems.length; i++) {
            var problem = problems[i];
            var problemLabel = $('<span>', {
              text: problem,
              id: 'problem-' + i,
              class: 'error-msg wizardProblem'
            });
            this.problems.append(problemLabel);
          }
          if (options.hideDisabledButtons) {
            this.nextButton.attr('style', self.hasNext ? 'display: inline' : 'display: none')
            this.prevButton.attr('style', self.hasPrev ? 'display: inline' : 'display: none')
            this.finishButton.attr('style', finishable ? 'display: inline' : 'display: none')
          }
          if (options.finishText) {
            this.finishButton.text(options.finishText);
          }

          this.wizardAbove.empty();
          this.wizardBelow.empty();

          if (options.titler) {
            this.nextButton.text(options.titler(self.position, self.current))
          }

          function addClosed(i, toWhat) {
            var CV = self.closedView(i);
            var container = $('<div>', {
              id: closedViewName(i)
            });
            var element = new CV($(this.wizardBelow)).$el;
            container.append(element);
            toWhat.append(container);
          }

          for (var i = 0; i < self.position; i++) {
            addClosed(i, this.wizardAbove);
          }
          for (var i = self.position + 1; i < pages.length; i++) {
            addClosed(i, this.wizardBelow);
          }

          if(options.summary) {
            //this.summary.empty();
            this.summary.append(options.summary.$el);
          }
        },
        events: events
      });
      return Result;
    };

    self.viewBuilder = function(wizardTemplate, scope) {
      var bldr = this;
      var mapping = {};
      var templates = [];

      function titler(position) {
        var result = "Next";
        if (self.position < pages.length && templates[position + 1]) {
          result += ': ' + templates[position + 1].titleStr;
        }
        return result;
      }
      var options = {titler: titler};

      function genericView(tpl) {
        return Backbone.View.extend({
          initialize: function() {
            this.render();
          },
          render: function() {
            $(this.el).html(tpl)
          }
        });
      }

      function genericTitle(name, index) {
        return Backbone.View.extend({
          initialize: function() {
            this.render();
          },
          render: function() {
            var container = $('<div>', {
              class: 'wizardTitle wizard-step ' + (self.position > index ? 'wizardTitleLink' : '')
            });
            var h3 = $('<h3>', {
              id: 'wizardClosed-' + index,
              class: self.position > index ? 'wizardTitleLink required-label' : ''
            });
            h3.text(name)
            container.append(h3);
            $(this.el).append(container);
          }
        });
      }

      this.add = function(template) {
        CaegoryTest.php(template, genericTitle(self.scrapeTitle(template), pages.length));
        // templates.push(template);
        return bldr;
      }

      this.add = function(template, closedView) {
        var tObj;
        var cView;
            var titleStr;
        if (typeof template === 'string') {
            tObj = new (genericView(template));
            titleStr = self.scrapeTitle(template);
        } else if (typeof template === 'function') {
            tObj = new template({scope: scope, model: this.pageModel});
        } else if (typeof template === 'object') {
            tObj = template;
        }

        if (tObj.title) {
            if (typeof tObj.title === 'string') {
                titleStr = tObj.title;
            } else if (typeof tObj.title === 'function') {
                titleStr = tObj.title();
            } else {
                titleStr = 'Page ' + pages.length + 1;
            }
        } 

        if (closedView) 	
            cView = closedView;
        else
            cView = genericTitle(titleStr, pages.length);

        tObj.cView = cView;
        tObj.titleStr = titleStr;
        doAdd(tObj, cView);

        templates.push(tObj);
        return bldr;
      }

      this.setHideDisabledButtons = function(hide) {
        options.hideDisabledButtons = hide;
        return bldr;
      }

      this.setTitler = function(titler) {
        options.titler = titler;
        return bldr;
      }

      this.setFinishText = function(finishText) {
        options.finishText = finishText;
        return bldr;
      }

      this.setFinishChecker = function(canFinish) {
        options.canFinish = canFinish;
        return bldr;
      }

      this.finisher = function(finisher) {
        options.finish = finisher;
        return bldr;
      }

      this.summary = function(summary) {
        options.summary = summary;
        return bldr;
      }

      this.setPageModel = function(model) {
          this.pageModel = model;
        //for(i=0; i<templates.length; i++) {
        //  templates[i]['model'] = model;
        //}
        return bldr;
      }

      this.map = function(a, b) {
        mapping[a] = b;
      }

      this.build = function() {
        return self.makeView(options, wizardTemplate, mapping, scope);
      }

      return this;
    }
  }

// Modules, for tests with node
  if (typeof module !== 'undefined' && typeof module.exports !== 'undefined') {
    module.exports.Wizard = Wizard;
  }
  return Wizard;
});
