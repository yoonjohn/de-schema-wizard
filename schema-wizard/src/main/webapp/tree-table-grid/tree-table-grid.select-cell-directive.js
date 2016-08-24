(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.directive('selectCell', function () {
        var linker = function (scope, element, attrs) {
            scope.internalMethod1 = function (selectedItem) {
                scope.externalMethod1()(selectedItem);
            };
            scope.data = scope.$eval(decodeURI(attrs.data));
        };
        return {
            restrict: "E",
            templateUrl: "tree-table-grid/templates/select-cell.html",
            transclude: true,
            replace: true,
            scope: {
                nodeLabel: '@',
                externalMethod1: '&',
                externalMethod2: '&'
            },
            link: linker
        }
    }); // selectCell
})();
