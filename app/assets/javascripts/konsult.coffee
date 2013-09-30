ourModule = angular.module "konsult", ['LocalStorageModule'] # 

ourModule.controller
  Lines: ($scope, localStorageService) ->
    $scope.lines = localStorageService.get("lines") or []
    $scope.saveLocally = -> 
      localStorageService.set "lines", $scope.lines

    $scope.clearLocalStore = -> 
      localStorageService.remove "lines"
      $scope.lines = []

    $scope.addLine = -> 
      $scope.lines.push angular.copy $scope.newline

    $scope.showModal = (line) ->
      $scope.modal = line
      $('#myModal').modal('toggle')
