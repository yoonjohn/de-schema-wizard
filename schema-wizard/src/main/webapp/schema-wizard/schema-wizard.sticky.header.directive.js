(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.directive("sticky", ["$window", function($window) {
        return ({
            link: link,
            restrict: "C"
        });
        function link(scope, element, attrs) {
            angular.element($window).bind("scroll", function() {
                if ($window.pageYOffset >= 10) {
                    console.log("test")
                    angular.element(element).addClass("fixed");
                } else {
                    angular.element(element).removeClass("fixed");
                }
            });
        }
    }]);


})();
