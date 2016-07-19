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

    it("if the catalog updates - broadcast", function () {
        $controller('catalogCtrl', {

            $scope: $rootScope.$new(),

            catalogData: {}
        })
        expect($rootScope.$broadcast).toHaveBeenCalledWith('catalogUpdate', {catalog:{}})});
})
