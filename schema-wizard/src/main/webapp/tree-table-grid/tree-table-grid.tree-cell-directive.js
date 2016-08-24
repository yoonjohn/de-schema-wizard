(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    var previousDetailsNodeId = null;

    schemaWizardApp.directive('treecell', function () {
        var linker = function (scope, element, attrs) {

            scope.internalMethod1 = function (node) {
                var thisNode = scope.$eval(node);
                // only act upon leaf nodes
                if (thisNode && thisNode.children.length == 0) {
                    try {
                        if (previousDetailsNodeId) document.getElementById(previousDetailsNodeId).style.backgroundColor = "transparent";
                    } catch (e) {}
                    scope.externalMethod1()(thisNode);
                    var thisNodeId = "treecell-" + thisNode.id;
                    document.getElementById(thisNodeId).style.backgroundColor = "gold";
                    previousDetailsNodeId = thisNodeId;
                    return angular.toJson(thisNode);
                }
            };

            scope.internalMethod2 = function (node) {
                scope.externalMethod2()(node);
            };

            scope.cursor = (scope.$eval(attrs.node).children.length > 0 ? 'not-allowed' : 'pointer');
        };
        return {
            restrict: "E",
            templateUrl: "tree-table-grid/templates/tree-cell.html",
            transclude: true,
            replace: true,
            scope: {
                node: '@',
                nodeId: '@',
                nodeLabel1: '@',
                nodeLabel2: '@',
                externalMethod1: "&",
                externalMethod2: "&"
            },
            link: linker
        }
    }); // treecell
})();
