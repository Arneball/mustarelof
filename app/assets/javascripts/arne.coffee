ourModule = angular.module "testmodule", [] # empty array is for some kind of dependency injection

controllers = 
  TestController: ($scope, testDataService) ->
    $scope.data = testDataService.testData()
    $scope.title = "Hej title"
  
  Mustare: ($scope) ->
    $scope.initDynamic = () ->
      $scope.view = "assets/Partials/list.html"
      $scope.data = ["ein", "swine", "dry"]

  Header: ($scope, testDataService) ->
    $scope.labels = testDataService.testLabels()

  WsTest: ($scope, testDataService) ->
    $scope.initData = () -> testDataService.getFromWs().then (wsdata) -> 
      $scope.data = wsdata
      $scope.arraysum = wsdata.reduce (acc, item) -> acc + item

services =
  testDataService: ($http) ->
    this.testData = () -> [{
        must: 3  
        name: "arne"
      }, {
        must: 4
        name: "slaskarn"
      }]

    this.testLabels = () -> [{
      a: "http://google.com"
      name: "Gammelgoogel"
    }, {  
      a: "http://altavista.com",
      name: "hastalavista"
    }]

    this.getFromWs = () -> 
      promise = $http.get("/test").then (response) -> response.data
      promise # return promise

ourModule.config ($routeProvider) ->
  $routeProvider.when "/",
    templateUrl: "assets/Partials/routedefault.html"
  .when "/route1",
    templateUrl: "assets/Partials/route1.html"
  .when "/route2",
    templateUrl: "assets/Partials/route2.html"
  .otherwise 
    redirectTo: "/"

ourModule.controller controllers
ourModule.service services