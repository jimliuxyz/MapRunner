package com.jimliuxyz.maprunner.treadmill

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.jimliuxyz.maprunner.utils.direction.DirNode


class RecMapFragment : SupportMapFragment(), Contract.IRecMap, OnMapReadyCallback {

    private val ZOOM = 17f
    private var marker: Marker? = null
    private var polydirs: Polyline? = null

    private lateinit var googleMap: GoogleMap
    private lateinit var googleApi: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getMapAsync(this)
    }

    private var last_dir: List<DirNode>? = null
    private var last_marker: LatLng? = null
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        last_dir?.let {
            outState.putParcelableArrayList("last_dir", ArrayList<DirNode>(it))
        }
        last_marker?.let {
            outState.putParcelable("last_marker", last_marker)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            last_dir = savedInstanceState.getParcelableArrayList("last_dir")
            last_marker = savedInstanceState.getParcelable("last_marker")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        val googleMap = googleMap ?: return
        this.googleMap = googleMap!!

        last_dir?.let {
            showDirection(it)
        }
        last_marker?.let {
            moveMarker(it.latitude, it.longitude)
        }
    }

    override fun showDirection(nodes: List<DirNode>) {
        last_dir = nodes
        activity?.runOnUiThread() {
            val rectLine = PolylineOptions().width(10f).color(Color.BLUE)

            for (node in nodes) {
                rectLine.add(node.toLatLng())
            }

            polydirs?.remove()
            polydirs = googleMap.addPolyline(rectLine)
        }
    }

    var appendPolyline: PolylineOptions?=null
    override fun appendDirection(node: DirNode) {
        var starting_node = last_dir?.takeIf { appendPolyline==null&&it.isNotEmpty() }?.let{it.last()}

        appendPolyline = appendPolyline?:PolylineOptions().width(10f).color(Color.RED)

        var nodes = (last_dir?.let { it } ?: listOf<DirNode>()).toMutableList()
        last_dir = nodes
        nodes.add(node)

        activity?.runOnUiThread() {
            starting_node?.let {
                appendPolyline!!.add(it.toLatLng())
            }
            appendPolyline!!.add(node.toLatLng())
            googleMap.addPolyline(appendPolyline)
        }
    }

    var onAnimate = false
    override fun isOnAnimate(): Boolean {
        return onAnimate
    }

    override fun moveMarker(la: Double, lo: Double, done: (() -> Unit)?) {
        activity?.runOnUiThread() {
            var lalo = LatLng(la, lo)
            last_marker = lalo

            marker?.remove()
            val opt = MarkerOptions()
                    .position(lalo)
                    .title("I'm in here")
                    .icon(BitmapDescriptorFactory.fromResource(com.jimliuxyz.maprunner.R.drawable.pegman))
            marker = googleMap.addMarker(opt)

            val screen = googleMap.getProjection().getVisibleRegion().latLngBounds
            if (!screen.contains(lalo) && !onAnimate) {
                val update = CameraUpdateFactory.newLatLngZoom(lalo, ZOOM)
//          googleMap.moveCamera(update)

                onAnimate = true
                googleMap.animateCamera(update, 1000, object : GoogleMap.CancelableCallback {
                    override fun onFinish() {
                        done?.invoke()
                        onAnimate = false
                    }

                    override fun onCancel() {
                        done?.invoke()
                        onAnimate = false
                    }
                })
            } else {
                done?.invoke()
            }
        }
    }

    override fun addTag(la: Double, lo: Double) {
        activity?.runOnUiThread() {
            var lalo = LatLng(la, lo)

            val opt = MarkerOptions()
                    .position(lalo)
            googleMap.addMarker(opt)
        }
    }

    override fun clear() {
        last_dir = null
        last_marker = null
        activity?.runOnUiThread() {
            googleMap.clear()
        }
    }
}