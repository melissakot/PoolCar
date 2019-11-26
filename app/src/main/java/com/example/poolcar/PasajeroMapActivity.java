package com.example.poolcar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class PasajeroMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mLogout, mRequest, mSettings;

    private LatLng pickupLocation;

    private Boolean requestBol = false;
    private Boolean isLoggingOut = false;

    private Marker pickupMarker;

    private SupportMapFragment mapFragment;

    private String destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pasajero_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(PasajeroMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
//            return;
        } else {
            mapFragment.getMapAsync(this);
        }


        mLogout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.request);
        mSettings = (Button) findViewById(R.id.settings);

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut = true;
                FirebaseAuth.getInstance().signOut();
                mGoogleApiClient.disconnect();
                mMap.stopAnimation();
                Intent intent = new Intent(PasajeroMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");//("pedido del pasajero");
                GeoFire geoFire = new GeoFire(ref);

                if (requestBol) {
                    requestBol = false;

                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if (driverFoundID != null) {
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("CustomerRequest");
                        driverRef.setValue(true);
                        driverFoundID = null;
                    }
                    driverFound = false;
                    radius = 1;
                    geoFire.removeLocation(userId, new
                            GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    Boolean a = false;
                                    //Do some stuff if you want to
                                }
                            });

                    if (pickupMarker != null) {
                        pickupMarker.remove();
                    }
                    mRequest.setText("Llamar al conductor");
                } else {
                    requestBol = true;
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new
                            GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
//                                    Boolean a = false;
                                    //Do some stuff if you want to
                                }
                            });

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Recoger aquí"));

                    mRequest.setText("Buscando tu conductor...");

                    getClossestDriver();
                }
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PasajeroMapActivity.this, PasajeroSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        /**
         * Initialize Places. For simplicity, the API key is hard-coded. In a production
         * environment we recommend using a secure mechanism to manage API keys.
         */
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyBIRJQawafcAqAjJyDFfgt_Nja6LOCBZEY");
            // Create a new Places client instance.
            PlacesClient placesClient = Places.createClient(this);
        }

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName().toString();

//                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("Error", "An error occurred: " + status);
            }
        });

    }

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;
    private GeoQuery geoQuery;

    private void getClossestDriver() {
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("DriversAvailable");
        //Ver si hay que poner driversWorking en lugar de Available,
        GeoFire geoFire = new GeoFire((driverLocation));

        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        //Busca un conductor para el viaje
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestBol) {
                    driverFound = true;
                    driverFoundID = key;

//                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Conductores").child(driverFoundID);
                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("CustomerRequest");
                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("CustomerRideId", customerID);
                    if (!destination.equals("")) {
                        map.put("CustomerDestination", destination);
                    }
                    driverRef.updateChildren(map);
                    mRequest.setText("Buscando la ubicación del conductor");
                    getDriverLocation();


                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            //Si no encuentra un conductor en radio de 1 km, aumenta el radio y vuelve a ejecutar la funcion
            //de búsqueda de conductor
            @Override
            public void onGeoQueryReady() {

                if (!driverFound) {
                    radius++;
//                    getClossestDriver();
                    geoQuery.setRadius(radius);
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
//                System.err.println("There was an error with this query: " + error);
            }
        });

    }

    //Muestra al pasajero dodne esta el conductor
    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    private void getDriverLocation() {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("DriversWorking").child(driverFoundID).child("1");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            //Se llama a esta funcion cada vez que la ubicacion cambia
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Conductor Encontrado");
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    Float distance = loc1.distanceTo(loc2);

                    if (distance>100) {
                        mRequest.setText("Conductor Encontrado a: " + String.valueOf(distance) + " Mts");
                    }else{
                        mRequest.setText("El Conductor esta AQUI ");
                    }

                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Tu conductor"));

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
//                System.err.println("There was an error in getDriverLocation(): " + databaseError.getMessage().toString());
            }
        });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(PasajeroMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
//            return;
        }

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null && !isLoggingOut) {
            mLastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(PasajeroMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
//            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    final int LOCATION_REQUEST_CODE = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mapFragment.getMapAsync(this);
                } else {
                    Toast.makeText(getApplicationContext(), "Brinde el permiso necesario", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }
}
