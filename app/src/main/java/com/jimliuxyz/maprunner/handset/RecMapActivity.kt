package com.jimliuxyz.maprunner.handset

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.jimliuxyz.maprunner.R
import com.jimliuxyz.maprunner.handset.db.RecDatabase
import com.jimliuxyz.maprunner.handset.db.RunRec
import com.jimliuxyz.maprunner.treadmill.Contract
import com.jimliuxyz.maprunner.treadmill.RecMapFragment
import com.jimliuxyz.maprunner.utils.direction.DirNode
import com.jimliuxyz.maprunner.utils.direction.NodeType
import com.jimliuxyz.maprunner.utils.doNetwork
import java.util.*

class RecMapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        val EXTRA_KEY = "Rec"
    }

    private lateinit var mapFragment: RecMapFragment
    private lateinit var rec: RunRec

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec_map)

        rec = intent.getSerializableExtra(EXTRA_KEY) as RunRec

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as RecMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(p0: GoogleMap?) {
        doNetwork {

            //read databse record
            rec?.let {
                var rec = it

                var db = RecDatabase.getInstance(this)
                var dao = db.getDao()

                var dirs = dao.getRunDirs(rec.id)
                if (dirs != null && dirs.size > 0) {
                    var nodeList = ArrayList<DirNode>()
                    for (dir in dirs) {
                        nodeList.add(DirNode(NodeType.Point, dir.latitude, dir.longitude, 0.0))
                    }
                    mapFragment.showDirection(nodeList)
                    mapFragment.moveMarker(dirs[dirs.size - 1].latitude, dirs[dirs.size - 1].longitude)
                }
            }

            var client = BtClient.getClient()

            //read real time data from bluetooth
            client.setMsgListener(rec) { state, msg ->
                msg?.let {
                    var msg = it
                    mapFragment.moveMarker(msg.latitude, msg.longitude)

                    mapFragment.appendDirection(DirNode(NodeType.Point, msg.latitude, msg.longitude, 0.0))
                }
                if (state == Contract.BluetoothState.Off)
                    finish()
            }
        }
    }
}
