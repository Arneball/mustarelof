ourModule = angular.module "konsult", ['LocalStorageModule', 'restangular', 'xeditable'] # 

serialize = (obj) ->
  console.log obj
  str = []
  for p of obj when obj.hasOwnProperty p
    do (p) -> 
      str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]))
   tmp = str.join "&"
   console.log str
   tmp

ourModule.filter 'serialize', () -> serialize


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
  $scope.getPdf = (rapport) ->
    window.location = "/users/#{ $scope.user_id }/reports/#{ rapport.id }/pdf"
  $scope.getPdf2 = (rapport) ->
    window.location = "/users/#{ $scope.user_id }/reports/#{ rapport.id }/pdf2"

FbController = ($scope) ->
  initFb = ->
    $scope.facebook = serialize
      client_id: "184407735081979"
      redirect_uri: "http://skandal.dyndns.tv:9000/users/#{ $scope.email }/fblogin"
  $scope.updateUrl = -> initFb()
      
ourModule.controller
  Lines: LineCtrl
  History: HistoryCtrl
  Fb: FbController

ourModule.run (editableOptions) -> editableOptions.theme = 'bs3'

ourModule.config ['$routeProvider', ($routeProvider) ->
  $routeProvider.when "/",
    templateUrl: "assets/Partials/rapportera.html"
    controller: LineCtrl
  .when "/rapporter"
    templateUrl: "assets/Partials/rapporter.html"
  .when "/login"
    templateUrl: "assets/Partials/login.html"
  .otherwise 
    redirectTo: "/"
]
      
