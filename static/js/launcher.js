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
    launchParam : {
      emi : null,
      type : null,
      number : null,
      zone : null,
      keypair : null,
      sgroup: null, // this and above are the required fields
      kernel: null,
      ramdisk: null,
      data: null,
      private_addressing: false,
      device_map : [],
    },
    _create : function() { },
    _init : function() {
      // read template and attach to the widget element
      var thisObj = this;
      $.each(thisObj.launchParam, function(k,v){
        thisObj.launchParam[k] = null;
      });

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

      $launcher.find('#launch-wizard-cancel').find('a').click( function(e){
        var $container = $('html body').find(DOM_BINDING['main']);
        $container.maincontainer("changeSelected", e, {selected:'instance'});
      });
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
    _inferImageName : function(manifest, desc, platform){
      if(!platform)
        platform='linux';
      var name = platform;
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
      var $summary = '';
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

              var imgClass = $selectedRow.find('td div').attr('class');
              var imgName = $selectedRow.find('.image-name').first().html();
              var emi = $selectedRow.find('.image-id-arch').children().first().text();
              thisObj.launchParam['emi'] = emi;
              $summary =  $('<div>').addClass(imgClass).addClass('summary').append($('<div>').text(launch_instance_summary_platform), $('<span>').text(imgName));
            });
          }
        });
      };

      $section.find('#launch-wizard-buttons-image-next').click( function(e){
        thisObj._setSummary('image', $summary);
      });
      
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
                 var desc = oObj.aData.description ? oObj.aData.description : oObj.aData.location;
                 var arch = oObj.aData.architecture;
                 arch=arch.replace('i386', '32 bit')
                 arch=arch.replace('x86_64', '64 bit');

                 var name = '';
                 var imgKey = thisObj._inferImageName(oObj.aData.location, desc, oObj.aData.platform);
                 if(imgKey)
                   name = nameMap[imgKey];
                 var $cell = $('<div>').addClass(imgKey).append(
                               $('<div>').addClass('image-name').text(name), // should be linux, windows, or distros
                               $('<div>').addClass('image-id-arch').append($('<span>').text(emi), $('<span>').text(arch)),
                               $('<div>').addClass('image-description').text(desc)); 
                 
                 return $cell.wrap($('<div>')).parent().html();
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
              if($section.find('table tbody').find('tr.selected-row').length === 0)
                $section.find('table tbody').find('tr').first().trigger('click'); 
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
      var thisObj = this;
      var $content = $section.find('#launch-wizard-type-main-contents');
      var $size = $content.find('#launch-wizard-type-size');
      var $option = $content.find('#launch-wizard-type-options');
    
      var $list = $('<ul>').addClass('launch-wizard-type-size').html(launch_instance_type_size_header);
      var $legend = $('<div>').attr('id','launch-wizard-type-size-legend');
      var selectedType = 'm1.small';
      var instType ={};
      instType['m1.small'] = $.eucaData.g_session['instance_type']['m1.small'];
      instType['c1.medium'] = $.eucaData.g_session['instance_type']['c1.medium'];
      instType['m1.large'] = $.eucaData.g_session['instance_type']['m1.large'];
      instType['m1.xlarge'] = $.eucaData.g_session['instance_type']['m1.xlarge'];
      instType['c1.xlarge'] = $.eucaData.g_session['instance_type']['c1.xlarge'];

      $.each(instType, function(type, size){
        $list.append(
          $('<li>').addClass('instance-type-'+type.replace('.','_')).append(
            $('<a>').attr('href','#').text(type).click( function(){
              selectedType = type;
              var legend = type +' defaults: ' + size[0] + ' CPUs, '+size[1]+' memory(MB), '+size[2]+' disk(GB,root device)';  
              $size.find('#launch-wizard-type-size-legend').html(legend); 
            })));
      });
      $size.append($list, $legend); 
      $list.find('a').first().trigger('click');
      var numInstances = 1;
      var selectedZone = 'Any'
      $list = $('<ul>').addClass('launch-wizard-type-option').html(launch_instance_type_option_header);
      $list.append(
        $('<li>').append(
          launch_instance_type_option_numinstance,$('<input>').attr('id','launch-instance-type-num-instance').attr('type','text').change( function(e) {
            numInstances = $(this).val(); 
           })));
      $list.append($('<li>').append(launch_instance_type_option_az,$('<select>').attr('id','launch-instance-type-az')));

      $list.find('#launch-instance-type-num-instance').val('1');
      var $az = $list.find('#launch-instance-type-az');
      $az.append($('<option>').attr('value', 'Any').text(launch_instance_type_option_az_any));
      $az.change(function(e){
        selectedZone = $(this).val();
      });
      var results = describe('zone');
      for( res in results) {
        var azName = results[res].name;
        $az.append($('<option>').attr('value', azName).text(azName));
      }
      $option.append($list);

      $section.find('#launch-wizard-buttons-type-next').click(function(e) {
        var $summary = $('<div>').addClass(selectedType).addClass('summary').append(
          $('<div>').attr('id','summary-type-insttype').append($('<span>').text(launch_instance_summary_type), $('<span>').text(selectedType)),
          $('<div>').attr('id','summary-type-numinst').append($('<span>').text(launch_instance_summary_instances), $('<span>').text(numInstances)),
          $('<div>').attr('id','summary-type-zone').append($('<span>').text(launch_instance_summary_zone), $('<span>').text(selectedZone)));
        thisObj.launchParam['type'] = selectedType;
        thisObj.launchParam['number'] = numInstances;
        thisObj.launchParam['zone'] = selectedZone;
        thisObj._setSummary('type', $summary); 
      });
    },

    _makeSecuritySection : function($section) {
      var thisObj = this;
      var $content = $section.find('#launch-wizard-security-main-contents');
      $content.prepend($('<span>').html(launch_instance_security_header));
      var $keypair = $content.find('#launch-wizard-security-keypair');
      var $sgroup = $content.find('#launch-wizard-security-sgroup');

      $keypair.append(
        $('<div>').append(
          $('<span>').text(launch_instance_security_keypair),
          $('<select>').attr('id','launch-wizard-security-keypair-selector')),
        $('<div>').append('Or. ',$('<a>').attr('href','#').text(launch_instance_security_create_kp).click(function(e){
        })));
      $sgroup.append(
        $('<div>').append(
          $('<span>').text(launch_instance_security_sgroup),
          $('<select>').attr('id','launch-wizard-security-sg-selector')),
        $('<div>').append('Or. ',$('<a>').attr('href','#').text(launch_instance_security_create_sg).click(function(e){
        })),
        $('<div>').attr('id','launch-wizard-security-sg-detail'));
     
      var $kp_selector = $keypair.find('select');
      var results = describe('keypair');
      for( res in results) {
        var kpName = results[res].name;
        $kp_selector.append($('<option>').attr('value', kpName).text(kpName));
      }
      var $sg_selector = $sgroup.find('select');
      results = describe('sgroup');
      for(res in results){
        var sgName = results[res].name;
        $sg_selector.append($('<option>').attr('value',sgName).text(sgName)); 
      }
      $sg_selector.find('option').each(function(){
        if($(this).val() ==='default')
          $(this).attr('selected','selected');
      });
    
      var summarize = function(){
        var selectedKp = $kp_selector.val();
        var selectedSg = $sg_selector.val();
        thisObj.launchParam['keypair'] = selectedKp;
        thisObj.launchParam['sgroup'] = selectedSg;
        return $('<div>').append(
          $('<div>').attr('id','summary-security-keypair').text(launch_instance_summary_keypair+' '+selectedKp),
          $('<div>').attr('id','summary-security-sg').text(launch_instance_summary_sg+' '+selectedSg));
      }

      $section.find('#launch-wizard-buttons-security').find('ul a').click(function(e) {
        var $summary = summarize(); 
        thisObj._setSummary('security', $summary.clone().children()); 
      });
      $section.find('#launch-wizard-buttons-security').find('ul button').click(function(e){
        var $summary = summarize(); 
        thisObj._setSummary('security', $summary.clone().children()); 
        thisObj._launch();
        // and launch
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
      $section.removeClass('required-missing');
      $section.children().detach(); 
      $section.append(content);
    },

    _launch : function(){
      // validate
      var thisObj = this;
      var param = thisObj.launchParam;

      if(!param['emi'])
        return thisObj._showError('image');
      if(!param['type'])
        return thisObj._showError('type');
      if(!param['number'])
        return thisObj._showError('type');
      if(!param['zone'])
        return thisObj._showError('type');
      if(!param['keypair'])
        return thisObj._showError('security');
      if(!param['sgroup'])
        return thisObj._showError('security'); 
   
      //prepare for the actual request parameters
      var reqParam = 'ImageId='+param['emi'];
      reqParam += '&InstanceType='+param['type'];
      reqParam += '&MinCount='+param['number'];
      reqParam += '&MaxCount='+param['number'];
      if(param['zone'].toLowerCase() !== 'any')
        reqParam += '&Placement.AvailabilityZone='+param['zone'];
      reqParam += '&Placement.GroupName='+param['sgroup'];
      reqParam += '&KeyName='+param['keypair'];

      $.ajax({
          type:"GET",
          url:"/ec2?Action=RunInstances&" + reqParam,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:true,
          success: function(data, textStatus, jqXHR){
            if ( data.results ){
              var instances ='';
              $.each(data.results, function(idx, instance){
                instances += instance+' ';
              });
              instances = $.trim(instances);
              notifySuccess(null, instance_run_success + ' ' + instances);
              //TODO: move to instance page?
              var $container = $('html body').find(DOM_BINDING['main']);
              $container.maincontainer("changeSelected",null, {selected:'instance'});

            } else {
              notifyError(null, instance_run_error);
              //TODO: clear launch-instance wizard?
              var $container = $('html body').find(DOM_BINDING['main']);
              $container.maincontainer("clearSelected");
              $container.maincontainer("changeSelected",null, {selected:'launcher'});

            }
          },
          error: function(jqXHR, textStatus, errorThrown){
            notifyError(null, instance_run_error);
            var $container = $('html body').find(DOM_BINDING['main']);
            $container.maincontainer("clearSelected");
            $container.maincontainer("changeSelected",null, {selected:'launcher'});
          }
        });
    },
 
    _showError : function(step){
      var thisObj = this;
      var $summary = thisObj.element.find('#launch-wizard-summary');
      var $step = $summary.find('#summary-'+step);
      $step.addClass('required-missing');
      var header = '';
      if(step==='image')
        header = launch_instance_section_header_image+':';
      else if(step==='type')
        header = launch_instance_section_header_type+':';
      else if(step==='security')
        header = launch_instance_section_header_security+':';
      else if(step==='advanced')
        header = launch_instance_section_header_advanced+':';
      $step.find('.required-missing-message').remove();
      $step.append($('<div>').addClass('required-missing-message').append($('<span>').html(header), $('<span>').html(launch_instance_required_missing)));
      return false;
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
