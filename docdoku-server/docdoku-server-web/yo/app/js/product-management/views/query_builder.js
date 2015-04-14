/*global _,define,App*/
define([
    'backbone',
    'mustache',
    'text!templates/query_builder.html',
    'query-builder',
    'selectize',
    '../../utils/query-builder-options',
    'common-objects/views/alert'
], function (Backbone, Mustache, template, queryBuilder, selectize,querybuilderOptions, AlertView) {
    'use strict';
    var QueryBuilderModal = Backbone.View.extend({

        events: {
            'click .search-button': 'onSearch'
        },

        delimiter: '/',

        initialize: function () {
            this.selectizeOptions = {
                plugins: ['remove_button','drag_drop'],
                persist: false,
                delimiter:this.delimiter,
                valueField: 'value',
                searchField: ['name'],
                options: querybuilderOptions.fields,
                render: {
                    item: function(item, escape) {
                        return '<div><span class="name">' + escape(item.name) + '</span></div>';
                    },
                    option: function(item, escape) {
                        return '<div><span class="label">' + escape(item.name) + '</span></div>';
                    }
                }
            };
        },

        render: function () {
            this.$el.html(Mustache.render(template, {i18n: App.config.i18n}));
            this.bindDomElements();
            this.fillSelectizes();

            this.$where.queryBuilder({
                filters: querybuilderOptions.filters,

                icons:{
                    add_group : 'fa fa-plus-circle',
                    remove_group : 'fa fa-times-circle',
                    error : 'fa fa-exclamation',
                    remove_rule : 'fa fa-remove',
                    add_rule : 'fa fa-plus'
                }
            });

            this.$saveSwitch.bootstrapSwitch();
            this.$saveSwitch.bootstrapSwitch('setState', false);
            this.$inputName.toggle(false);
            var self = this;
            this.$saveSwitch.on('switch-change', function (event, state) {
                self.$inputName.toggle(state.value);
            });

            return this;
        },

        fillSelectizes: function(){
            this.$select.selectize(this.selectizeOptions);
            this.$orderBy.selectize(this.selectizeOptions);
            this.$groupBy.selectize(this.selectizeOptions);
        },

        bindDomElements: function () {
            this.$modal = this.$('#query-builder-modal');
            this.$where = this.$('#where');
            this.$select = this.$('#select');
            this.$orderBy = this.$('#orderBy');
            this.$groupBy = this.$('#groupBy');
            this.$saveSwitch = this.$('.saveSwitch.switch');
            this.$inputName = this.$('.queryName');
        },

        onSearch:function(){
            var self = this;

            var isValid = this.$where.queryBuilder('validate');

            if(isValid) {

                var selectList = this.$select[0].selectize.getValue().split(this.delimiter);
                var where = this.$where.queryBuilder('getRules');
                var orderByList = this.$orderBy[0].selectize.getValue().split(this.delimiter);
                var groupByList = this.$groupBy[0].selectize.getValue().split(this.delimiter);

                var saveQuery = this.$saveSwitch.bootstrapSwitch('status');

                if (saveQuery && this.$inputName.val().length === 0) {
                    this.$inputName.focus();
                }

                var queryData = {
                    selects: selectList,
                    orderByList: orderByList,
                    groupedByList: groupByList,
                    subRules: where,
                    name: saveQuery ? this.$inputName.val() : ''
                };

                var url = App.config.contextPath + '/api/workspaces/' + App.config.workspaceId + '/parts/queries?save=' + saveQuery;
                $.ajax({
                    type: 'POST',
                    url: url,
                    data: JSON.stringify(queryData),
                    contentType: 'application/json',
                    success: function (data) {
                        var dataToTransmit = {
                            queryData:queryData,
                            queryResponse:data
                        };
                        self.trigger('query:search', dataToTransmit);
                    },
                    error: function (errorMessage) {
                        self.$('#alerts').append(new AlertView({
                            type: 'error',
                            message: errorMessage
                        }).render().$el);
                    }
                });
            }


        }
    });



    return QueryBuilderModal
});
