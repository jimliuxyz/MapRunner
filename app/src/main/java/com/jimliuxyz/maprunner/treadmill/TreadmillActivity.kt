package com.jimliuxyz.maprunner.treadmill

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.jimliuxyz.maprunner.R
import com.jimliuxyz.maprunner.utils.orientation


class TreadmillActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    companion object {
        private val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_ADMIN)
        private val REQUEST_ALL_PERMISSION = 7

        private var presenter: Presenter? = null
    }
    private lateinit var googleMap: GoogleMap
    private lateinit var googleApi: GoogleApiClient

    private lateinit var mapFragment: RecMapFragment
    private lateinit var cpanelFragment: CPanelFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (orientation() == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_treadmill_landscope)
        } else if (orientation() == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_treadmill)
        }

        cpanelFragment = supportFragmentManager.findFragmentById(R.id.cpanel) as CPanelFragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as RecMapFragment

        if (savedInstanceState == null && presenter != null){
            presenter!!.deInit()
            presenter = null
        }
        presenter = presenter ?: Presenter()

//        if (googleMap == null)
            mapFragment.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onMapReady(map: GoogleMap?) {
        if (map != null) {
            googleMap = map!!

            if (hasAllPermissions())
                init()
            else
                requestAllPermissions()

        } else {
            Toast.makeText(applicationContext, "Map cannot be Ready!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onConnected(p0: Bundle?) {
        println("gapi onConnected")
    }

    override fun onConnectionSuspended(p0: Int) {
        println("gapi onConnectionSuspended")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        println("gapi onConnectionFailed")
    }

    @SuppressLint("MissingPermission")
    private fun init() {
        googleApi = GoogleApiClient.Builder(application.applicationContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
//        googleApi.connect()
        googleMap.setMyLocationEnabled(true)

        presenter!!.init(mapFragment, cpanelFragment, googleApi)
    }

    private fun hasAllPermissions(): Boolean {
        for (permission in PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(PERMISSIONS,
                    REQUEST_ALL_PERMISSION)
        }
//        ActivityCompat.requestPermissions(this,
//                PERMISSIONS,
//                REQUEST_ALL_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size == 0) return

        var grant_cnt = 0
        for (res in grantResults) {
            if (res == PackageManager.PERMISSION_GRANTED)
                grant_cnt += 1
        }
        when (requestCode) {

            REQUEST_ALL_PERMISSION -> {

                println("REQUEST_ALL_PERMISSION : ${PERMISSIONS.size} ${grantResults.size} ${grant_cnt}")

                if (grant_cnt == PERMISSIONS.size)
                    init()
                else
                    finish()
            }
            else -> {
                println("System request permission : ${PERMISSIONS.size} ${grantResults.size} ${grant_cnt}")

                if (grant_cnt == permissions.size)
                    init()
                else
                    finish()
            }
        }
    }

    fun clickBt(view: View) {
        presenter?.onClickBluetooth()
    }

    fun clickSpdDn(view: View) {
        presenter?.onClickSpeed(-5.0)
    }

    fun clickSpdUp(view: View) {
        presenter?.onClickSpeed(5.0)
    }

}
