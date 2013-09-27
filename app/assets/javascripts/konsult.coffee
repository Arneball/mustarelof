ourModule = angular.module "konsult", [] # empty array is for some kind of dependency injection

ourModule.controller
  Lines: ($scope) ->
    $scope.lines = [{customer: "Arne", hours: 3, extras: []}]
    $scope.addLine = () -> 
      {customer: c, hours: h, extras: e} = $scope
      $scope.lines.push
        customer: c
        hours: h
        extras: e

    $scope.showModal = (line) ->
      $scope.modal = line
      $('#myModal').modal('toggle')