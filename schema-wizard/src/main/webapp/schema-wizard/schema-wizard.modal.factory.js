(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.factory('myModals', ['$uibModal', function ($uibModal) {
        // called from various methods within factory
        function openModal(template, data, options) {
            //build all of the modal options
            var modalOpts = {
                animation: true,
                templateUrl: template,
                controller: function ($scope, $uibModalInstance, alert_data) {
                    $scope.alert_data = alert_data;
                    $scope.cancel = function () {
                        $uibModalInstance.dismiss('cancel');
                    };
                },
                resolve: {
                    alert_data: data
                },
                size: '560',
                backdrop: false
            };
            // extend options set in each use type function
            if (options) {
                angular.extend(modalOpts, modalOpts);

            }
            var modalInstance = $uibModal.open(modalOpts);

            modalInstance.result.then(function (data) {
                // always do something when close called
                return data;
            }, function (data) {
                //always do something when dismiss called
                return data
            });

            return modalInstance;
        }

        function alert(type, title, size, url, text) {

            var template;
            // enter in template and string being passed back to identify modal type
            switch (type) {
                case 'success':
                    template = 'schema-wizard/schema-wizard.empty.popup.html';
                    break;
                case 'generic':
                    template = 'schema-wizard/schema-wizard.generic.popup.html';
                    break;
                case 'confirm':
                    template = 'schema-wizard/schema-wizard.confirm.popup.html';
                    break;
            }

            var opts = {
                //default but should be passed back
                size: size || 'sm'
            };
            var data = {
                title: title,
                text: text,
                url: url
            };

            return openModal(template, data, opts);

        }

        return {
            alert: alert
        }

    }])
})();
