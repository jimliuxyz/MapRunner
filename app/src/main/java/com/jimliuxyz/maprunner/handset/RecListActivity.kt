package com.jimliuxyz.maprunner.handset

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import com.jimliuxyz.maprunner.R
import com.jimliuxyz.maprunner.handset.db.RecDatabase
import com.jimliuxyz.maprunner.handset.db.RunRec
import com.jimliuxyz.maprunner.treadmill.Contract
import com.jimliuxyz.maprunner.utils.doNetwork
import kotlinx.android.synthetic.main.activity_rec_list.*

class RecListActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: RecListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec_list)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            var btClient = BtClient.getClient()

            if (btClient.getState() == Contract.BluetoothState.Linked){
                Toast.makeText(this, "中斷連線", Toast.LENGTH_SHORT).show()
                btClient.close()
            }
            else{
                var intent = Intent(this, DeviceListActivity::class.java)
                startActivity(intent)
            }
        }

        recycler = findViewById<RecyclerView>(R.id.lvRec)
        val layoutManager = LinearLayoutManager(this)
        recycler.setLayoutManager(layoutManager)
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))


        adapter = RecListAdapter()
        adapter.regItemClickListener(object : RecListAdapter.ItemClickListener {
            override fun onItemClick(rec: RunRec) {
                var intent = Intent(getApplicationContext(), RecMapActivity::class.java)
                intent.putExtra(RecMapActivity.EXTRA_KEY, rec)
                startActivity(intent)
            }
        })
        recycler.adapter = adapter

    }

    override fun onResume() {
        super.onResume()

        BtClient.getClient().setStateListener { state ->
            runOnUiThread {
                when (state){
                    Contract.BluetoothState.Off->{
                        findViewById<FloatingActionButton>(R.id.fab).setImageResource(R.drawable.ic_bluetooth_white_36dp)
                    }
                    else->{
                        findViewById<FloatingActionButton>(R.id.fab).setImageResource(R.drawable.ic_bluetooth_blue_a700_36dp)
                    }
                }
                adapter.notifyDataSetChanged()
            }
        }

        doNetwork {
            var db = RecDatabase.getInstance(this)
            var dao = db.getDao()

            var list = dao.getRunRecList()
            runOnUiThread{
                adapter.updateList(list)
            }
        }
    }

}
