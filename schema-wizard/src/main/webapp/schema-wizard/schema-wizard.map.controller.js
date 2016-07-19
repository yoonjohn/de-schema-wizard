(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.controller('mapCtrl', function ($scope, $interval, $log) {
        $scope.createMap = function(width, height, colors, jsonData) {
            //$log.debug("createMap");
            //$log.debug(jsonData);
            if (jsonData) {
                var map = { type: "GeoChart", data: [] };
                map.data.push([jsonData.cols[0].label, jsonData.cols[1].label]);
                jsonData.rows.forEach(function (current) {
                    map.data.push([current.c[0].v, current.c[1].v])
                });
                map.options = { width: width, height: height, colorAxis: {colors: colors} };
                //$log.debug("map");
                //$log.debug(map);
                return map;
            }
        }; // createMap
    }); // mapCtrl

})();
