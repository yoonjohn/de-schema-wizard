(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');
    schemaWizardApp.constant("matchConfidenceThreshold", 94);
    schemaWizardApp.constant("defaultInterpretationMatch", false);
    schemaWizardApp.controller('schemaWizardCtrl', ['$scope', '$rootScope', '$window',
        '$cookies', '$location', '$http', '$routeParams', '$uibModal', '$timeout',
        '$interval', '$log', 'DomainInformation', 'Server', 'UploadParameters',
        'matchConfidenceThreshold', 'defaultInterpretationMatch', 'myModals', '$confirm', '$sce', 'guidedTourStepFactory', '$q', 'uiTourService', 'statusCodesFactory',
        function ($scope, $rootScope, $window, $cookies, $location, $http, $routeParams,
                  $uibModal, $timeout, $interval, $log,
                  DomainInformation, Server, UploadParameters,
                  matchConfidenceThreshold, defaultInterpretationMatch, myModals, $confirm, $sce, guidedTourStepFactory, $q, TourService, statusCodesFactory) {

            $scope.userid = "wizard-user";

            $scope.animationsEnabled = true;
            $scope.isCollapsed = true;
            $scope.modifySchemaMode = false;
            $scope.confidenceThreshold = matchConfidenceThreshold;
            $scope.interpretationMatch = defaultInterpretationMatch;

            $scope.initializeModels = function () {
                $scope.model = {
                    "dataSamples": [],
                    "properties": {}
                };
                $scope.detailModels = {
                    selected1: null,
                    selected2: null,
                    dataList: {"data": []},
                    detailPanels: {"panel1": [], "panel2": []}
                };
                $scope.dropzoneModels = {
                    selected1: null,
                    selected2: null,
                    selected3: null,
                    dataList: {"data": []},
                    dropzones: {"zone1": [], "zone2": [], "zone3": []}
                };
            }; // initializeModels
            $scope.initializeModels();

            guidedTourStepFactory.get()
                .$promise.then(function (response) {
                $rootScope.tourInformation = response;
                $scope.mainTour = $rootScope.tourInformation.mainTour;
                $scope.inspectionTour = $rootScope.tourInformation.inspectSampleTour;
                $scope.inspectDataSourceTour = $rootScope.tourInformation.inspectDataSourceTour;
                $scope.finalizeSchemaTour = $rootScope.tourInformation.finalizeSchemaTour;
                console.log($rootScope.tourInformation)
            });
            if ($scope.path == "/wizardInspectSamples") {
                if ($cookies.get('schwiz.tours.inspectSample') !== "visited") {
                    $timeout(function () {
                        TourService.getTourByName('catalog').startAt('300');
                    }, 2500);
                    $cookies.put('schwiz.tours.interpretations', "visited");
                }
            }

            //TODO: test
            $scope.fileIndex = -1;

            //TODO: determine scoping issue for long field names
            $scope.placeHolderForLongFieldName = "Hover over points to see full name";

            //these are used to grab the field name and then log long-field names
            $scope.fieldName = null;
            $scope.logFieldNameFromRepeat = function(field){
                $scope.fieldName = field;
            }
            $scope.hoverLongFieldName = function (points, evt) {
                $scope.index = points[0]['_index']
                $scope.placeHolderForLongFieldName = $rootScope.test.sProfile[$scope.fieldName]['detail']['freq-histogram']['long-labels'][$scope.index];
                $scope.$apply()
            };

            $scope.transformTable = function () {
                $rootScope.$broadcast("transformTable", {});
                $scope.hasBeenClicked = false;
                setTimeout(fixHeader, 200);
                function fixHeader() {
                    var tableHeader = document.getElementById('customTableHeader');
                    tableHeader.style.position = "fixed";
                    $scope.hasBeenClicked = true;
                    console.log($scope.hasBeenClicked)
                }

                if ($scope.hasBeenClicked == true) {
                    var tableHeader = document.getElementById('customTableHeader');
                    tableHeader.style.position = "relative";
                    console.log($scope.hasBeenClicked)
                }
            }; // transformTable

            $scope.hidePanel = false;
            $scope.collapseDetailsPanels = function () {
                $scope.hidePanel = !$scope.hidePanel;
                if ($scope.hidePanel == true) {
                    $scope.transformTable();
                    document.getElementById("resizePanelDiv").style.width = "100%";
                    document.getElementById("collapseImage").src = "assets/img/collapse-panel-16x16.png";

                } else {
                    $scope.transformTable();
                    setTimeout(resizeOnMinimize, 300);
                    function resizeOnMinimize() {
                        document.getElementById('customTableHeader').style.position = "relative";
                    }

                    document.getElementById("resizePanelDiv").style.width = "";
                    document.getElementById("collapseImage").src = "assets/img/expand-panel-16x16.png";
                }
            }; // collapse details comparison panels

            $scope.showMask = function () {
                document.getElementById("mask").style.display = "block";
            }; // showMask

            $scope.hideMask = function () {
                document.getElementById("mask").style.display = "none";
            }; // hideMask

            $scope.preferredSchemaDomain = $location.search().domain;
            $scope.schemaTolerance = $location.search().tolerance;
            UploadParameters.setSchemaDomain($scope.schemaDomain);
            UploadParameters.setSchemaTolerance($scope.schemaTolerance);
            //$log.debug("UploadParameters");
            //$log.debug(UploadParameters.get());

            $scope.path = "/startup";
            $scope.resizeWizard = function () {
                try {
                    $log.debug("|" + $scope.path + "|");
                    if ($scope.path) {
                        //$log.debug("window.innerWidth: " + window.innerWidth);
                        var newViewHeight = (window.innerHeight
                            - document.getElementById("banner").style.height
                            - 20 /* footer */
                        );
                        // the /interpretations path can have additional parameters so strip them off now
                        if ($scope.path.indexOf("/interpretations") > 0) {
                            $scope.path = "/interpretations";
                        }
                        switch ($scope.path) {
                            case "/interpretations":
                                //TODO: remove try/catch when resizing interpretation dialog is finished
                                try {
                                    document.getElementById("interpretationsContainer").style.height = newViewHeight - 45 + "px";
                                    document.getElementById("interpretationsMain").style.height = newViewHeight - 98 + "px";

                                    var descPaneHeight = parseInt(document.getElementById("top-split-pane").style.height.slice(0, -2));

                                    // editor pane
                                    var editorPaneTop = parseInt(document.getElementById("bottom-center-pane").style.top.slice(0, -2));
                                    document.getElementById("editor-pane").style.height = (editorPaneTop - descPaneHeight - 2) + "px";
                                    document.getElementById("editorComponentInner").style.height = (editorPaneTop - descPaneHeight - 4) + "px";
                                    document.getElementById("editorPanel").style.height = (editorPaneTop - descPaneHeight - 8) + "px";
                                    document.getElementById("editorBody").style.height = (editorPaneTop - descPaneHeight - 38) + "px";
                                    document.getElementById("editor").style.height = (editorPaneTop - descPaneHeight - 68) + "px";
                                    $rootScope.$broadcast("resizeEditor", {});

                                    // bottom center panes
                                    var bottomCenterPaneHeight = newViewHeight - editorPaneTop - 100;
                                    document.getElementById("bottom-center-pane").style.height = bottomCenterPaneHeight + "px";
                                    document.getElementById("dataPanel").style.height = (bottomCenterPaneHeight - 3) + "px";
                                    document.getElementById("dataPanelBody").style.height = (bottomCenterPaneHeight - 34) + "px";
                                    document.getElementById("sampleData").style.width =
                                        (parseInt(document.getElementById("dataPanelBody").offsetWidth) - 4) + "px";
                                    document.getElementById("sampleData").style.height = (bottomCenterPaneHeight - 36) + "px";
                                    document.getElementById("consolePanel").style.height = (bottomCenterPaneHeight - 3) + "px";
                                    document.getElementById("consolePanelBody").style.height = (bottomCenterPaneHeight - 34) + "px";
                                    document.getElementById("console").style.width =
                                        (parseInt(document.getElementById("consolePanelBody").offsetWidth) - 4) + "px";
                                    document.getElementById("console").style.height = (bottomCenterPaneHeight - 36) + "px";

                                    var centerPanelsLeft = parseInt(document.getElementById("interpretationItem").style.left.slice(0, -2));
                                    var centerPanelsRight = parseInt(document.getElementById("centerPanels").style.right.slice(0, -2));
                                    document.getElementById("editorBody").style.width = (window.innerWidth - centerPanelsLeft - centerPanelsRight - 38) + "px";
                                    document.getElementById("editor").style.width = (window.innerWidth - centerPanelsLeft - centerPanelsRight - 42) + "px";

                                    document.getElementById("matchingNames").style.width =
                                        (parseInt(document.getElementById("matchingNamesPanelBody").offsetWidth) - 16) + "px";
                                    document.getElementById("matchingNames").style.height =
                                        (newViewHeight
                                        - parseInt(document.getElementById("matchingNamesPane").style.top.slice(0, -2)) - 183) + "px";

                                } catch (e) {
                                    console.log(e.toString());
                                }
                                break;
                            case "/startup":
                            case "/catalog":
                                document.getElementById("catalogPanel").style.height =
                                    newViewHeight - 123 + "px";
                                document.getElementById("schemaPanel").style.height =
                                    newViewHeight - 171 + "px";
                                document.getElementById("samplePanel").style.height =
                                    newViewHeight - 171 + "px";
                                document.getElementById("domainPanel").style.height =
                                    newViewHeight - 171 + "px";
                                document.getElementById("schemaPanelBody").style.height =
                                    newViewHeight - 246 + "px";
                                document.getElementById("samplePanelBody").style.height =
                                    newViewHeight - 197 + "px";
                                document.getElementById("domainPanelBody").style.height =
                                    newViewHeight - 246 + "px";
                                document.getElementById("schemaPanelTable").style.height =
                                    newViewHeight - 276 + "px";
                                document.getElementById("samplePanelTable").style.height =
                                    newViewHeight - 227 + "px";
                                document.getElementById("domainPanelTable").style.height =
                                    newViewHeight - 276 + "px";
                                break;
                            case "/schema":
                                document.getElementById("schemaDetailsContainer").style.height =
                                    newViewHeight - 45 + "px";
                                document.getElementById("schemaDetailsPanelBody").style.height =
                                    newViewHeight - 413 + "px";
                                break;
                            case "/sampleData":
                                document.getElementById("sampleDetailsContainer").style.height =
                                    newViewHeight - 45 + "px";
                                document.getElementById("sampleDetailsPanelBody").style.height =
                                    newViewHeight - 129 + "px";
                                document.getElementById("wizardDetailsPanelBody").style.height =
                                    newViewHeight - 129 + "px";
                                document.getElementById("wizardDetailsHbcCanvas").style.height =
                                    newViewHeight - 355 + "px";
                                document.getElementById("wizardDetailsVbcCanvas").style.height =
                                    newViewHeight - 355 + "px";
                                document.getElementById("wizardDetailsGphCanvas").style.height =
                                    newViewHeight - 355 + "px";
                                document.getElementById("wizardDetailsMapCanvas").style.height =
                                    newViewHeight - 355 + "px";
                                document.getElementById("wizardDetailsExampleCanvas").style.height =
                                    newViewHeight - 355 + "px";
                                break;
                            case "/wizardUploadSamples":
                                break;
                            case "/wizardInspectSamples":
                                document.getElementById("wizardInspectSamplesContainer").style.height =
                                    newViewHeight - 115 + "px";
                                document.getElementById("sampleDetailsPanelBody").style.height =
                                    newViewHeight - 199 + "px";
                                document.getElementById("wizardDetailsPanelBody").style.height =
                                    newViewHeight - 199 + "px";
                                document.getElementById("wizardDetailsHbcCanvas").style.height =
                                    newViewHeight - 425 + "px";
                                document.getElementById("wizardDetailsVbcCanvas").style.height =
                                    newViewHeight - 425 + "px";
                                document.getElementById("wizardDetailsGphCanvas").style.height =
                                    newViewHeight - 425 + "px";
                                document.getElementById("wizardDetailsMapCanvas").style.height =
                                    newViewHeight - 425 + "px";
                                document.getElementById("wizardDetailsExampleCanvas").style.height =
                                    newViewHeight - 425 + "px";
                                break;
                            case "/wizardMatchFields":
                                document.getElementById("wizardMatchFieldsContainer").style.height =
                                    newViewHeight - 107 + "px";
                                document.getElementById("wizardMatchFieldsProfilesPanelBody").style.height =
                                    newViewHeight - 193 + "px";
                                document.getElementById("wizardDetails1PanelBody").style.height =
                                    newViewHeight - 193 + "px";
                                document.getElementById("wizardDetails2PanelBody").style.height =
                                    newViewHeight - 193 + "px";
                                document.getElementById("wizardDetails1CanvasBody").style.maxHeight =
                                    newViewHeight - 422 + "px";
                                document.getElementById("wizardDetails2CanvasBody").style.maxHeight =
                                    newViewHeight - 422 + "px";
                                document.getElementById("wizardDetails1HbcCanvas").style.height =
                                    newViewHeight - 418 + "px";
                                document.getElementById("wizardDetails1VbcCanvas").style.height =
                                    newViewHeight - 418 + "px";
                                document.getElementById("wizardDetails1GphCanvas").style.height =
                                    newViewHeight - 418 + "px";
                                document.getElementById("wizardDetails1MapCanvas").style.height =
                                    newViewHeight - 418 + "px";
                                document.getElementById("wizardDetails1ExampleCanvas").style.height =
                                    newViewHeight - 418 + "px";
                                document.getElementById("wizardDetails2HbcCanvas").style.height =
                                    newViewHeight - 418 + "px";
                                document.getElementById("wizardDetails2VbcCanvas").style.height =
                                    newViewHeight - 418 + "px";
                                document.getElementById("wizardDetails2GphCanvas").style.height =
                                    newViewHeight - 418 + "px";
                                document.getElementById("wizardDetails2MapCanvas").style.height =
                                    newViewHeight - 418 + "px";
                                document.getElementById("wizardDetails2ExampleCanvas").style.height =
                                    newViewHeight - 418 + "px";
                                break;
                            case "/wizardFinalizeSchema":
                                document.getElementById("wizardFinalizeSchemaContainer").style.height =
                                    newViewHeight - 108 + "px";
                                document.getElementById("wizardFinalizeSchemaPanelBody").style.height =
                                    newViewHeight - 419 + "px";
                                break;
                            case "/wizardSave":
                                break;
                            default:
                                break;
                        }
                    }
                } catch (e) {
                    // if resizing fails because a view hasn't rendered yet
                    // then keep trying to resize it
                    $timeout($scope.resizeWizard, 300);
                }
            }; // resizeWizard

            angular.element($window).bind('resize', function () {
                $scope.resizeWizard();
            });
            // resizeInterpretation is a custom event triggered by split-pane-modified.js
            // in order to resize the content of split panes in the interpretation dialog.
            angular.element($window).bind('resizeInterpretation', function () {
                $scope.resizeWizard();
            });
            $scope.$on("resizeWizard", function (event, args) {
                $log.debug("onResizeWizard delay: " + args.delay);
                $timeout($scope.resizeWizard, args.delay);
            }); // onResizeWizard
            $scope.resizeWizard();

            $scope.previousDialog = "";
            $scope.navigateTo = function (path, param1, param2, param3) {
                $log.debug("navigateTo: " + path);
                $log.debug("parameters: " + param1 + ", " + param2 + ", " + param3);
                var fullPath = path;
                (param1 ? fullPath += '/:' + param1 : fullPath);
                (param2 ? fullPath += '/:' + param2 : fullPath);
                (param3 ? fullPath += '/:' + param3 : fullPath);
                $location.path(fullPath);
                $scope.path = path;
                if (path == "/catalog") {
                    document.getElementById('titleRef').style.pointerEvents = 'auto';
                    $rootScope.$broadcast("closeWebSocket", {});
                }
                if (path == "/wizardUploadSample" || "/startup") {
                    $rootScope.tabNumber = 1;
                }
                if (path == "/sampleData") {
                    $rootScope.tabNumber = 2;
                }
                $timeout($scope.resizeWizard, 300);
            }; // navigateTo
            $scope.browseSchema = function (schema) {
                //$log.debug("$scope.browseSchema: " + schema.sId);
                $scope.navigateTo("/schema", schema.sId);
            }; // browseSchema
            $scope.browseSample = function (sample, rtnMethod, rtnParm) {
                $scope.rtnMethod = rtnMethod;
                $scope.rtnParm = rtnParm;
                $scope.navigateTo("/sampleData", sample.dsId);
            }; // browseSample
            $scope.browseDomain = function (domain) {
                $scope.navigateTo("/:" + domain.dName + "/:" + domain.dId + "/interpretations");
            }; // browseDomain

            $scope.wizardStateControl = function (newState) {
                switch (newState) {
                    case "wizard-upload-samples-new-schema":
                        if (DomainInformation.isEmpty()) {
                            $scope.emptyDomainTitle = 'Empty Domain List';
                            var baseUrl = 'assets/help/Content/Products/';
                            $scope.emptyDomainUrl = $sce.trustAsResourceUrl(baseUrl + 'Domains/Empty Domains.htm');
                            var modal = myModals.alert('generic', $scope.emptyDomainTitle, 'small', $scope.emptyDomainUrl);
                            modal.result.then(function (data) {
                                // do nothing
                            }, function (data) {
                                $scope.modifySchemaMode = false;
                                $scope.schemaDomain = null;
                                $scope.wizardStateControl('wizard-upload-samples');
                                console.log("modal gone")
                            });
                        } else {
                            $scope.modifySchemaMode = false;
                            $scope.schemaDomain = null;
                            $scope.wizardStateControl('wizard-upload-samples');
                        }
                        break;
                    case "wizard-upload-samples-existing-schema":
                        $scope.modifySchemaMode = true;
                        $scope.schemaDomain = $scope.currentSchema.sDomainName;
                        $log.debug("wizard-upload-samples-existing-schema $scope.currentSchema.sDomainName: " + $scope.currentSchema.sDomainName);
                        $scope.wizardStateControl('wizard-upload-samples');
                        break;
                    case "wizard-upload-samples":
                        // open the websocket for progress bar updates
                        $rootScope.$broadcast("openWebSocket", {
                            sessionId: Server.getSessionId().sessionId
                        });
                        // clear working arrays each time the wizard starts
                        $scope.initializeModels();
                        $scope.navigateTo("/wizardUploadSamples");
                        break;
                    case "wizard-inspect-samples":
                        if ($cookies.get('schwiz.tours.inspectSample') !== "visited") {
                            $timeout(function () {
                                TourService.getTourByName('catalog').startAt('300');
                            }, 2500);
                            $cookies.put('schwiz.tours.inspectSample', "visited");
                        }
                        if ($scope.model.dataSamples.length == 0) {
                            $scope.currentSampleIndex -= 1;
                            $confirm({
                                    title: 'No data Samples',
                                    text: " There are no Data Samples to Inspect\nPress 'OK' to return back to the Catalog",
                                    ok: 'OK'
                                },
                                {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'}
                            ).then(function () {
                                $scope.fileIndex = -1;
                                $scope.navigateTo("/catalog");
                                $rootScope.$broadcast("closeWebSocket", {});
                            })
                        }
                        $scope.currentSampleIndex += 1;
                        $scope.fileIndex += 1;
                        if ($scope.currentSampleIndex < $scope.model.dataSamples.length) {
                            if (!$scope.model.dataSamples[$scope.currentSampleIndex].dsName) {
                                $confirm({
                                        title: 'Schema Wizard does not support this file type',
                                        text: "Cannot determine the format of the data sample, or does not support: "
                                        + $scope.sampleFiles[$scope.currentSampleIndex].name +
                                        "\n\nPress OK to discard and proceed to the next data sample.",
                                        ok: 'OK'
                                    },
                                    {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'}
                                ).then(function () {
                                    $scope.model.dataSamples.splice($scope.currentSampleIndex, 1);
                                    // decrement the index since we just created a hole
                                    $scope.currentSampleIndex -= 1;
                                    $scope.fileIndex -=1;
                                    $scope.wizardStateControl('wizard-inspect-samples');
                                })
                            }
                            //   }

                            $scope.model.dataSamples[$scope.currentSampleIndex].dsFileSize =
                                $scope.sampleFiles[$scope.currentSampleIndex].size;
                            $scope.currentSample = $scope.model.dataSamples[$scope.currentSampleIndex];

                            $scope.navigateTo("/wizardInspectSamples");
                            // broadcast to treeTableController (after waiting for it to become available in the dom)
                            $timeout(function () {
                                    $rootScope.$broadcast("setCurrentSample", {
                                    sample: $scope.currentSample
                                })}, 500);
                        } else {
                            $scope.wizardStateControl("wizard-match-fields");
                            $scope.fileIndex = -1;
                        }
                        break;
                    case "wizard-match-fields":
                        if ($cookies.get('schwiz.tours.matchFields') !== "visited") {
                            $timeout(function () {
                                TourService.getTourByName('catalog').startAt('400');
                            }, 2500);
                            $cookies.put('schwiz.tours.matchFields', "visited");
                        }
                        if ($scope.modifySchemaMode === true) {
                            $scope.addSchemaToModel();
                            //$log.debug("$scope.model.properties");
                            //$log.debug($scope.model.properties);
                        }
                        $log.debug("$scope.model.dataSamples");
                        $log.debug($scope.model.dataSamples);
                        // make a copy of data samples to preserve the originals
                        // repeatMatching() needs to start with the original copy each time it gets called
                        $scope.model.originalDataSamples = angular.copy($scope.model.dataSamples);
                        // reinitialize interpretationMatch
                        $scope.interpretationMatch = defaultInterpretationMatch;
                        $scope.addNewDataSamples($scope.model.dataSamples,
                                                 $scope.confidenceValues.selectedConfidenceValue,
                                                 $scope.interpretationMatch);

                        var foundDetailsToDisplay = false;
                        angular.forEach(Object.keys($scope.model.properties), function (property) {
                            if ($scope.model.properties[property].linkedDs.length > 1 &&
                                $scope.model.properties[property].linkedDs[0].dsProfile[property]['main-type'] == "number" && !foundDetailsToDisplay) {
                                $scope.showInDetails1($scope.model.properties[property].linkedDs[0], property, false);
                                $scope.showInDetails2($scope.model.properties[property].linkedDs[1], property, false);
                                foundDetailsToDisplay = true;
                            }
                        });
                        $scope.navigateTo("/wizardMatchFields");
                        break;
                    case "wizard-finalize-schema":

                        if ($cookies.get('schwiz.tours.finalizeSchema') !== "visited") {
                            $timeout(function () {
                                TourService.getTourByName('catalog').startAt('500');
                            }, 2500);
                            $cookies.put('schwiz.tours.finalizeSchema', "visited");
                        }
                        $log.debug("wizard-finalize-schema");

                        // interate through linked data sources to get alias names
                        angular.forEach(Object.keys($scope.model.properties), function (property) {
                            angular.forEach($scope.model.properties[property].linkedDs, function (linkedDs) {
                                if (!linkedDs.dsProfile[property]['alias-names']) {
                                    linkedDs.dsProfile[property]['alias-names'] = [];
                                }
                                linkedDs.dsProfile[property]['alias-names'].push(
                                    {
                                        "alias-name": linkedDs.dsProfile[property]['original-name'],
                                        "dsId": linkedDs.dsId
                                    });
                            })
                        });
                        // overwrite the original dataSamples with the possibly altered ones after matching
                        $log.debug($scope.model.dataSamples);
                        // open the websocket for progress bar updates
                        $rootScope.$broadcast("openWebSocket", {
                            sessionId: Server.getSessionId().sessionId
                        });
                        $scope.showMask();
                        // can't get this to work using $routeProvider and $resource
                        // TODO: try again in the future
                        var restURL =
                            $location.protocol() + "://" +
                            $location.host() + ":" +
                            $location.port() +
                            "/schwiz/rest/uploadModifiedSamples?" +
                            "schemaGuid=" + ($scope.modifySchemaMode ? $scope.currentSchema.sId : null) + "&" +
                            "domain=" + $scope.schemaDomain;
                        var schemaAnalysisData = {
                          "existing-schema": ($scope.modifySchemaMode ? $scope.currentSchema : null),
                          "data-samples" :  $scope.model.dataSamples
                        };
                        $log.debug(restURL);
                        $http({
                            method: 'POST',
                            url: restURL,
                            data: schemaAnalysisData
                        })
                        .success(function (data) {
                            $log.debug("post uploadModifiedSamples success");
                            $log.debug(data);
                            $scope.currentSchema = data;
                            if ($scope.currentSchema.sVersion == null) {
                                $scope.currentSchema.sVersion = "1.0";
                            }
                            $scope.hideMask();
                            $rootScope.$broadcast("closeWebSocket", {});
                            $scope.navigateTo("/wizardFinalizeSchema");
                        }, function (error) {
                            statusCodesFactory.get().$promise.then(function (response) {
                                $confirm(
                                    {
                                        title: response.updateDataSamplesFailed.title,
                                        text: response.updateDataSamplesFailed.title +
                                        " (" + error.status + ")",
                                        ok: 'OK'
                                    },
                                    {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                            })
                        });
                        break;
                    case "wizard-save":
                        $scope.navigateTo("/wizardSave");
                        break;
                    case "wizard-complete":
                        $log.debug("wizard-complete");
                        $log.debug($scope.currentSchema);
                        document.getElementById('titleRef').style.pointerEvents = 'auto';
                        // open the websocket for progress bar updates
                        $rootScope.$broadcast("openWebSocket", {
                            sessionId: Server.getSessionId().sessionId
                        });
                        $scope.showMask();
                        // can't get this to work using $routeProvider and $resource
                        // TODO: try again in the future
                        var restURL =
                            $location.protocol() + "://" +
                            $location.host() + ":" +
                            $location.port() +
                            "/schwiz/rest/schema";
                        $log.debug(restURL);
                        $http({
                            method: 'POST',
                            url: restURL,
                            data: $scope.currentSchema
                        })
                        .success(function (data) {
                            $log.debug("post saveSchema success");
                            $log.debug(data);
                            $scope.hideMask();
                            $rootScope.$broadcast("closeWebSocket", {});
                            $scope.navigateTo("/catalog");
                        });
                        break;
                    default:
                        break;
                }
            }; // wizardStateControl

            /* Titlebar Menu Items */
            $scope.useLeidosTheme = function ($event, use) {
                if ($event) $event.preventDefault();
                for (var i = 0; i < document.styleSheets.length; i++) {
                    if (document.styleSheets[i].href &&
                        document.styleSheets[i].href.indexOf("leidos-theme.css") > 0) {
                        document.styleSheets[i].disabled = !use;
                        $cookies.put('schwiz.theme', (use ? 'leidos' : 'digitalEdge2'));
                        break;
                    }
                }
            }; // useLeidosTheme
            $scope.useLeidosTheme(null,
                (!$cookies.get('schwiz.theme') ? true : $cookies.get('schwiz.theme') === 'leidos'));

            $scope.launchHelp = function () {
                var childWindowForHelp = window.open('assets/help/Default.htm', "", "width=950,height=850");
                childWindowForHelp.moveTo(300, 50);
            }; // launchHelp

            $scope.launchAbout = function () {
                $scope.successMessage = 'About-Coming Soon';
                var modal = myModals.alert('generic', $scope.successMessage, 'small');
                modal.result.then(function (data) {
                    // do something with data on close
                }, function (data) {
                    // do something on dismiss
                })
            };  // signout

            $scope.clearDetails = function (dropzone) {
                $scope.dropzoneModels['selected' + dropzone] = null;
                delete $scope.dropzoneModels.dropzones['zone' + dropzone][0];
            }; // clearDetails

            // tour 'previous' button
            $scope.onTourPrev = function (tour) {
                console.log('Moving back...', tour);
                console.log(tour.getCurrentStep().order);
                switch (tour.getCurrentStep().order) {
                    case 30:
                        $rootScope.$broadcast("selectTab", {tabNumber: 2});
                        break;
                    case 40:
                        $rootScope.$broadcast("selectTab", {tabNumber: 3});
                        break;
                    default:
                        $rootScope.$broadcast("selectTab", {tabNumber: 1});
                        break;
                }
            }; // onTourPrev

            // tour 'next' button
            $scope.onTourNext = function (tour) {
                console.log('Moving next...', tour);
                console.log(tour.getCurrentStep().order);
                switch (tour.getCurrentStep().order) {
                    case 10:
                        $rootScope.$broadcast("selectTab", {tabNumber: 2});
                        break;
                    case 20:
                        $rootScope.$broadcast("selectTab", {tabNumber: 3});
                        break;
                    default:
                        $rootScope.$broadcast("selectTab", {tabNumber: 1});
                        break;
                }
            }; // onTourNext

            // tour 'end' button
            $scope.onTourEnd = function (tour) {
                console.log('Ending tour', tour);
                $cookies.put('schwiz.tours.catalog', "visited");
            }; // onTourNext

            // get the sample files from fileUploadCtrl
            $scope.$on("sampleFilesSelected", function (event, args) {
                $scope.sampleFiles = args.newSampleFiles;
                $log.debug("sampleFilesSelected:");
                $log.debug($scope.sampleFiles);
            }); // onSampleFilesSelected

            // get the data sources from fileUploadCtrl
            $scope.$on("dataSamplesReceived", function (event, args) {
                $scope.hideMask();
                $log.debug("dataSamplesReceived: " + args.newDataSamples);
                $scope.model.dataSamples = args.newDataSamples;
                // get a dump of the data samples by uncommenting the following line
                //$log.debug(angular.toJson($scope.model.dataSamples, true));
                // initialize index used to iterate through inspection of samples
                $scope.currentSampleIndex = -1;
                $scope.wizardStateControl('wizard-inspect-samples');
            }); // ondataSamplesReceived

            $scope.$on("setCurrentSample", function (event, args) {
                $log.debug("onSetCurrentSample");
                $log.debug(args);
                $log.debug(args.sample);
                $scope.currentSample = args.sample;
            }); // onsetCurrentSample

            $scope.$on("setCurrentSchema", function (event, args) {
                $log.debug("onSetCurrentSchema");
                $log.debug(args.schema);
                $scope.currentSchema = args.schema;
                //TODO dont use rootscope to try and resolve scoping issue before broadcast
                $rootScope.test = $scope.currentSchema;
            }); // onsetCurrentSchema

            $scope.$on("setSchemaDomain", function (event, args) {
                $log.debug("onSetSchemaDomain");
                $log.debug(args.schemaDomain);
                $scope.schemaDomain = args.schemaDomain;
            }); // onsetCurrentSchema

            $scope.$on("schemaUpdate", function (event, args) {
                $log.debug("onSchemaUpdate");
                $log.debug(args.schema);
                $scope.currentSchema = args.schema;
            }); // onSchemaUpdate

            $scope.showCurrentSchema = function () {
                $log.debug("showCurrentSchema");
                $log.debug($scope.currentSchema);
            }; // showCurrentSchema

            $scope.addSchemaToModel = function () {
                $log.debug("addSchemaToModel");
                $log.debug($scope.currentSchema);
                angular.forEach(Object.keys($scope.currentSchema.sProfile), function (property) {
                    $log.debug("adding property: " + property);
                    $scope.model.properties[property] = $scope.currentSchema.sProfile[property];
                    $scope.model.properties[property]["interpretations"] = [];
                    for (var i = 0; i < $scope.currentSchema.sProfile[property].interpretations.length; i++) {
                        if ($scope.model.properties[property].interpretations.indexOf($scope.currentSchema.sProfile[property].interpretations[i].iName) === -1) {
                            $scope.model.properties[property].interpretations.push($scope.currentSchema.sProfile[property].interpretations[i]);
                        }
                    }
                    $scope.model.properties[property]["linkedDs"] = [];
                    $scope.model.properties[property]["existing-schema-property"] = true;
                });
                $log.debug("$scope.model.properties");
                $log.debug($scope.model.properties);
            }; // addSchemaToModel

            $scope.addPropertyToModel = function (dataSample, property) {
                $log.debug("addPropertyToModel property: " + property);
                if ($scope.modifySchemaMode) $log.debug(dataSample);
                //$log.debug(dataSample);
                if (!$scope.model.properties.hasOwnProperty(property)) {
                    // add the property and an array for linking data samples
                    $scope.model.properties[property] =
                        {"display-name": dataSample.dsProfile[property]["display-name"], "interpretations": [], "linkedDs": []};
                    $scope.model.properties[property]["existing-schema-property"] = false;
                    // since first ds in linked list, mark it as seed and not merged
                    dataSample.dsProfile[property]['used-in-schema'] = true;
                    dataSample.dsProfile[property]['merged-into-schema'] = false;
                } else {
                    dataSample.dsProfile[property]['used-in-schema'] = false;
                    dataSample.dsProfile[property]['merged-into-schema'] = true;
                }
                for (var i = 0; i < dataSample.dsProfile[property].interpretations.length; i++) {
                    if ($scope.model.properties[property].interpretations.indexOf(dataSample.dsProfile[property].interpretations[i].iName) === -1) {
                        $scope.model.properties[property].interpretations.push(dataSample.dsProfile[property].interpretations[i].iName);
                    }
                }
                // unless a seed property, mark for merging
                if (!dataSample.dsProfile[property]['used-in-schema']) {
                    dataSample.dsProfile[property]['merged-into-schema'] = true;
                }
                // add a link to the data sample
                $scope.model.properties[property].linkedDs.push(dataSample);
            }; // addPropertyToModel

            $scope.repeatMatching = function (interpretationMatch) {
                $log.debug("confidenceThreshold");
                $scope.interpretationMatch = interpretationMatch;
                //TODO: make this work with modify existing schema
                // can't do this when modifying existing schema, would need to start with addSchemaToModel
                if (!$scope.modifySchemaMode) {
                    $scope.model.properties = {};
                    //$rootScope.$apply();
                    $timeout(function () {
                        $log.debug("$scope.confidenceValues.selectedConfidenceValue");
                        $log.debug($scope.confidenceValues.selectedConfidenceValue);
                        $log.debug("$scope.interpretationMatch: " + $scope.interpretationMatch);
                        // start matching over using the original copy of the data samples
                        $scope.model.dataSamples = angular.copy($scope.model.originalDataSamples);
                        $log.debug("$scope.model.dataSamples");
                        $log.debug($scope.model.dataSamples);
                        $scope.addNewDataSamples($scope.model.dataSamples,
                                                 $scope.confidenceValues.selectedConfidenceValue,
                                                 $scope.interpretationMatch);
                        var foundDetailsToDisplay = false;
                        angular.forEach(Object.keys($scope.model.properties), function (property) {
                            if ($scope.model.properties[property].linkedDs.length > 1 &&
                                $scope.model.properties[property].linkedDs[0].dsProfile[property]['main-type'] == "number" && !foundDetailsToDisplay) {
                                $scope.showInDetails1($scope.model.properties[property].linkedDs[0], property, false);
                                $scope.showInDetails2($scope.model.properties[property].linkedDs[1], property, false);
                                foundDetailsToDisplay = true;
                            }
                        });
                    }, 300)
                }
            }; // repeatMatching

            // TODO: test interpretation match in the future
            $scope.matchesFieldInOtherDataSample = function(interpretationsToMatch, fieldToMatch, newDataSamples) {
                $log.debug("matchesFieldInOtherDataSample");
                $log.debug("interpretationsToMatch: " + angular.toJson(interpretationsToMatch));
                $log.debug("fieldToMatch: " + fieldToMatch);
                foundMatch = false;
                findMatch: for (var i = 0; i < interpretationsToMatch.length; i++) {
                    for (var j = 0; j < newDataSamples.length; j++) {
                        if (newDataSamples[j].dsProfile.hasOwnProperty(fieldToMatch)) {
                            for (var k = 0; k < newDataSamples[j].dsProfile[fieldToMatch].interpretations.length; k++) {
                                foundMatch = foundMatch
                                    || (newDataSamples[j].dsProfile.hasOwnProperty(fieldToMatch)
                                    && interpretationsToMatch[i].iName
                                        === newDataSamples[j].dsProfile[fieldToMatch].interpretations[k].iName
                                    && interpretationsToMatch[i].iName != "Unknown");
                                if (foundMatch) break findMatch;
                            }
                        }
                    }
                }
                return foundMatch;
            }; // matchesFieldInOtherDataSample

            $scope.addNewDataSamples = function (newDataSamples,  confidenceThreshold, interpretationMatch) {
                $log.debug("addNewDataSamples");
                $log.debug(newDataSamples);
                angular.forEach(newDataSamples, function (newDs) {
                    var propertiesToDelete = [];
                    angular.forEach(Object.keys(newDs.dsProfile), function (property) {
                        // build drop-down listbox for alternate names
                        newDs.dsProfile[property]['match-names'] =
                            {availableOptions: [], selectedOption: null, previousOption: null};
                        for (var i = 0; i < newDs.dsProfile[property]['matching-fields'].length; i++) {
                            // TODO: test interpretation match in the future
                            if (!interpretationMatch
                                || $scope.matchesFieldInOtherDataSample(
                                    newDs.dsProfile[property].interpretations,
                                    newDs.dsProfile[property]['matching-fields'][i]['matching-field'],
                                    newDataSamples)) {
                                newDs.dsProfile[property]['match-names']['availableOptions'].push(
                                    {
                                        id: i,
                                        name: newDs.dsProfile[property]['matching-fields'][i]['matching-field'] +
                                        ':' +
                                        newDs.dsProfile[property]['matching-fields'][i]['confidence']
                                    }
                                )
                            }
                        }
                        // examine alternate names and their confidence factor
                        // if high and not a seed property then change the name of data
                        // sample's property to the alternate name and delete the original
                        for (var i = 0; i < newDs.dsProfile[property]['matching-fields'].length; i++) {
                            var matchingField = newDs.dsProfile[property]['matching-fields'][i];

                            // if the data sample property being examined is the same as
                            // the alternate name and exists in the properties model
                            // then stop examining the alternates
                            if (matchingField['matching-field'] == property &&
                                $scope.model.properties.hasOwnProperty(matchingField['matching-field'])) break;

                            // if the data sample has a property which is the same as
                            // this alternate name then move on to the next alternate name
                            if (newDs.dsProfile.hasOwnProperty(matchingField['matching-field'])) continue;

                            // if this alternate name has high confidence then rename
                            // this property to that alternate name
                            if (matchingField['confidence'] >= confidenceThreshold
                                && ((interpretationMatch
                                     && newDs.dsProfile[property].interpretations[0].interpretation
                                        != "Unknown")
                                    || !interpretationMatch
                                   )) {
                                if ($scope.model.properties.hasOwnProperty(matchingField['matching-field'])) {
                                    newDs.dsProfile[property]['original-name'] = property;
                                    newDs.dsProfile[property]['used-in-schema'] = false;
                                    newDs.dsProfile[property]['merged-into-schema'] = true;
                                    // select this matching-field in the drop-down listbox
                                    newDs.dsProfile[property]['match-names'].selectedOption =
                                    {
                                        id: i,
                                        name: newDs.dsProfile[property]['matching-fields'][i]['matching-field'] +
                                        ':' +
                                        newDs.dsProfile[property]['matching-fields'][i]['confidence']
                                    };
                                    // save current selection for an undo
                                    newDs.dsProfile[property]['match-names'].previousOption =
                                        newDs.dsProfile[property]['match-names'].selectedOption;
                                    // show the original property name at the top of the drop-down listbox
                                    newDs.dsProfile[property]['match-names']['availableOptions'].unshift(
                                        {id: 99, name: property});
                                    newDs.dsProfile[matchingField['matching-field']] = angular.copy(newDs.dsProfile[property]);
                                    // delete later otherwise the loop index will be incorrect
                                    propertiesToDelete.push(property);
                                    break;
                                }
                            }
                        }
                        for (var i = 0; i < propertiesToDelete.length; i++) {
                            delete newDs.dsProfile[propertiesToDelete[i]];
                        }
                    });
                    angular.forEach(Object.keys(newDs.dsProfile), function (property) {
                        $scope.addPropertyToModel(newDs, property);
                    });
                });
                $log.debug("$scope.model");
                $log.debug($scope.model);
            }, function (error) {
                statusCodesFactory.get().$promise.then(function (response) {
                    $confirm(
                        {
                            title: response.failedToAddNewDataSample.title,
                            text: response.failedToAddNewDataSample.title +
                            " (" + error.status + ")",
                            ok: 'OK'
                        },
                        {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                })
            }; // addNewDataSamples

            $scope.changeMatchedProperty = function (dataSample, property) {
                $log.debug("changeMatchField");
                $log.debug(dataSample);
                $log.debug(property);
                $log.debug(dataSample.dsProfile[property]['match-names'].selectedOption);
                var newName = dataSample.dsProfile[property]['match-names'].selectedOption.name
                    .substring(0, dataSample.dsProfile[property]['match-names'].selectedOption.name.indexOf(":"));
                $log.debug("newName: " + newName);

                // check if the new name already exists.
                if (dataSample.dsProfile.hasOwnProperty(newName)) {
                    $confirm(
                        {
                            title: 'Merge Operation',
                            text: "The property '" + newName + "' already exists in data sample: '" +
                            dataSample.dsName + "'",
                            ok: 'OK'
                        },
                        {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'});
                    // undo user selection to previous selection
                    dataSample.dsProfile[property]['match-names'].selectedOption =
                        dataSample.dsProfile[property]['match-names'].previousOption;
                    return;
                }

                var linkedDs = $scope.model.properties[property].linkedDs;
                $scope.removeFromDetailsPanels(dataSample.dsProfile[property]);
                // remove data sample from current property linked list
                var removedDs;
                for (var i = 0; i < linkedDs.length; i++) {
                    if (linkedDs[i] == dataSample) {
                        removedDs = linkedDs.splice(i, 1)[0];
                        break;
                    }
                }
                if (dataSample.dsProfile[property]['match-names'].selectedOption.id == 99) {
                    newName = removedDs.dsProfile[property]['original-name'];
                    delete removedDs.dsProfile[property]['original-name'];
                } else if (!removedDs.dsProfile[property]['original-name']) {
                    removedDs.dsProfile[property]['original-name'] = property;
                    // show the original property name at the top of the drop-down listbox (only once)
                    if (removedDs.dsProfile[property]['match-names']['availableOptions'][0].name != property) {
                        removedDs.dsProfile[property]['match-names']['availableOptions'].unshift(
                            {id: 99, name: property});
                    }
                }
                // save current selection for an undo
                removedDs.dsProfile[property]['match-names'].previousOption =
                    removedDs.dsProfile[property]['match-names'].selectedOption;
                $log.debug("newName: " + newName);
                removedDs.dsProfile[newName] =
                    angular.copy(removedDs.dsProfile[property]);
                delete removedDs.dsProfile[property];
                $log.debug("linkedDs");
                $log.debug(linkedDs);
                if (linkedDs.length == 0 && $scope.modifySchemaMode === false) {
                    delete $scope.model.properties[property];
                } else {
                    // rebuild the interpretations for this property based on the linked data samples
                    $scope.model.properties[property].interpretations = [];
                    for (var i = 0; i < linkedDs.length; i++) {
                        for (var j = 0; j < linkedDs[i].dsProfile[property].interpretations.length; j++) {
                            if ($scope.model.properties[property].interpretations.indexOf(linkedDs[i].dsProfile[property].interpretations[j].iName) === -1) {
                                $scope.model.properties[property].interpretations.push(linkedDs[i].dsProfile[property].interpretations[j].iName);
                            }
                        }
                    }
                }
                // add restored property to property model
                $scope.addPropertyToModel(removedDs, newName);
            }, function (error) {
                statusCodesFactory.get().$promise.then(function (response) {
                    $confirm(
                        {
                            title: response.failedToMatchProperty.title,
                            text: response.failedToMatchProperty.title +
                            " (" + error.status + ")",
                            ok: 'OK'
                        },
                        {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                })
            }; // changeMatchedProperty

            $scope.highlightIfInDetails = function (property) {
                if (property) {
                    if (property['shown-in-details1']) return {"background-color": "gold", "cursor": "pointer"};
                    if (property['shown-in-details2']) return {"background-color": "lightsalmon", "cursor": "pointer"};
                }
            }; // highlightIfInDetails

            $scope.removeFromDetailsPanels = function (property) {
                // remove data sample property from details displays
                if (property['shown-in-details1']) {
                    $scope.detailModels.detailPanels.panel1 = [];
                    property['shown-in-details1'] = false;
                }
                if (property['shown-in-details2']) {
                    $scope.detailModels.detailPanels.panel2 = [];
                    property['shown-in-details2'] = false;
                }
            };

            $scope.showInGenericDetails = function (dataSource, property) {
                $log.debug("showInGenericDetails");
                $log.debug(dataSource);
                $log.debug(property);
                // interate through dataSource properties to turn off shown-in-details
                angular.forEach(Object.keys(dataSource.dsProfile), function (property) {
                    dataSource.dsProfile[property]['shown-in-details'] = false;
                });
                dataSource.dsProfile[property]['shown-in-details'] = true;
                $scope.detailModels.detailPanels.panel1 = [];
                $scope.detailModels.detailPanels.panel1.push(dataSource.dsProfile[property]);
                $scope.detailModels.detailPanels.panel1[0]["dsName"] = dataSource.dsName;
                $scope.detailModels.detailPanels.panel1[0]["property-name"] = property;
                // set the default viz for the histogram
                if ($scope.detailModels.detailPanels.panel1[0].detail['freq-histogram'].type == "map") {
                    $scope.detailModels.detailPanels.panel1[0].viz = "map";
                }
                else if($scope.detailModels.detailPanels.panel1[0].detail['detail-type'] == "text") {
                    $scope.detailModels.detailPanels.panel1[0].viz = "example";
                }
                else {
                    $scope.detailModels.detailPanels.panel1[0].viz = "hbc";
                }
                $log.debug("detailModels.detailPanels.panel1[0]");
                $log.debug($scope.detailModels.detailPanels.panel1[0]);
                $log.debug(dataSource);
            }; // showInGenericDetails

            $scope.showInDetails1 = function (dataSource, property, schemaProperty) {
                $log.debug("showInDetails1");
                $log.debug(dataSource);
                $log.debug(property);
                    // interate through working model properties to turn off shown-in-details1
                    angular.forEach(Object.keys($scope.model.properties), function (property) {
                        $scope.model.properties[property]['shown-in-details1'] = false;
                    });
                    // interate through linked data sources to turn off shown-in-details1
                    angular.forEach(Object.keys($scope.model.properties), function (property) {
                        angular.forEach($scope.model.properties[property].linkedDs, function (linkedDs) {
                            linkedDs.dsProfile[property]['shown-in-details1'] = false;
                        })
                    });
                    $scope.detailModels.detailPanels.panel1 = [];
                    if (!schemaProperty) {
                        $scope.detailModels.detailPanels.panel1.push(dataSource.dsProfile[property]);
                        dataSource.dsProfile[property]['shown-in-details1'] = true;
                    } else {
                        $scope.detailModels.detailPanels.panel1.push(dataSource[property]);
                        dataSource[property]['shown-in-details1'] = true;
                    }
                    $scope.detailModels.detailPanels.panel1[0]["dsName"] = dataSource.dsName;
                    $scope.detailModels.detailPanels.panel1[0]["property-name"] = property;
                    $scope.detailModels.detailPanels.panel1[0]["hideUseBtn"] = schemaProperty;
                    $scope.detailModels.detailPanels.panel1[0]["hideMergeBtn"] = schemaProperty;
                    // set the default viz for the histogram
                    if ($scope.detailModels.detailPanels.panel1[0].detail['freq-histogram'].type == "map") {
                        $scope.detailModels.detailPanels.panel1[0].viz = "map";
                    }
                    else if($scope.detailModels.detailPanels.panel1[0].detail['detail-type'] == "text") {
                        $scope.detailModels.detailPanels.panel1[0].viz = "example";
                    }
                    else {
                        $scope.detailModels.detailPanels.panel1[0].viz = "hbc";
                    }
                    if (dataSource.dsProfile[property]['original-name']) {
                        $scope.detailModels.detailPanels.panel1[0]["confidence"] =
                            dataSource.dsProfile[property]['matching-fields'][0]['confidence'];
                    }
                    $log.debug("detailModels.detailPanels.panel1[0]");
                    $log.debug($scope.detailModels.detailPanels.panel1[0]);
            }; // showInDetails1

            $scope.showInDetails2 = function (dataSource, property) {
                $log.debug("showInDetails2");
                $log.debug(dataSource);
                $log.debug(property);
                    // interate through linked data sources to turn off shown-in-details2
                    angular.forEach(Object.keys($scope.model.properties), function (property) {
                        angular.forEach($scope.model.properties[property].linkedDs, function (linkedDs) {
                            linkedDs.dsProfile[property]['shown-in-details2'] = false;
                        })
                    });
                    $scope.detailModels.detailPanels.panel2 = [];
                    $scope.detailModels.detailPanels.panel2.push(dataSource.dsProfile[property]);
                    dataSource.dsProfile[property]['shown-in-details2'] = true;
                    $scope.detailModels.detailPanels.panel2[0]["dsName"] = dataSource.dsName;
                    $scope.detailModels.detailPanels.panel2[0]["property-name"] = property;
                    // set the default viz for the histogram
                    if ($scope.detailModels.detailPanels.panel2[0].detail['freq-histogram'].type == "map") {
                        $scope.detailModels.detailPanels.panel2[0].viz = "map";
                    }
                    else if($scope.detailModels.detailPanels.panel2[0].detail['detail-type'] == "text") {
                        $scope.detailModels.detailPanels.panel2[0].viz = "example";
                    }
                    else {
                        $scope.detailModels.detailPanels.panel2[0].viz = "hbc";
                    }
                    if (dataSource.dsProfile[property]['original-name']) {
                        $scope.detailModels.detailPanels.panel2[0]["confidence"] =
                            dataSource.dsProfile[property]['matching-fields'][0]['confidence'];
                    }
                    $log.debug("detailModels.detailPanels.panel2[0]");
                    $log.debug($scope.detailModels.detailPanels.panel2[0]);
            }; // showInDetails2

            $scope.editFieldName = function ($event, oldName, newName) {
                $scope.editFieldNames = function () {
                    document.getElementById("wizard-finalize-schema-back").disabled = true;
                    $log.debug("editFieldName oldName: '" + oldName + "', newName: '" + newName + "'");
                    $scope.currentSchema.sProfile[newName] =
                        angular.copy($scope.currentSchema.sProfile[oldName]);
                    $scope.currentSchema.sProfile[newName]['display-name'] = newName;
                    $scope.currentSchema.sProfile[newName]['original-name'] = oldName;
                    // add the existing name to the list of alias names
                    if (!$scope.currentSchema.sProfile[newName]['alias-names']) {
                        $scope.currentSchema.sProfile[newName]['alias-names'] = [];
                        $scope.currentSchema.sProfile[newName]['alias-names'].push(
                            {"alias-name": newName, "dsId": null});
                    } else {
                        // don't add a duplicate
                        var aliasExists = false;
                        for (var i = 0; i < $scope.currentSchema.sProfile[newName]['alias-names'].length; i++) {
                            if ($scope.currentSchema.sProfile[newName]['alias-names'][i]['alias-name'] === newName) {
                                aliasExists = true;
                                break;
                            }
                        }
                        if (!aliasExists) {
                            $scope.currentSchema.sProfile[newName]['alias-names'].push(
                                {"alias-name": newName, "dsId": null});
                        }
                    }
                    delete $scope.currentSchema.sProfile[oldName];
                    $log.debug($scope.currentSchema);
                    return newName;
                }, function (error) {
                    statusCodesFactory.get().$promise.then(function (response) {
                        $confirm(
                            {
                                title: response.failedToEditFieldName.title,
                                text: response.failedToEditFieldName.title +
                                " (" + error.status + ")",
                                ok: 'OK'
                            },
                            {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                    })
                };
                $log.debug($event);
                $log.debug("oldName: '" + oldName + "'   newName: '" + newName + "'");
                if (oldName !== newName && newName !== '') {
                    if (document.getElementById("wizard-finalize-schema-back").disabled) {
                        $scope.editFieldNames();
                    } else {
                        $confirm({
                                title: 'Confirm Edit Field Name',
                                text: "The 'Back' button will be disabled if a field \n" +
                                "name is changed. Press 'OK' to confirm.",
                                ok: 'OK',
                                cancel: 'Cancel'
                            }
                        ).then(function () {
                            $scope.editFieldNames();
                        }, function (error) {
                            statusCodesFactory.get().$promise.then(function (response) {
                                $confirm(
                                    {
                                        title: response.editFieldNameFailed.title,
                                        text: response.editFieldNameFailed.title +
                                        " (" + error.status + ")",
                                        ok: 'OK'
                                    },
                                    {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                            })
                        });
                    }
                    return oldName;
                } else {
                    return oldName;
                }
            };// editFieldName

            $scope.addNewField = function () {
                $scope.addNewFields = function () {
                    document.getElementById("wizard-finalize-schema-back").disabled = true;
                    $scope.currentSchema.sProfile['New-field'] = {};
                    $scope.currentSchema.sProfile['New-field']["manually-added"] = true;
                    $scope.currentSchema.sProfile['New-field']["main-type"] = "string";
                    $scope.currentSchema.sProfile['New-field']["presence"] = -1;
                    $scope.currentSchema.sProfile['New-field']["detail"] = {};
                    $scope.currentSchema.sProfile['New-field']["detail"]["detail-type"] = "phrase";
                    $log.debug($scope.currentSchema);

                    // need some time until DOM finishes
                    $timeout($scope.focusNewField, 250);
                };
                if (document.getElementById("wizard-finalize-schema-back").disabled) {
                    $scope.addNewFields();
                } else {
                    $confirm({
                            title: 'Confirm Add New Field',
                            text: "The 'Back' button will be disabled if a field \n" +
                            "name is changed. Press 'OK' to confirm.",
                            ok: 'OK',
                            cancel: 'Cancel'
                        }
                    ).then(function () {
                        $scope.addNewFields();
                    }, function (error) {
                        statusCodesFactory.get().$promise.then(function (response) {
                            $confirm(
                                {
                                    title: response.addNewFieldFailed.title,
                                    text: response.addNewFieldFailed.title +
                                    " (" + error.status + ")",
                                    ok: 'OK'
                                },
                                {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                        })
                    });
                }
            }; // addNewField

            $scope.focusNewField = function () {
                var theForm = document.getElementById(
                    "fieldNameForm" + (Object.keys($scope.currentSchema.sProfile).length - 1));
                theForm.elements.newName.scrollTop = 0;
                theForm.elements.newName.focus();
                theForm.elements.newName.select();
            }; // focusNewField

            $scope.setMenuTop = function (arr) {
                var styleObj = "{top: " + (-arr.length * 22) + "px !important;}";
                //$log.debug("setMenuTop");
                //$log.debug(styleObj);
                return styleObj;
            }; // setMenuTop

            $scope.useInSchema = function (property) {
                $log.debug("useInSchema property: " + property);
                var linkedDs = $scope.model.properties[property["property-name"]].linkedDs;
                for (var i = 0; i < linkedDs.length; i++) {
                    var dsProfile = linkedDs[i].dsProfile;
                    for (var key in linkedDs[i].dsProfile) {
                        if (key == property["property-name"]) {
                            dsProfile[key]["used-in-schema"] = false;
                            dsProfile[key]["merged-into-schema"] = false;
                        }
                    }
                }
                property['merged-into-schema'] = false;
                property['used-in-schema'] = true;
            }; // useInSchema

            $scope.useAsInSchema = function (ds, property) {
                $log.debug("useAsInSchema property: " + property);
                $log.debug(ds);
            }; // useAsInSchema

            $scope.mergeIntoSchema = function (property) {
                $log.debug("mergeIntoSchema");
                property['merged-into-schema'] = true;
            }; // mergeIntoSchema

            $scope.mergeAsIntoSchema = function (ds, property) {
                $log.debug("mergeAsIntoSchema property: " + property);
                $log.debug(ds);
            }; // mergeAsIntoSchema

            $scope.discardDataSource = function (currentSampleIndex) {
                $confirm({
                        title: 'Confirm Discard Data Source',
                        text: "Press 'OK' to confirm.",
                        ok: 'OK',
                        cancel: 'Cancel'
                    }
                ).then(function () {
                    $log.debug("discardDataSource currentSampleIndex: " + currentSampleIndex);
                    $log.debug($scope.model.dataSamples[currentSampleIndex]);
                    // delete the linked data source and any properties it uniquely introduced
                    angular.forEach(Object.keys($scope.model.properties), function (property) {
                        var linkedDs = $scope.model.properties[property].linkedDs;
                        for (var i = 0; i < linkedDs.length; i++) {
                            if (linkedDs[i] == $scope.model.dataSamples[$scope.currentSampleIndex]) {
                                $log.debug("Found dataSource linkedDs index: " + i);
                                $log.debug(linkedDs[i]);
                                // if this is the only linked data souce then it introduced this property
                                // so delete the property
                                if (linkedDs.length == 1) {
                                    $log.debug("before");
                                    $log.debug($scope.model.properties);
                                    delete $scope.model.properties[property];
                                    $log.debug("after");
                                    $log.debug($scope.model.properties);
                                }
                                linkedDs.splice(i, 1);
                            }
                        }
                    });
                    $scope.model.dataSamples.splice(currentSampleIndex, 1);
                    // decrement the index since we just created a hole
                    $scope.currentSampleIndex -= 1;
                    $scope.wizardStateControl('wizard-inspect-samples');
                }, function (error) {
                    statusCodesFactory.get().$promise.then(function (response) {
                        $confirm(
                            {
                                title: response.failedToDiscardDataSource.title,
                                text: response.failedToDiscardDataSource.title +
                                " (" + error.status + ")",
                                ok: 'OK'
                            },
                            {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                    })
                });
            }; // discardDataSource

            $scope.removeProperty = function (property) {
                $confirm({
                        title: 'Confirm Remove Property',
                        text: "Press 'OK' to confirm.",
                        ok: 'OK',
                        cancel: 'Cancel'
                    }
                ).then(function () {
                    var linkedDs = $scope.model.properties[property].linkedDs;
                    for (var i = 0; i < linkedDs.length; i++) {
                        $scope.removeFromDetailsPanels(linkedDs[i].dsProfile[property]);
                        delete linkedDs[i].dsProfile[property]
                    }
                    delete $scope.model.properties[property];
                    if ($scope.modifySchemaMode === true) {
                        delete $scope.currentSchema.sProfile[property]
                    }
                }, function (error) {
                    statusCodesFactory.get().$promise.then(function (response) {
                        $confirm(
                            {
                                title: response.failedToRemoveProperty.title,
                                text: response.failedToRemoveProperty.title +
                                " (" + error.status + ")",
                                ok: 'OK'
                            },
                            {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                    })
                })
            }; // removeProperty

            var canvasBase = document.getElementById('panel1base');
            var canvasBar = document.getElementById('panel1bar');
            var canvasLine = document.getElementById('panel1line');
            if (canvasBase||canvasBar||canvasLine){
                var ctx = canvas.getContext('2d');
                ctx.clearRect(0,0, ctx.canvas.width, ctx.canvas.height);
            }

            $scope.removeDs = function (index) {
                $confirm({
                        title: 'Confirm Remove Data Sample',
                        text: "Press 'OK' to confirm.",
                        ok: 'OK',
                        cancel: 'Cancel'
                    }
                ).then(function () {
                    angular.forEach(Object.keys($scope.model.properties), function (property) {
                        var linkedDs = $scope.model.properties[property].linkedDs;
                        for (var i = 0; i < linkedDs.length; i++) {
                            if (linkedDs[i] == $scope.model.dataSamples[index]) {
                                $scope.removeFromDetailsPanels($scope.model.dataSamples[index].dsProfile[property]);
                                linkedDs.splice(i, 1);
                            }
                        }
                        if (linkedDs.length == 0) {
                            delete $scope.model.properties[property];
                        }
                    });
                    $scope.model.dataSamples.splice(index, 1);
                }, function (error) {
                    statusCodesFactory.get().$promise.then(function (response) {
                        $confirm(
                            {
                                title: response.failedToRemoveDataSample.title,
                                text: response.failedToRemoveDataSample.title +
                                " (" + error.status + ")",
                                ok: 'OK'
                            },
                            {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                    })
                });
            }; // removeDs

            $scope.confidenceValues = {
                selectedConfidenceValue: $scope.confidenceThreshold.toString(),
                availableValues: []
            };
            for (var i = 100; i > 80; i--) {
                $scope.confidenceValues.availableValues.push( { value: i });
            }

            $scope.dragEnd = function (property, obj) {
                for (var i = 1; i < 4; i++) {
                    if (angular.equals($scope.dropzoneModels.dropzones["zone" + i][0], obj)) {
                        $scope.dropzoneModels.dropzones["zone" + i][0]["property-name"] = property;
                        $scope.dropzoneModels["selected" + i] = obj;
                        $scope.dropzoneModels.dropzones["zone" + i].splice(1, 1);
                    }
                }
            }; // dragEnd

            $scope.sort = function (obj, type, caseSensitive) {
                var temp_array = [];
                for (var key in obj) {
                    if (obj.hasOwnProperty(key)) {
                        if (!caseSensitive) {
                            key = (key['toLowerCase'] ? key.toLowerCase() : key);
                        }
                        temp_array.push(key);
                    }
                }
                if (typeof type === 'function') {
                    temp_array.sort(type);
                } else if (type === 'value') {
                    temp_array.sort(function (a, b) {
                        var x = obj[a];
                        var y = obj[b];
                        if (!caseSensitive) {
                            x = (x['toLowerCase'] ? x.toLowerCase() : x);
                            y = (y['toLowerCase'] ? y.toLowerCase() : y);
                        }
                        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
                    });
                } else {
                    temp_array.sort();
                }
                var temp_obj = {};
                for (var i = 0; i < temp_array.length; i++) {
                    temp_obj[temp_array[i]] = obj[temp_array[i]];
                }
                return temp_obj;
            }; // sort
        }]); // schemaWizardCtrl

    schemaWizardApp.directive('singleClick', ['$parse', '$timeout', function ($parse, $timeout) {
        return {
            restrict: 'A',
            link: function (scope, element, attr) {
                var fn = $parse(attr['singleClick']);
                var clicks = 0, timer = null;
                element.on('click', function (event) {
                    clicks++;  //count clicks
                    if (clicks === 1) {
                        timer = $timeout(function () {
                            fn(scope, {$event: event});
                            clicks = 0;         //after action performed, reset counter
                        }, 300);
                    } else {
                        $timeout.cancel(timer);
                        clicks = 0;             //after action performed, reset counter
                    }
                });
            }
        };
    }]); // singleClick

})();
