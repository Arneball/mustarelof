		var myApp = angular.module('timeReportApp',[]);
		
		myApp.directive('calendar', function () {
		            return {
		                require: 'ngModel',
		                link: function ($scope, el, attr, ngModel) {
		                    $(el).datepicker({
		                        dateFormat: 'yy-mm-dd',
				                firstDay: 1,
				                showWeek: true,
		                        onSelect: function (dateText, inst) {
		                            $scope.setDate(dateText);
		                        }
		                    });
		                }
		            };
		        });
		
		/* $(function(){$('.teddie').resizable();}); */
		myApp.directive('teddie', function(){
			return {
				restrict: 'A',
				link: function(scope, element, attr){
					$(element).resizable({		
						start: function(event, ui){
							
						},
						stop: function(event, ui){
							var tdWidth = $('#timeTable').find('td').first().width();
							console.log(tdWidth);
							//var orgWidth = ui.originalSize.width;
							//var orgHeight = ui.originalSize.height;
							var curWidth = ui.size.width;
							var corr = 0;
							
							var numSelected = Math.ceil(curWidth/tdWidth);
							console.log(curWidth+'/'+tdWidth+'='+numSelected);
							$(element).css('width', numSelected*(tdWidth+corr));
						}
					});
	                $(element).resize(function() {
	                	$(element).addClass('active');
	                });
	                $(element).click(function() {
	                	$(element).addClass('active');
	                });
				}
			};
		}); 
function TimeReportCtrl($scope) {
	$scope.dateSelected = '';
	
	$scope.setDate = function(date){
		console.log('setDate called ' +date);
		$scope.dateSelected = date;
		$scope.$apply();
	}
}