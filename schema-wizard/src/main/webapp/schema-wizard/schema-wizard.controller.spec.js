describe('schemaWizardApp', function () {
    var testScope;
// test to see if schema wizard is initialized
    beforeEach(function () {
        module('schemaWizardApp');
        inject(function ($controller, $rootScope) {
            testScope = $rootScope.$new();
            $controller('schemaWizardCtrl', {
                $scope: testScope
            });
        });
    });

    it('should success fully initiate data', function () {
        expect(testScope.userid).toBe('wizard-user');
    });
});

describe('Testing that all the broadcasts are working in the schema-wizard.controller', function () {
    var $scope = null;
    var ctrl = null;


    beforeEach(module('schemaWizardApp'));

    it('sample file has been selected broadcast recieved, schema-wizard.controller', inject(function ($rootScope, $controller) {
        $scope = $rootScope.$new();

        ctrl = $controller('schemaWizardCtrl', {
            $scope: $scope
        });

        $scope.$broadcast('sampleFilesSelected', {'newSampleFiles': true});
        $scope.$digest();
        expect($scope.sampleFiles).toBe(true);
    }));


    //it('data sample has been recieved, schema-wizard.controller', inject(function($rootScope, $controller) {
    //    $scope = $rootScope.$new();
    //
    //    ctrl = $controller('schemaWizardCtrl', {
    //        $scope: $scope
    //    });
    //
    //    $scope.$broadcast('dataSamplesReceived', { 'newDataSamples' : true } );
    //    $scope.$digest();
    //    expect($scope.model.sampleFiles).toBe(true);
    //}));


    it('current sample has been selected, schema-wizard.controller', inject(function ($rootScope, $controller) {
        $scope = $rootScope.$new();

        ctrl = $controller('schemaWizardCtrl', {
            $scope: $scope
        });

        $scope.$broadcast('setCurrentSample', {'sample': true});
        $scope.$digest();
        expect($scope.currentSample).toBe(true);
    }));


    //it('catalog has been updated, schema-wizard.controller', inject(function($rootScope, $controller) {
    //    $scope = $rootScope.$new();
    //
    //    ctrl = $controller('schemaWizardCtrl', {
    //        $scope: $scope
    //    });
    //    //TODO - define objects for catalog
    //
    //    $scope.$broadcast('catalogUpdate', { 'catalog.schemaCatalog' : true } );
    //    $scope.$digest();
    //    expect($scope.catalog.schemaCatalog).toBe(true);
    //}));


    it('set current schema, schema-wizard.controller', inject(function ($rootScope, $controller) {
        $scope = $rootScope.$new();

        ctrl = $controller('schemaWizardCtrl', {
            $scope: $scope
        });

        $scope.$broadcast('setCurrentSchema', {'schema': true});
        $scope.$digest();
        expect($scope.currentSchema).toBe(true);
    }));


    it('on Schema Update, schema-wizard.controller', inject(function ($rootScope, $controller) {
        $scope = $rootScope.$new();

        ctrl = $controller('schemaWizardCtrl', {
            $scope: $scope
        });

        $scope.$broadcast('schemaUpdate', {'schema': true});
        $scope.$digest();
        expect($scope.currentSchema).toBe(true);
    }));
});


