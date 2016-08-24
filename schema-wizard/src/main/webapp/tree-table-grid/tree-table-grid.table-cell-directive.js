(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.directive('tablecell',['numberFilter', function (numberFilter) {
        var linker = function (scope, element, attrs) {
            var linkData = attrs.linkData;
            var node = attrs.node;
            node = scope.$eval(node);
            var label1 = attrs.label1;
            var label2 = attrs.label2;
            if (node.children.length == 0) {
                var label1Splits = label1.split('.');
                var property1 = null;
                switch (label1Splits.length) {
                    case 1: property1 = scope[linkData].dsProfile[node.path][label1Splits[0]]; break;
                    case 2: property1 = scope[linkData].dsProfile[node.path][label1Splits[0]][label1Splits[1]]; break;
                    case 3: property1 = scope[linkData].dsProfile[node.path][label1Splits[0]][label1Splits[1]][label1Splits[2]]; break;
                }
                scope.justify = "center";
                // if nodeLabel1 is syntactically a number then eval to a number
                if (/^[0-9]*\.?[0-9]+$/.test(property1)) {
                    var castNodeLabel1 = Number(property1);
                    scope.justify = "right";
                    if (Number.isInteger(castNodeLabel1)) {
                        scope.nodeLabel1 = castNodeLabel1;
                    } else {
                        scope.nodeLabel1 = numberFilter(castNodeLabel1, 2);
                    }
                } else {
                    scope.nodeLabel1 = property1;
                }
                // check for two labels for this node
                if (label2) {
                    var label2Splits = label2.split('.');
                    var property2 = null;
                    switch (label2Splits.length) {
                        case 1: property2 = scope[linkData].dsProfile[node.path][label2Splits[0]]; break;
                        case 2: property2 = scope[linkData].dsProfile[node.path][label2Splits[0]][label2Splits[1]]; break;
                        case 3: property2 = scope[linkData].dsProfile[node.path][label2Splits[0]][label2Splits[1]][label2Splits[2]]; break;
                    }
                    scope.justify = "center";
                    if (/^[0-9]*\.?[0-9]+$/.test(property2)) {
                        var castNodeLabel2 = Number(property2);
                        if (Number.isInteger(castNodeLabel2)) {
                            scope.nodeLabel2 = castNodeLabel2;
                        } else {
                            scope.nodeLabel2 = numberFilter(castNodeLabel2, 2);
                        }
                    } else {
                        scope.nodeLabel2 = property2;
                    }
                } else {
                    scope.nodeLabel2 = null;
                }
            } else {
                scope.nodeLabel1 = "&nbsp;";
            }
        };
        return {
            restrict: "E",
            templateUrl: "tree-table-grid/templates/table-cell.html",
            transclude: true,
            replace: true,
            link: linker
        }
    }]); // tablecell
})();
