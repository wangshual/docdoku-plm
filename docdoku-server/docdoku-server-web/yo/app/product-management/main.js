/*global _,require,window*/
var workspace = /^#([^\/]+)/.exec(window.location.hash);
if(!workspace){
    location.href = '../faces/admin/workspace/workspacesMenu.xhtml';
    throw new Error('Cannot parse workspace in url');
}
var App = {
	debug:false,

	config:{
		workspaceId: decodeURIComponent(workspace[1]).trim() || null,
		login: '',
		groups: [],
		contextPath: '',
		locale: window.localStorage.getItem('locale') || 'en'
	}
};

App.log=function(message){
    'use strict';
    if(App.debug){
        window.console.log(message);
    }
};

require.config({
    baseUrl: '../js/product-management',
    shim: {
        jqueryUI: { deps: ['jquery'], exports: 'jQuery' },
        effects: { deps: ['jquery'], exports: 'jQuery' },
        popoverUtils: { deps: ['jquery'], exports: 'jQuery' },
        bootstrap:{ deps: ['jquery','jqueryUI'], exports: 'jQuery' },
        bootbox: { deps: ['jquery'], exports: 'jQuery' },
        datatables:{ deps: ['jquery'], exports: 'jQuery' },
        backbone: {deps: ['underscore', 'jquery'],exports: 'Backbone'},
        bootstrapCombobox:{deps:["jquery"],exports:"jQuery"},
        bootstrapSwitch:{deps:['jquery'],exports:'jQuery'},
        bootstrapDatepicker: {deps: ['jquery'], exports: 'jQuery'},
        unmask: { deps: ['jquery'], exports: 'jQuery' },
        unmaskConfig: { deps: ['unmask'], exports: 'jQuery' },
        inputValidity: { deps: ['jquery'], exports: 'jQuery' },
        'query-builder': { deps: ['jquery'], exports: 'jQuery' },
        selectize: { deps: ['jquery'], exports: 'jQuery' }
    },
    paths: {
        jquery: '../../bower_components/jquery/jquery',
        backbone: '../../bower_components/backbone/backbone',
        underscore: '../../bower_components/underscore/underscore',
        mustache: '../../bower_components/mustache/mustache',
        text: '../../bower_components/requirejs-text/text',
        i18n: '../../bower_components/requirejs-i18n/i18n',
        buzz: '../../bower_components/buzz/dist/buzz',
        bootstrap:'../../bower_components/bootstrap/docs/assets/js/bootstrap',
        bootbox:'../../bower_components/bootbox/bootbox',
        datatables:'../../bower_components/datatables/media/js/jquery.dataTables',
        jqueryUI: '../../bower_components/jqueryui/ui/jquery-ui',
        unmask:'../../bower_components/jquery-maskedinput/dist/jquery.maskedinput',
        bootstrapCombobox:'../../bower_components/bootstrap-combobox/js/bootstrap-combobox',
        bootstrapSwitch:'../../bower_components/bootstrap-switch/static/js/bootstrap-switch',
        bootstrapDatepicker:'../../bower_components/bootstrap-datepicker/js/bootstrap-datepicker',
        unorm:'../../bower_components/unorm/lib/unorm',
        moment:'../../bower_components/moment/min/moment-with-locales',
        momentTimeZone:'../../bower_components/moment-timezone/builds/moment-timezone-with-data',
        date:'../../bower_components/date.format/date.format',
        unmaskConfig:'../utils/jquery.maskedinput-config',
        localization: '../localization',
        modules: '../modules',
        'common-objects': '../common-objects',
        userPopover:'modules/user-popover-module/app',
        effects:'../utils/effects',
        popoverUtils: '../utils/popover.utils',
        inputValidity: '../utils/input-validity',
        datatablesOsortExt: '../utils/datatables.oSort.ext',
        utilsprototype:'../utils/utils.prototype',
        async: '../../bower_components/async/lib/async',
        'query-builder': '../../bower_components/jQuery-QueryBuilder/dist/js/query-builder.standalone',
        selectize: '../../bower_components/selectize/dist/js/standalone/selectize'
    },

    deps:[
        'jquery',
        'underscore',
        'date',
        'bootstrap',
        'bootbox',
        'jqueryUI',
        'effects',
        'popoverUtils',
        'datatables',
        'datatablesOsortExt',
        'bootstrapCombobox',
        'bootstrapSwitch',
        'bootstrapDatepicker',
        'utilsprototype',
        'unmaskConfig',
        'inputValidity',
        'query-builder',
        'selectize'
    ],
    config: {
        i18n: {
            locale: (function(){
	            'use strict';
                try{
                    return App.config.locale;
                }catch(ex){
                    return 'en';
                }
            })()
        }
    }
});

require(['common-objects/contextResolver','i18n!localization/nls/common','i18n!localization/nls/product-management'],
    function (ContextResolver,  commonStrings, productManagementStrings) {
	    'use strict';
        App.config.i18n = _.extend(commonStrings,productManagementStrings);
        ContextResolver.resolveUser(function(){
            require(['backbone','app','router','common-objects/views/header','modules/all'],function(Backbone, AppView, Router,HeaderView,Modules){
                App.appView = new AppView().render();
                App.headerView = new HeaderView().render();
                App.router = Router.getInstance();
                App.coworkersView = new Modules.CoWorkersAccessModuleView().render();
                Backbone.history.start();
            });
        });
    });
