(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.directive('detailsPanel', function () {
        var linker = function (scope, element, attrs) {
                        scope.$watch('detailModelsPanel', function() {})
        };
        return {
            restrict: "A",
            templateUrl: "schema-wizard/schema-wizard.details.html",
            transclude: true,
            replace: false,
            scope: {
                panelIndex: '@',
                detailModelsPanel: '='
            },
            link: linker
        }
    });
})();
