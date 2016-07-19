(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.directive('detailsGenericTable', function () {
        var linker = function (scope, element, attrs) {
            scope.showInDetails = function (currentSample, field) {
                scope.showInGenericDetails()(currentSample, field);
            };
            scope.$watch('currentSample', function() {
            })
        };
        return {
            restrict: "A",
            templateUrl: "schema-wizard/schema-wizard.generic.details.table.html",
            transclude: true,
            replace: false,
            scope: {
                detailsRow: '@',
                currentSample: '=',
                showInGenericDetails: "&"
            },
            link: linker
        }
    });
})();
