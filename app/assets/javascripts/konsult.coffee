ourModule = angular.module "konsult", ['LocalStorageModule', 'restangular', 'xeditable'] # 
  
LineCtrl = ($scope, localStorageService, Restangular) ->
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
    Restangular.one('users', $scope.user_id).all('reports').post { lines: $scope.lines } 
     
HistoryCtrl = ($scope, Restangular, localStorageService) ->
  $scope.fetch = -> 
    $scope.reports = Restangular.one('users',  $scope.user_id).getList('reports')
  $scope.saveReport = (rapport) ->
    rapport.put()
    
ourModule.controller
  Lines: LineCtrl
  History: HistoryCtrl

ourModule.run (editableOptions) -> editableOptions.theme = 'bs3'

ourModule.config ['$routeProvider', ($routeProvider) ->
  $routeProvider.when "/",
    templateUrl: "assets/Partials/rapportera.html"
    controller: LineCtrl
  .when "/rapporter"
    templateUrl: "assets/Partials/rapporter.html"
  .otherwise 
    redirectTo: "/"
]
      