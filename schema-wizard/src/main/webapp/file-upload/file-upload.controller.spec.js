describe('test broadcast for new file opload ', function () {
    var $controller, $rootScope;

    beforeEach(function () {
        module('schemaWizardApp');
        inject(function (_$rootScope_, _$controller_) {
            $rootScope = _$rootScope_;
            spyOn($rootScope, '$broadcast').and.callThrough();
            $controller = _$controller_;
        });
    });

    it("should select/upload new sample files", function () {
        $controller('fileUploadCtrl', {

            $scope: $rootScope.$new(),

        })
         $rootScope.$broadcast('sampleFilesSelected', { 'newSampleFiles': {} });
        expect($rootScope.$broadcast).toHaveBeenCalledWith('sampleFilesSelected', {newSampleFiles: {}})
    });


    it("should recieve data samples", function () {
        $controller('fileUploadCtrl', {

            $scope: $rootScope.$new(),

        })
        $rootScope.$broadcast('dataSamplesReceived', { 'newDataSamples': {} });
        expect($rootScope.$broadcast).toHaveBeenCalledWith('dataSamplesReceived', {newDataSamples: {}})

    });
})
