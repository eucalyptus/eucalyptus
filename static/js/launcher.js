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
  $.widget('eucalyptus.launcher', $.eucalyptus.eucawidget, {
    options : { },
    _create : function() { },
    _init : function() {
      // read template and attach to the widget element
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #launchWizardTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_launcher)));
      var $launcher = $wrapper.children().first();
      var $launcherHelp = $wrapper.children().last();
      thisObj.element.addClass('launch-wizard');

      // make section headers
      thisObj._makeSectionHeader($launcher);
 
      // make section buttons 
      thisObj._makeSectionButton($launcher);
      // make sections (image, type, security, advanced options)

      thisObj._makeImageSection($launcher.find('#launch-wizard-image'));
      thisObj._makeTypeSection($launcher.find('#launch-wizard-type'));
      thisObj._makeSecuritySection($launcher.find('#launch-wizard-security'));
      thisObj._makeAdvancedSection($launcher.find('#launch-wizard-advanced'));
      $launcher.appendTo(thisObj.element);
     },
    _destroy : function() { },

    _makeSectionHeader : function($launcher) {
      var thisObj = this;
      var $header = $launcher.find('#launch-wizard-image-header');
      $header.append($('<a>').attr('href', '#').html('*'+launch_instance_section_header_image).click( function(e) {
        var imgSection = $launcher.find('#launch-wizard-image-contents');
        thisObj._selectedSection.slideToggle('fast');
        imgSection.slideToggle('fast');
        thisObj._selectedSection = imgSection;
      }));
      $header = $launcher.find('#launch-wizard-type-header');
      $header.append($('<a>').attr('href', '#').html('*'+launch_instance_section_header_type).click(function(e) {
        var typeSection = $launcher.find('#launch-wizard-type-contents');
        thisObj._selectedSection.slideToggle('fast');
        typeSection.slideToggle('fast');
        thisObj._selectedSection = typeSection;
      }));
      $header = $launcher.find('#launch-wizard-security-header');
      $header.append($('<a>').attr('href', '#').html('*'+launch_instance_section_header_security).click(function(e){
        var secSection = $launcher.find('#launch-wizard-security-contents');
        thisObj._selectedSection.slideToggle('fast');
        secSection.slideToggle('fast');
        thisObj._selectedSection = secSection;
      }));
      $header = $launcher.find('#launch-wizard-advanced-header');
      $header.append($('<a>').attr('href', '#').html(launch_instance_section_header_advanced).click(function(e){
        var advSection = $launcher.find('#launch-wizard-advanced-contents');
        thisObj._selectedSection.slideToggle('fast');
        advSection.slideToggle('fast');
        thisObj._selectedSection = advSection;
      }));
    },
 
    _makeSectionButton : function($launcher) {
      var thisObj = this;
      var $imageBtn = $launcher.find('#launch-wizard-buttons-image');
      $imageBtn.append($('<button>').attr('id', 'launch-wizard-buttons-image-next').html(launch_instance_btn_next_type).click( function(e){
        var typeSection = $launcher.find('#launch-wizard-type-contents');
        thisObj._selectedSection.slideToggle('fast');
        typeSection.slideToggle('fast');
        thisObj._selectedSection = typeSection;
      }));
      var $typeBtn = $launcher.find('#launch-wizard-buttons-type');
      $typeBtn.append($('<button>').attr('id', 'launch-wizard-buttons-type-next').html(launch_instance_btn_next_security).click( function(e){
        var secSection = $launcher.find('#launch-wizard-security-contents');
        thisObj._selectedSection.slideToggle('fast');
        secSection.slideToggle('fast');
        thisObj._selectedSection = secSection;
      }));
      var $securityBtn = $launcher.find('#launch-wizard-buttons-security');
      $securityBtn.append(
        $('<ul>').append(
          $('<li>').append($('<button>').attr('id','launch-wizard-buttons-security-launch').html(launch_instance_btn_launch).click( function(e){
        })),
          $('<li>').append($('<span>').text('Or:'), $('<a>').attr('href','#').html(launch_instance_btn_next_advanced).click( function(e){
            var advSection = $launcher.find('#launch-wizard-advanced-contents');
            thisObj._selectedSection.slideToggle('fast');
            advSection.slideToggle('fast');
            thisObj._selectedSection = advSection;
        })))); 
      var $advancedBtn = $launcher.find('#launch-wizard-buttons-advanced');
      $advancedBtn.append(
        $('<button>').attr('id', 'launch-wizard-buttons-advanced-launch').html(launch_instance_btn_launch).click( function(e){
        })); 
    },
    _selectedSection : null,
    _imageTable : null,
    _inferImageName : function(manifest, desc){
     // Regex '$distro[seperator]$version' 
      var inferMap = 
        {'rhel5':new RegExp('(rhel|redhat).5','ig'),
         'rhel6':new RegExp('(rhel|redhat).6','ig'),
         'rhel':new RegExp('(rhel|redhat)','ig'),
         'centos5':new RegExp('centos.5','ig'),
         'centos6':new RegExp('centos.6','ig'),
         'centos':new RegExp('centos','ig'),
         'lucid': new RegExp('(lucid|ubuntu.10[\\W\\s]04)','ig'),
         'precise':new RegExp('(precise|ubuntu.12[\\W\\s]04)','ig'),
         'ubuntu':new RegExp('ubuntu','ig'),
         'debian' :new RegExp('debian','ig'), 
         'linux' : new RegExp('linux','ig'),
         'windows' :new RegExp('windows','ig'),
        };
      var name = '';
      for (key in inferMap){
        var reg = inferMap[key];
        if(reg.test(manifest) || reg.test(desc)){
          name = key;
          break;
        }
      }
      return name;
    },
    _makeImageSection : function($section){ 
      var thisObj = this;
      var $content = $section.find('#launch-wizard-image-main-contents');

      var $table = $content.find('table');
      var drawCallback = function(oSettings){
        $section.find('table tbody').find('tr').each(function(index, tr) {
        // add custom td handlers
          var $currentRow = $(tr);
          if(!$currentRow.data('events') || !('click' in $currentRow.data('events'))){
            $currentRow.unbind('click').bind('click', function (e) {
              var $selectedRow = $currentRow; 
              $section.find('table tbody').find('tr.selected-row').each(function(){
                $(this).toggleClass('selected-row');
              });
              $(this).toggleClass('selected-row');
              e.stopPropagation();
              thisObj._setSummary('image', $currentRow.find('div').first().clone());
            });
          }
        });
      };
      var dtArg = { 
          "sAjaxSource": "../ec2?Action=DescribeImages",
          "bSortClasses" : false,
          "bSortable" : false,
          "oLanguage" : { "sProcessing": "<img src=\"images/dots32.gif\"/> &nbsp; <span>Loading...</span>", 
                             "sLoadingRecords": ""},
          "aoColumns": [
             { // platform
               "fnRender" : function(oObj) { 
                 var nameMap = {
                   'rhel5' : 'RHEL 5',
                   'rhel6' : 'RHEL 6',
                   'rhel' : 'RHEL',
                   'centos5' : 'CENTOS 5',
                   'centos6' : 'CENTOS 6',
                   'centos' : 'CENTOS',
                   'lucid' : 'UBUNTU LUCID(10.04)',
                   'precise' : 'UBUNTU PRECISE(12.04)',
                   'ubuntu' : 'UBUNTU',
                   'debian' : 'DEBIAN',
                   'linux' : 'LINUX',
                   'windows' : 'WINDOWS' ,
                 };
                 var emi = oObj.aData.id;
                 var desc = oObj.aData.description ? oObj.aData.description : '';
                 var name = 'Machine Image';
                 var imgKey = thisObj._inferImageName(oObj.aData.location, desc);
                 if (imgKey)
                   name = nameMap[imgKey];
                 var cell = '<div class="'+imgKey+'"><div class="image-id">'+emi+'</div> <div class="image-name">'+name+'</div> <div class="image-description">'+desc+'</div></div>';
                 return cell;
               }
             },
             {
               "bVisible": false,
               "fnRender" : function(oObj){
                 if(!oObj.aData.platform)
                   return 'linux';
                 else
                   return oObj.aData.platform;
               }
             },
             {
               "bVisible": false,
               "fnRender" : function(oObj){
                 if(oObj.aData.ownerId ==='000000000001')
                   return 'all';
                 else
                   return 'me';
               }
             },
             {
               "bVisible": false,
               "mDataProp": "type",             
             }
           ],
           "sDom" : "<\"#filter-wrapper\"<\"#platform-filter\"><\"clear\"><\"#owner-filter\"><\"clear\">f><\"#table-wrapper\"tr>", 
           "bProcessing" : true,
           "sAjaxDataProp" : "results",
           "bAutoWidth" : false,
           "fnDrawCallback" : function( oSettings ) {
              drawCallback(oSettings);
            }
          };
      this._imageTable = $table.dataTable(dtArg);
 
      $section.find('#filter-wrapper').prepend($('<span>').html(launch_instance_image_table_refine));
      $section.find('#table-wrapper').prepend($('<span>').html(launch_instance_image_table_header));

      $.fn.dataTableExt.afnFiltering = [];
      $.fn.dataTableExt.afnFiltering.push(
        function( oSettings, aData, iDataIndex ) {
          if (oSettings.sInstance !== $table.attr('id'))
            return true;
          return aData[3] === 'machine';
        }); // display only machine images

      var filters = [{name:"platform", options: ['all', 'linux','windows'], text: [launch_instance_image_table_platform_all, launch_instance_image_table_platform_linux, launch_instance_image_table_platform_windows], filter_col:1}, 
                   {name:"owner", options: ['all', 'me'], text: [launch_instance_image_table_owner_all, launch_instance_image_table_owner_me], filter_col:2}];
      $.each(filters, function (idx, filter){
        var $filter = $section.find('#'+filter['name']+'-filter');
        $filter.addClass('euca-table-filter');
          $filter.append(
            //$('<span>').addClass('filter-label').html(table_filter_label),
            $('<select>').attr('id',filter['name']+'-selector'));
          var $selector = $filter.find('#'+filter['name']+'-selector');
           
          for (i in filter.options){
            var option = filter.options[i];
            var text = (filter.text&&filter.text.length>i) ? filter.text[i] : option; 
            $selector.append($('<option>').val(option).text(text));
          }
         
          if(filter['filter_col']){
            $.fn.dataTableExt.afnFiltering.push(
	      function( oSettings, aData, iDataIndex ) {
                if (oSettings.sInstance !== $table.attr('id'))
                  return true;
                var selectorVal = thisObj.element.find('select#'+filter['name']+'-selector').val();
                if(filter['alias'] && filter['alias'][selectorVal]){
                  var aliasTbl = filter['alias'];
                  return aliasTbl[selectorVal] === aData[filter['filter_col']];
                }else if ( selectorVal !== filter['options'][0] ){ // not 'all'
                    return selectorVal === aData[filter['filter_col']];
                }else
                  return true;
            });
          }
          $selector.change( function() { thisObj._imageTable.fnDraw(); } );
      });

      $content = $section.find('#launch-wizard-image-contents').slideToggle('fast');
      thisObj._selectedSection = $content;
    },
   
    _makeTypeSection : function($section){
      var $content = $section.find('#launch-wizard-type-main-contents');
      $.each($content.children(), function(idx, child){
        $(child).html('type-contents'); 
      });
    },

    _makeSecuritySection : function($section) {
      var $content = $section.find('#launch-wizard-security-main-contents');
      $.each($content.children(), function(idx, child){
        $(child).html('security-contents'); 
      });
    },

    _makeAdvancedSection : function($section) { 
      var $content = $section.find('#launch-wizard-advanced-main-contents');
      $.each($content.children(), function(idx, child){
        $(child).html('advanced-contents'); 
      });
    },

    _setSummary : function(section, content){
      var thisObj = this;
      var $summary = thisObj.element.find('#launch-wizard-summary');
      var $section = $summary.find('#summary-'+section);
      $section.children().detach(); 
      $section.append(content);
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
