/**
 * 
 */
var geocoder;
var pos;
var map;
function initialize() {
  geocoder = new google.maps.Geocoder();
  var mapOptions = {
    zoom: 6,
    mapTypeId: google.maps.MapTypeId.ROADMAP
  };
  map = new google.maps.Map(document.getElementById('map-canvas'),
      mapOptions);

  // Get location using HTML5 geolocation
  if(navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(function(position) {
      pos = new google.maps.LatLng(position.coords.latitude,
                                       position.coords.longitude);

      var infowindow = new google.maps.InfoWindow({
        map: map,
        position: pos,
        content: 'You are here'
      });
      var marker = new google.maps.Marker({
          map: map,
          position: pos
      });
      map.setCenter(pos);
    }, function() {
      handleNoGeolocation(true);
    });
  } else {
    // Browser doesn't support Geolocation
    handleNoGeolocation(false);
  }
}

function handleNoGeolocation(errorFlag) {
  if (errorFlag) {
    var content = 'Error: The Geolocation service failed.';
  } else {
    var content = 'Error: Your browser doesn\'t support geolocation.';
  }

  var options = {
    map: map,
    position: new google.maps.LatLng(60, 105),
    content: content
  };

  var infowindow = new google.maps.InfoWindow(options);
  map.setCenter(options.position);
}

function codeAddress(address) {
  geocoder.geocode( { 'address': address}, function(results, status) {
    if (status == google.maps.GeocoderStatus.OK) {
      map.setCenter(results[0].geometry.location);
      var marker = new google.maps.Marker({
          map: map,
          position: results[0].geometry.location,
      });
    } else {
      alert('Geocode was not successful for the following reason: ' + status);
    }
  });
}

function calculateDistances(origins, destinations) {
  if(origins.length < 1)
	  origins.push(pos);
  var service = new google.maps.DistanceMatrixService();
  service.getDistanceMatrix(
    {
      origins: origins,
      destinations: destinations,
      travelMode: google.maps.TravelMode.DRIVING,
      unitSystem: google.maps.UnitSystem.METRIC,
      avoidHighways: false,
      avoidTolls: false
    }, callback);
}

function callback(response, status) {
  var totalDistance = 0;
  if (status != google.maps.DistanceMatrixStatus.OK) {
    alert('Failed calculating distance due to: ' + status);
  } else {
//    var origins = response.originAddresses;
//    var destinations = response.destinationAddresses;
	
	//Distance in km  
    var distance = parseInt(response.rows[0].elements[0].distance.value)/1000;
    
    //Current total distance
    var totalDistanceValue = parseInt(document.getElementById('totalDist').innerHTML);
    
    //Calc new total and display
    totalDistanceValue += distance;
    document.getElementById('totalDist').innerHTML = totalDistanceValue;
    
  }
}

google.maps.event.addDomListener(window, 'load', initialize);