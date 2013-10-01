ourModule = angular.module "konsult", ['LocalStorageModule', 'ngResource'] # 

LineCtrl = ($scope, $resource, localStorageService) ->
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
    res = $resource "/reports/:that_user_id", {that_user_id: "@user_id"}
    res.save {user_id: $scope.user_id, lines: $scope.lines}, console.log 
    
ourModule.controller
  Lines: LineCtrl
   
ourModule.config ['$routeProvider', ($routeProvider) ->
  $routeProvider.when "/",
    templateUrl: "assets/Partials/rapportera.html"
    controller: LineCtrl
  .when "/rapporter"
    templateUrl: "assets/Partials/rapporter.html"
  .otherwise 
    redirectTo: "/"
]
      