ourModule = angular.module "konsult", ['LocalStorageModule', 'restangular', 'xeditable'] # 

serialize = (obj) ->
  str = []
  for p of obj when obj.hasOwnProperty p
    do (p) -> 
      str.push "#{ encodeURIComponent(p) }=#{ encodeURIComponent(obj[p]) }"
   str.join "&"

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

FbController = ($scope, Restangular) ->
  init = (type, disableProperty) -> Restangular.one("hasData", $scope.email).one(type).get().then (result) ->
    {user_has: should_fail} = result
    $scope[disableProperty] = should_fail
  
  initFb = ->
    init "fb", "disableFb"
    $scope.facebook = serialize # request parameters
      client_id: "184407735081979"
      redirect_uri: "http://skandal.dyndns.tv:9000/users/#{ $scope.email }/fblogin"
  
  initGoogle = ->
    init "google", "disableGoogle"
    $scope.google = serialize # request paramaters
      client_id: "311906667213.apps.googleusercontent.com"
      redirect_uri: "http://skandal.dyndns.tv:9000/gmaillogin"
      response_type: "code"
      state: $scope.email
      scope: "https://www.googleapis.com/auth/userinfo.profile"
    
  initLinkedin = ->
    init "linkedin", "disableLinkedin"
    $scope.linkedin = serialize # request parameters
      response_type: "code"
      client_id: "or5btja04vjl"
      state: $scope.email
      redirect_uri: "http://skandal.dyndns.tv:9000/linkedinlogin"
        
  $scope.updateForm = -> initFb(); initGoogle(); initLinkedin()
  gotoUrl = (url) -> window.location.href = url 
  
  $scope.submitLinkedin = -> gotoUrl "https://www.linkedin.com/uas/oauth2/authorization?#{ $scope.linkedin }"
  $scope.submitFb = -> gotoUrl "https://graph.facebook.com/oauth/authorize?#{ $scope.facebook }"
  $scope.submitGoogle = -> gotoUrl "https://accounts.google.com/o/oauth2/auth?#{ $scope.google }"
  
  $scope.dummy = ->
    Restangular.one("dummy").one($scope.newemail).get().then (res) ->
      alert(res.errtext) if !res.success 
      
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
      
