describe('test broadcast', function () {
    var $controller, $rootScope;

    beforeEach(function() {
        module('schemaWizardApp');
        inject(function (_$rootScope_, _$controller_) {
            $rootScope = _$rootScope_;
            spyOn($rootScope, '$broadcast');
            $controller = _$controller_;
        });
    });

    it("see if broadcast is working for current sample", function () {
        $controller('sampleDetailsCtrl', {

            $scope: $rootScope.$new(),

            sampleData: {}
        })
        expect($rootScope.$broadcast).toHaveBeenCalledWith('setCurrentSample', {sample:{}})});
})
