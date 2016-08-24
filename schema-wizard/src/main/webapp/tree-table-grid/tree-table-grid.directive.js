/*
    <div
        data-angular-tree-table="true"          // the treetableview directive
        data-tree-table-id="treeTable"          // each tree's unique id
        data-tree-model="treeTable.data"        // the tree model on $scope
        data-node-id="id"                       // each node's id
        data-link-data=linkData                 // external data reference
        data-node-label1="columnName1"          // each node's 1st label
        data-node-label2="columnName2"          // each node's 2nd label
        data-node-children="children"           // each node's children
        data-tree-depth="0"                     // the depth of the tree at each level
        data-column-is-tree=""                  // render column as tree
        data-cell-min-width=""                  // the width of the collumn header
        data-tree-cell-directive="directive"    // directive for node (tree) cells
        data-other-tree-data="data"             // other data for the tree directive
        data-tree-cell-method1="method1"        // method1 for tree-cell-directive
        data-tree-cell-method2="method2"        // method2 for tree-cell-directive
        data-table-cell-directive="directive"   // directive for node (table) cells
        data-other-table-data="data"            // other data for the table directive
        data-table-cell-method1="method1"       // method1 for table-cell-directive
        data-table-cell-method2="method2">      // method2 for table-cell-directive
     </div>
*/
(function ( angular ) {
	'use strict';

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.directive( 'treeTableGrid', ['$compile', function( $compile ) {
			return {
				restrict: 'A',
				link: function ( scope, element, attrs ) {
					var treeTableId = attrs.treeTableId;
					var treeTableGrid = attrs.treeTableGrid;
                    var linkData = attrs.linkData;
					var nodeId = attrs.nodeId || 'id';
                    var nodeLabel1 = attrs.nodeLabel1 || 'label1';
                    var nodeLabel2 = attrs.nodeLabel2 || null;
					var nodeChildren = attrs.nodeChildren || 'children';
                    var depth = attrs.treeDepth;
                    var columnIsTree = attrs.columnIsTree;
                    var cellMinWidth = attrs.cellMinWidth;
                    var treeCellDirective = attrs.treeCellDirective || 'treecell';
                    var otherTreeData = attrs.otherTreeData || '';
                    var treeCellMethod1 = attrs.treeCellMethod1 || '';
                    var treeCellMethod2 = attrs.treeCellMethod2 || '';
                    var tableCellDirective = attrs.tableCellDirective || 'tablecell';
                    var otherTableData = attrs.otherTableData || '';
                    var tableCellMethod1 = attrs.tableCellMethod1 || '';
                    var tableCellMethod2 = attrs.tableCellMethod2 || '';
                    var template =
                        '<div class="tree-table-row" style="display: block; width: 100%;" data-ng-repeat-start="node in ' + treeTableGrid + '">' +
                            '<div class="tree-table-cell" style="display: block; width: 100%;">' +
                                '<span ng-show="' + columnIsTree + '" ' +
                                      'style="display: block; min-width: 200px; text-align: left;" ' +
                                      'ng-style="node.' + nodeId + '% 2 == 1 && {\'background\': \'white\', \'padding-left\': \'' + (depth * 30) + 'px\'} || ' +
                                                'node.' + nodeId + '% 2 == 0 && {\'background\': \'azure\', \'padding-left\': \'' + (depth * 30) + 'px\'}"> ' +
                                    '<i class="tree-table-collapsed" ' +
                                       'data-ng-show="node.' + nodeChildren + '.length && node.collapsed" ' +
                                       'data-ng-click="' + treeTableId + '.explandCollapse($event, node)"></i>' +
                                    '<i class="tree-table-expanded" ' +
                                       'data-ng-show="node.' + nodeChildren + '.length && !node.collapsed" ' +
                                       'data-ng-click="' + treeTableId + '.explandCollapse($event, node)"></i>' +
                                    '<i class="tree-table-normal" data-ng-hide="node.' + nodeChildren + '.length"></i> ' +
                                    '<' + treeCellDirective + ' node="{{node}}" ' +
                                                               'node-id="{{node.' + nodeId + '}}" ' +
                                                               'node-label1="{{node.' + nodeLabel1 + '}}" ' +
                                                               'node-label2="{{node.' + nodeLabel2 + '}}" ' +
                                                               'external-method1="' + treeCellMethod1 + '" ' +
                                                               'external-method2="' + treeCellMethod2 + '" ' +
                                                               'data-link-data="' + linkData + '"/> ' +
                                '</span>' +
                                '<span ng-hide="' + columnIsTree + '" ' +
                                      'style="display: block; width: ' + cellMinWidth + 'px; margin: 0px; padding-left: 4px; padding-right: 4px;" ' +
                                      'ng-style="node.' + nodeId + '% 2 == 1 && {\'background\': \'white\'} || ' +
                                                'node.' + nodeId + '% 2 == 0 && {\'background\': \'azure\'}"> ' +
                                    '<i class="tree-table-collapsed" style="background-image: none;"' +
                                       'data-ng-show="node.' + nodeChildren + '.length && node.collapsed" ' +
                                       'data-ng-click="' + treeTableId + '.explandCollapse($event, node)"></i>' +
                                    '<i class="tree-table-expanded" style="background-image: none;"' +
                                       'data-ng-show="node.' + nodeChildren + '.length && !node.collapsed" ' +
                                       'data-ng-click="' + treeTableId + '.explandCollapse($event, node)"></i>' +
                                    '<i class="tree-table-normal" data-ng-hide="node.' + nodeChildren + '.length"></i> ' +
                                    '<' + tableCellDirective + ' node="{{node}}" ' +
                                                                'node-id="{{node.' + nodeId + '}}" ' +
                                                                'label1="' + nodeLabel1 + '" ' +
                                                                'label2="' + nodeLabel2 + '" ' +
                                                                'node-label1="{{node.' + nodeLabel1 + '}}" ' +
                                                                'node-label2="{{node.' + nodeLabel2 + '}}" ' +
                                                                'external-method1="' + tableCellMethod1 + '" ' +
                                                                'external-method2="' + tableCellMethod2 + '" ' +
                                                                'data-link-data="' + linkData + '" ' +
                                                                'data="' + otherTableData + '" ' +
                                         'data-ng-hide="node.' + nodeChildren + '.length"/> ' +
                                '</span>' +
                            '</div>' +
                        '</div>' +
                        '<div class="tree-table-row" style="display: block; width: 100%;" data-ng-repeat-end>' +
                            '<div id="{{node.' + nodeId + '}}" ' +
                                 'class="tree-table-cell" style="display: block; width: 100%;" ' +
                                 'data-node-id=' + nodeId + ' ' +
                                 'data-ng-hide="node.collapsed" ' +
                                 'data-tree-table-id="' + treeTableId + '" ' +
                                 'data-tree-table-grid="node.' + nodeChildren + '" ' +
                                 'data-link-data="' + linkData + '" ' +
                                 'data-node-label1=' + nodeLabel1 + ' ' +
                                 'data-node-label2=' + nodeLabel2 + ' ' +
                                 'data-tree-depth="' + ++depth + '" ' +
                                 'data-node-children=' + nodeChildren + ' ' +
                                 'data-column-is-tree=' + columnIsTree + ' ' +
                                 'data-cell-min-width="' + cellMinWidth + 'px" ' +
                                 'data-tree-cell-directive="' + treeCellDirective + '" ' +
                                 'data-other-tree-data="' + otherTreeData + '" ' +
                                 'data-tree-cell-method1="' + treeCellMethod1 + '" ' +
                                 'data-tree-cell-method2="' + treeCellMethod2 + '" ' +
                                 'data-table-cell-directive="' + tableCellDirective + '" ' +
                                 'data-other-table-data="' + otherTableData + '" ' +
                                 'data-table-cell-method1="' + tableCellMethod1 + '" ' +
                                 'data-table-cell-method2="' + tableCellMethod2 + '">' +
                            '</div>' +
                        '</div>';

					if( treeTableId && treeTableGrid ) {
						if( attrs.angularTreeTable ) {
							scope[treeTableId] = scope[treeTableId] || {};
							scope[treeTableId].explandCollapse =
								scope[treeTableId].explandCollapse || function( $event, selectedNode ) {
                                    var expandCollapseAll = function(node, collapsed) {
                                        node.collapsed = collapsed;
                                        for (var i = 0, len = node.children.length; i < len; i++) {
                                            expandCollapseAll(node.children[i], collapsed);
                                        }
                                    }
                                    if ($event.shiftKey == true) {
                                        for (var i = 0, len = selectedNode.children.length; i < len; i++) {
                                            selectedNode.children[i].collapsed = !selectedNode.children[i].collapsed;
                                        }
                                    } else if ($event.ctrlKey == true) {
                                        expandCollapseAll(selectedNode, !selectedNode.collapsed);
                                    } else {
                                        selectedNode.collapsed = !selectedNode.collapsed;
                                    }
    							};
						}
                        element.html('').append( $compile( template )( scope ) );
					}
				}
			};
	}]);
})( angular );
