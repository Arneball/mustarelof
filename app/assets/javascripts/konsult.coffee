ourModule = angular.module "konsult", ['LocalStorageModule', 'ngResource', 'xeditable'] # 


services = 
  testDataService: ($resource) ->
    @reshandler = $resource "/reports/:that_user_id", {that_user_id: "@user_id"} 

    @saveReport = (user_id, lines, callback) -> 
      @reshandler.save {user_id: user_id, lines: lines}, callback

    @getReports = (user_id, callback)-> 
      @reshandler.query {that_user_id: user_id}, callback
  
LineCtrl = ($scope, $resource, localStorageService, testDataService) ->
  $scope.lines = localStorageService.get("lines") or []
  $scope.saveLocally = -> 
    localStorageService.set "lines", $scope.lines

  $scope.clearLocalStore = -> 
    localStorageService.remove "lines"
    $scope.lines = []

  $scope.addLine = ->
    $scope.lines.push angular.copy $scope.newline
    $scope.saveLocally()

  $scope.showModal = (line) ->
    $scope.modal = line
    $('#myModal').modal('toggle')
  
  $scope.savereport = -> 
    testDataService.saveReport $scope.user_id, $scope.lines, -> $scope.clearLocalStore()
     
HistoryCtrl = ($scope, $resource, localStorageService, testDataService) ->
  $scope.fetch = -> 
    testDataService.getReports $scope.user_id, (res) -> $scope.reports = res 
    
ourModule.controller
  Lines: LineCtrl
  History: HistoryCtrl

ourModule.run (editableOptions) -> editableOptions.theme = 'bs3'
ourModule.service services
ourModule.config ['$routeProvider', ($routeProvider) ->
  $routeProvider.when "/",
    templateUrl: "assets/Partials/rapportera.html"
    controller: LineCtrl
  .when "/rapporter"
    templateUrl: "assets/Partials/rapporter.html"
  .otherwise 
    redirectTo: "/"
]
      