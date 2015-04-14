/*global _,define,App*/
define([
    'backbone',
    'mustache',
    'text!templates/part/part_grouped_by_list.html',
    'views/part/part_grouped_by_list_item'
], function (Backbone, Mustache, template, PartGroupedByListItemView){
    'use strict';
    var PartGroupedByView = Backbone.View.extend({

        events: {

        },

        initialize: function () {
            this.items = this.options.data.queryResponse;
            this.selects = this.options.data.queryData.selects;
            this.orderByList = this.options.data.queryData.orderByList;
            this.groupedByList = this.options.data.queryData.groupedByList;
        },

        render: function () {

            var itemsGroupBy = this.groupBy();

            this.$el.html(Mustache.render(template, {
                i18n: App.config.i18n,
                columns:this.selects,
                groups: _.keys(itemsGroupBy)
            }));

            var self = this;
            _.each(_.keys(itemsGroupBy),function(key){
                var values = self.orderBy(itemsGroupBy[key]);
                _.each(values, function(item){
                    var itemView = new PartGroupedByListItemView({
                        item : item
                    }).render();
                    self.$('.items-'+key).append(itemView.el);
                });
            });

            return this;
        },

        groupBy:function(){
            var self = this;

            return _.groupBy(this.items,function(item){
                var groupByStringToUse = "";
                _.each(self.groupedByList, function(groupByColumn){
                    groupByStringToUse = groupByStringToUse+'_'+item[groupByColumn];
                });

                return groupByStringToUse.substring(1);
            });

        },

        orderBy:function(items){
            return _.orderBy(items, function(item){
                var orderByStringToUse = "";
                _.each(self.orderByList, function(orderByColumn){
                    orderByStringToUse = orderByStringToUse+'_'+item[orderByColumn];
                });
                return orderByStringToUse.substring(1);
            });
        }
    });

    return PartGroupedByView;
});
