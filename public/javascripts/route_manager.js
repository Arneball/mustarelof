/**
 * 
 */

function RouteManager($scope){
	$scope.locations = [
		{
			name: 'Helsingborg',
			address: 'Helsingborg, Sverige',
			active:false,
			lastDestination:false
		},{
			name: 'Lund',
			address: 'Lund, Sverige',
			active:false,
			lastDestination:false
		},{
			name: 'Kiruna',
			address: 'Kiruna, Sverige',
			active:false,
			lastDestination:false
		},{
			name: 'Stockholm',
			address: 'Stockholm, Sverige',
			active:false,
			lastDestination:false
		}
	];

	$scope.toggleActive = function(currentLocation){
		//Set active
		currentLocation.active = !currentLocation.active;
		
		//Code address and set marker
		codeAddress(currentLocation.address);
		
		//Calculate distance from last active location
		var origins = [];
		var destinations = [];

		angular.forEach($scope.locations, function(loc){
			if(loc.lastDestination == true){
				origins.push(loc.address);
				calculateFromPos = false;
			}
		});

		destinations.push(currentLocation.address);
		currentLocation.lastDestination = true;

		calculateDistances(origins, destinations);
	};
}