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

    it("when the schema is updated-broadcast", function () {
        $controller('wizardFinalizeCtrl', {

            $scope: $rootScope.$new(),

            schemaData: {}
        })
        expect($rootScope.$broadcast).toHaveBeenCalledWith('schemaUpdate', {schema:{}})});
})
