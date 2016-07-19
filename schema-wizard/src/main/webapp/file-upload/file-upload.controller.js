(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.directive('fileModel', ['$parse', '$log', '$confirm',
        function ($parse, $log, $confirm) {
            return {
                restrict: 'A',
                link: function (scope, element, attrs) {
                    var model = $parse(attrs.fileModel);
                    var modelSetter = model.assign;
                    scope.sampleFile = model;
                    element.bind('change', function () {
                        // if the file open dialog is raised and the file name
                        // is cleared and cancel is pressed then a reset is needed
                        document.getElementById('file-upload-name').innerHTML = "";
                        document.getElementById('file-upload-btn').disabled = true;

                        // status always needs reset if choosing another file
                        scope.$apply(function () {
                            modelSetter(scope, element[0].files);
                            if (document.getElementById('file-upload').files) {
                                for (var i = 0; i < element[0].files.length; i++) {
                                    if (element[0].files[i].name.indexOf(".xlsx")!==-1) {
                                        $confirm(
                                            {
                                                title: 'Schema Wizard does not support this file type',
                                                text: "Schema Wizard does not support the file type for: \n" + element[0].files[i].name,
                                                ok: 'OK'
                                            },
                                            {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                                            .then(function () {
                                                document.getElementById('file-upload-name').innerHTML = "";
                                                document.getElementById('file-upload-btn').disabled = true;
                                            });
                                    }// try and prevent users from uploading unsupported files.
                                     //this is a work-around for IE
                                    document.getElementById('file-upload-name').innerHTML +=
                                        element[0].files[i].name + " ";
                                    document.getElementById('file-upload-btn').disabled = false;
                                }
                            }
                        });
                    });
                } // link
            };
        }]); // fileModel

    schemaWizardApp.service('fileUpload', ['$http', '$log',
        function ($http, $log) {
            this.uploadFileToUrl = function (file, uploadUrl) {
                //$log.debug("file(s)");
                //$log.debug(file);
                var fd = new FormData();
                angular.forEach(file, function (value, key) {
                    fd.append(key, value);
                });
                return $http.post(uploadUrl, fd, {
                    transformRequest: angular.identity,
                    headers: {
                        'Content-Type': undefined,
                        'enctype': "multipart/form-data"
                    }
                })
            }; // uploadFileToUrl
        }]); // fileUpload

    schemaWizardApp.controller('fileUploadCtrl',
        function ($scope, $rootScope, $log, $http, UploadParameters, fileUpload, $confirm) {
            document.getElementById('titleRef').style.pointerEvents = 'none';

            $scope.hideMask = function () {
                document.getElementById("mask").style.display = "none";
            };

            $scope.setSchemaDomain = function ($event, schemaDomain) {
                if ($event !== null) $event.preventDefault();
                $scope.schemaDomain = schemaDomain;
                $rootScope.$broadcast("setSchemaDomain", {
                    schemaDomain: $scope.schemaDomain
                });
            }; // setSchemaDomain

            $scope.setSchemaTolerance = function ($event, schemaTolerance) {
                $event.preventDefault();
                $scope.schemaTolerance = schemaTolerance;
            }; // setSchemaTolerance

            $scope.domains = UploadParameters.get().domainsArray;
            $log.debug("Domains");
            $log.debug($scope.domains);
            if ($scope.domains.length === 0) {
                $scope.schemaDomain = "Not Available";
            }
            ;
            $scope.setSchemaDomain(null, $scope.schemaDomain);

            // disable the domain button when modifying an existing schema; o/w enable it
            document.getElementById('btn-append-to-domain-button').disabled = !(($scope.schemaDomain === null || $scope.schemaDomain === undefined || $scope.schemaDomain === '')
            && $scope.schemaDomain !== "Not Available");

            // set the domain to the one specified in the URL query string if provided, only for new schema
            if (($scope.schemaDomain === null || $scope.schemaDomain === undefined || $scope.schemaDomain === '')
                && $scope.preferredSchemaDomain !== null && $scope.preferredSchemaDomain !== undefined) {
                $scope.schemaDomain = $scope.preferredSchemaDomain;
            }
            ;
            $scope.setSchemaDomain(null, $scope.schemaDomain);
            //$log.debug("$scope.schemaDomain");
            //$log.debug("'" + $scope.schemaDomain + "'");

            // enable/disable the upload button when all parameters are set
            $scope.$watch(function (scope) {
                    return scope.schemaDomain && scope.schemaTolerance &&
                        document.getElementById('file-upload').files.length > 0;
                },
                function () {
                    if (document.getElementById('file-upload').files.length > 0 &&
                        $scope.schemaDomain != null && $scope.schemaTolerance != null) {
                        document.getElementById('file-upload-btn').disabled = false;
                    } else {
                        document.getElementById('file-upload-btn').disabled = true;
                    }
                }
            ); // watchFileUpload

            $scope.uploadFile = function () {
                var file = $scope.sampleFile;
                //$log.debug("fileUploadCtrl file(s) to upload:");
                //console.dir(file);
                $rootScope.$broadcast("sampleFilesSelected", {
                    newSampleFiles: file
                });

                fileUpload.uploadFileToUrl(
                    file,
                        "rest/upload?" +
                        "domain=" + $scope.schemaDomain + "&" +
                        "tolerance=" + $scope.schemaTolerance + "&" +
                        "schemaGuid=" + ($scope.modifySchemaMode ? $scope.currentSchema.sId : null))
                    .success(function (data) {
                        $log.debug("Data returned:");
                        $log.debug(data);
                        $log.info("File(s) uploaded successfully.");
                        // create an event for the enclosing controller so
                        // it can update the data sources in it's scope
                        $rootScope.$broadcast("dataSamplesReceived", {
                            newDataSamples: data
                        });
                        $rootScope.$broadcast("closeWebSocket", {});
                    })
                    .error(function (data) {
                        $confirm(
                            {
                                title: 'File Upload Failed',
                                text: "Uploading of the file(s) has failed. Click 'Ok' to try again.",
                                ok: 'OK'
                            },
                            {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                            .then(function () {
                                $log.debug(data);
                                $scope.hideMask();
                                $scope.navigateTo("/wizardUploadSamples");
                            });
                    });
            }; // uploadFile
        }); // fileUploadCtrl
})();
