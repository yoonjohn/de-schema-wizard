(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.service("Globals", function () {
        var detailModels = {
            selected1: null,
            selected2: null,
            dataList: {"data": []},
            detailPanels: {"panel1": [], "panel2": []}
        };
        this.setDetailModels = function (detMdls) {
            detailModels = detMdls;
        };
        this.getDetailModels = function () {
            return detailModels;
        }
    }); // Globals

    schemaWizardApp.service("Utilities", function ($log) {
        this.showInGenericDetails = function (Globals, profile, property) {
            $log.debug("showInGenericDetails property: " + property);
            //$log.debug(profile);
            profile['shown-in-details'] = true;
            var detailModels = Globals.getDetailModels();
            detailModels.detailPanels.panel1 = [];
            detailModels.detailPanels.panel1.push(profile);
            //$log.debug("detailModels.detailPanels.panel1[0]");
            //$log.debug(detailModels.detailPanels.panel1[0]);
            detailModels.detailPanels.panel1[0]["dsName"] = null;
            detailModels.detailPanels.panel1[0]["property-name"] = property;
            // set the default viz for the histogram
            if (detailModels.detailPanels.panel1[0].detail['freq-histogram'].type == "map") {
                detailModels.detailPanels.panel1[0].viz = "map";
            } else if(detailModels.detailPanels.panel1[0].detail['detail-type'] == "text") {
                detailModels.detailPanels.panel1[0].viz = "example";
            } else {
                detailModels.detailPanels.panel1[0].viz = "hbc";
            }
        } // showInGenericDetails
    }); // Utilities

})();
