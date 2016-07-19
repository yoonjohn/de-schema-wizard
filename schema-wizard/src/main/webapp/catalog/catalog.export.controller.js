(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.controller('exportController', function ($scope, $uibModal, $window, $log) {
        $scope.exportSchema = function (schema) {

            $scope.name = schema.sName;
            //Export for Ingest
            var ingestExportObject = {};
            // Export for VizWiz
            var vizWizExportObject = {};
            vizWizExportObject.sId = schema.sId;
            vizWizExportObject.sProfile = {};
            //DigitalEdge
            var digitalEdgeExportObject = {};
            digitalEdgeExportObject.sProfile = {};

            var fieldCount = 0;
            angular.forEach(schema.sProfile, function (value, key) {
                fieldCount += 1;
                vizWizExportObject.sProfile[key] = {};
                if (value['presence'] === -1) {
                    vizWizExportObject.sProfile[key].mainType = (value['main-type']);
//TODO: remove try/catch after detail gets set for user added fields (ref VersionOne B-06537)
                    try {
                        vizWizExportObject.sProfile[key].detailType = (value['detail']['detail-type']);
                    } catch (ex) {
                        vizWizExportObject.sProfile[key].detailType = "Unknown";
                    }
                    vizWizExportObject.sProfile[key].numberDistinctValues = 0;
                    vizWizExportObject.sProfile[key].interpretations = "Unknown";
                } else {
                    vizWizExportObject.sProfile[key].mainType = (value['main-type']);
                    vizWizExportObject.sProfile[key].detailType = (value['detail']['detail-type']);
                    vizWizExportObject.sProfile[key].numberDistinctValues = (value['detail']['num-distinct-values']);
                    vizWizExportObject.sProfile[key].interpretations = (value['interpretation']);
                }
                // DigitalEdge
                digitalEdgeExportObject.sProfile[key] = (value['main-type']);
                //Ingest Object
                ingestExportObject[key] = "get("+(value['alias-names'][0]['alias-name'])+")";
            });
            vizWizExportObject.numberOfFields = fieldCount;
            //Export DigitalEdge
            var digitalEdgeData = angular.toJson(digitalEdgeExportObject, true);
            digitalEdgeData = digitalEdgeData.replace(/\n/g, "\r\n");
            var blobDigitalEdge = new Blob([digitalEdgeData], {type: "octet/stream"});
            var urlDigitalEdge = $window.URL || $window.webkitURL;
            $scope.fileUrlDigital = urlDigitalEdge.createObjectURL(blobDigitalEdge);
            $scope.digitalEdgeDataObject = $scope.name + ".json";
            $scope.zipIngestObject = $scope.name + ".json";
            //Zip for DigitalEdge
            $scope.zipExtractDigitalEdge = function () {
                var zipDigitalEdge = new JSZip();
                var emptyArrayForDigitalEdge = "[ ]";
                $scope.jsonEnrichment = "datasources.json";
                $scope.jsonDataSource = "enrichcfg.json";
                $scope.jsonCanonical = "canonical.json";
                zipDigitalEdge.file($scope.jsonEnrichment, emptyArrayForDigitalEdge);
                zipDigitalEdge.file($scope.jsonDataSource, emptyArrayForDigitalEdge);
                zipDigitalEdge.file($scope.jsonCanonical, digitalEdgeData);
                var content = zipDigitalEdge.generate({type: "blob"});
                // see FileSaver.js
                saveAs(content, $scope.name);
            };

            // export viz
            var vizData = angular.toJson(vizWizExportObject, true);
            vizData = vizData.replace(/\n/g, "\r\n")
            var blobViz = new Blob([vizData], {type: "octet/stream"})
            var urlViz = $window.URL || $window.webkitURL;
            $scope.fileUrlWiz = urlViz.createObjectURL(blobViz);
            $scope.vizWizDataObject = $scope.name + ".json"

            //export to Ingest
            var ingestData = angular.toJson(ingestExportObject, true);
            ingestData = ingestData.replace(/\n/g, "\r\n")
            var blobIngest = new Blob([ingestData], {type: "octet/stream"})
            var urlIngest = $window.URL || $window.webkitURL;
            $scope.fileUrlIngest = urlIngest.createObjectURL(blobIngest);
            $scope.ingestDataObject = $scope.name + ".json"

            // File export for other file types
            var schemaData = angular.toJson(schema, true);
            schemaData = schemaData.replace(/\n/g, "\r\n")
            //$log.debug(schemaData)
            var blob = new Blob([schemaData], {type: "octet/stream"})
            var url = $window.URL || $window.webkitURL;
            $scope.fileUrl = url.createObjectURL(blob);
            $scope.json = $scope.name + ".json";
            $scope.txt = $scope.name + ".txt";
            $scope.doc = $scope.name + ".doc";
            $scope.zip = $scope.name + ".zip";

            $scope.zipExtract = function () {
                var zip = new JSZip();
                zip.file($scope.json, schemaData);
                zip.file($scope.txt, schemaData);
                zip.file($scope.doc, schemaData);
                var content = zip.generate({type: "blob"});
                // see FileSaver.js
                saveAs(content, $scope.name);
            };

            var modalInstance = $uibModal.open({
                animation: $scope.animationsEnabled,
                templateUrl: 'schema-wizard/schema-wizard.export.html',
                controller: 'exportInstanceController',
                scope: $scope,
                size: '560',
                backdrop: 'static'
            });
            modalInstance.result.then(function () {
            }, function () {
                $log.info("exportSchema Modal dismissed");
            });
        }; // exportSchema
    }); // exportController

    // $uibModalInstance represents a modal window (instance) dependency.
    schemaWizardApp.controller('exportInstanceController', function ($scope, $uibModalInstance) {
        $scope.cancel = function () {
            $uibModalInstance.dismiss('cancel');
        };
    }); // exportInstanceController

})();
