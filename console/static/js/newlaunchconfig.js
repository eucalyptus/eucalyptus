/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

(function($, eucalyptus) {
  $.widget('eucalyptus.newlaunchconfig', $.eucalyptus.eucawidget, {
    options : { },
    _init : function() {
      var thisObj = this;
        require([
          'wizard',
          'text!views/launchconfig/template.html',
          'views/launchconfig/image',
          'views/launchconfig/type',
          'views/launchconfig/security',
          'views/launchconfig/advanced',
          'views/launchconfig/summary',
          'models/launchconfig',
          'views/launchconfig/model/imagemodel',
          'views/launchconfig/model/typemodel',
          'views/launchconfig/model/securitygroup',
          'views/launchconfig/model/keypair',
          'views/launchconfig/model/advancedmodel',
          'views/launchconfig/model/blockmaps',
          'views/launchconfig/model/snapshots'
        ], function(Wizard, wizardTemplate, page1, page2, page3, page4, summary, launchconfigModel, imageModel, typeModel, securityModel, keyPair, advancedModel, blockMaps, snapShots) {
          var wizard = new Wizard();

          function canFinish(position, problems) {
            // VALIDATE THE MODEL HERE AND IF THERE ARE PROBLEMS,
            // ADD THEM INTO THE PASSED ARRAY
            return position === 2;
          }

          function finish() {
            alert("Placeholder function for saving the new launch config.");
          }

          var launchConfigModel = new launchconfigModel();
          var imageModel = new imageModel();
          var typeModel = new typeModel();
          var securityModel = new securityModel();
          var keyModel = new keyPair();
          var advancedModel = new advancedModel();
          var blockMaps = new blockMaps();
          var snapShots = new snapShots();

          var viewBuilder = wizard.viewBuilder(wizardTemplate)
                  .add(new page1({model: imageModel, blockMaps: blockMaps}))
                  .add(new page2({model: typeModel}))
                  .add(new page3({model: securityModel, keymodel: keyModel}))
                  .add(new page4({model: advancedModel, blockMaps: blockMaps, snapshots: snapShots}))
                  .setHideDisabledButtons(true)
                  .setFinishText('Create launch configuration').setFinishChecker(canFinish)
                  .finisher(finish)
                  .summary(new summary( {imageModel: imageModel, typeModel: typeModel, securityModel: securityModel, keymodel: keyModel, advancedModel: advancedModel} ));
        //  var ViewType = wizard.makeView(options, wizardTemplate);
          var ViewType = viewBuilder.build();
          new ViewType({el: thisObj.element}) 
        });
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    _expandCallback : function(row){ 
       var $wrapper = $('');
      return $wrapper;
    },

/**** Public Methods ****/
    close: function() {
      cancelRepeat(tableRefreshCallback);
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
