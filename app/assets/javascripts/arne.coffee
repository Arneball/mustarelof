ourModule = angular.module "testmodule", []

ourModule.controller 'TestController', ($scope) ->
  $scope.data = [{
  	must: 3
  	name: "arne"
  }, {
  	must: 4
  	name: "slaskarn"
  }]
  $scope.title = "Hej title"

ourModule.controller 'Header', ($scope) ->
  $scope.labels = [{
  		a: "http://google.com"
  		name: "Gammelgoogel"
  	}, {	
  		a: "http://altavista.com",
  		name: "hastalavista"
	}]