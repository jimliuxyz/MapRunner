package com.jimliuxyz.maprunner.handset

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import com.jimliuxyz.maprunner.MyApplication
import com.jimliuxyz.maprunner.R
import com.jimliuxyz.maprunner.handset.db.RunRec
import com.jimliuxyz.maprunner.utils.doNetwork


class DeviceListActivity : AppCompatActivity() {

    /**
     * Member fields
     */
    private var mBtAdapter: BluetoothAdapter? = null

    /**
     * Newly discovered devices
     */
    private lateinit var mNewDevicesArrayAdapter: ArrayAdapter<String>

    private fun onConnecting(connecting: Boolean){
        runOnUiThread{
            var en = !connecting
            findViewById<View>(R.id.button_scan)?.isEnabled = en
            findViewById<View>(R.id.paired_devices)?.isEnabled = en
            findViewById<View>(R.id.new_devices)?.isEnabled = en
        }
    }

    private fun goMapActivity(rec: RunRec){
        var intent = Intent(getApplicationContext(), RecMapActivity::class.java)
        intent.putExtra(RecMapActivity.EXTRA_KEY, rec)
        startActivity(intent)
    }

    private fun toast(msg: String){
        runOnUiThread{
            Toast.makeText(MyApplication.instance, msg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private val mDeviceClickListener = AdapterView.OnItemClickListener { av, v, arg2, arg3 ->
        // Cancel discovery because it's costly and we're about to connect
        mBtAdapter?.cancelDiscovery()

        // Get the device MAC address, which is the last 17 chars in the View
        val info = (v as TextView).text.toString()
        val address = info.substring(info.length - 17)

        toast("連線中...")
        onConnecting(true)
        doNetwork {
            var client = BtClient.getClient()
            client.connect(address) { ready, rec ->

                if (ready){
                    finish()
                    goMapActivity(rec!!)
                }
                else{
                    toast("連線失敗")
                }
                onConnecting(false)
            }
        }

    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // If it's already paired, skip it, because it's been listed already
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.name + "\n" + device.address)
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                setProgressBarIndeterminateVisibility(false)
                setTitle(R.string.select_device)
                if (mNewDevicesArrayAdapter.count == 0) {
                    val noDevices = resources.getText(R.string.none_found).toString()
                    mNewDevicesArrayAdapter.add(noDevices)
                }
                toast("搜尋完畢")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        setContentView(R.layout.activity_device_list)

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED)

        // Initialize the button to perform device discovery
        val scanButton = findViewById(R.id.button_scan) as Button
        scanButton.setOnClickListener { v ->
            doDiscovery()
            v.visibility = View.GONE
        }

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        val pairedDevicesArrayAdapter = ArrayAdapter<String>(this, R.layout.device_name)
        mNewDevicesArrayAdapter = ArrayAdapter(this, R.layout.device_name)

        // Find and set up the ListView for paired devices
        val pairedListView = findViewById(R.id.paired_devices) as ListView
        pairedListView.adapter = pairedDevicesArrayAdapter
        pairedListView.onItemClickListener = mDeviceClickListener

        // Find and set up the ListView for newly discovered devices
        val newDevicesListView = findViewById(R.id.new_devices) as ListView
        newDevicesListView.adapter = mNewDevicesArrayAdapter
        newDevicesListView.onItemClickListener = mDeviceClickListener

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        this.registerReceiver(mReceiver, filter)

        // Register for broadcasts when discovery has finished
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        this.registerReceiver(mReceiver, filter)

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()

        // Get a set of currently paired devices
        //seeu 直接取得已配對裝置
        mBtAdapter?.bondedDevices?.let {pairedDevices->
            // If there are paired devices, add each one to the ArrayAdapter
            if (pairedDevices.size > 0) {
                findViewById<View>(R.id.title_paired_devices).setVisibility(View.VISIBLE)
                for (device in pairedDevices) {
                    pairedDevicesArrayAdapter.add(device.name + "\n" + device.address)
                }
            } else {
                val noDevices = resources.getText(R.string.none_paired).toString()
                pairedDevicesArrayAdapter.add(noDevices)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Make sure we're not doing discovery anymore
        mBtAdapter?.cancelDiscovery()

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver)
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun doDiscovery() {
        Log.d(TAG, "doDiscovery()")

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true)
        setTitle(R.string.scanning)

        // Turn on sub-title for new devices
        findViewById<View>(R.id.title_new_devices).setVisibility(View.VISIBLE)

        // If we're already discovering, stop it
        mBtAdapter?.cancelDiscovery()

        // Request discover from BluetoothAdapter
        mBtAdapter?.startDiscovery()
    }

    companion object {

        /**
         * Tag for Log
         */
        private val TAG = "DeviceListActivity"

        /**
         * Return Intent extra
         */
        var EXTRA_DEVICE_ADDRESS = "device_address"
    }

}
