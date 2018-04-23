package com.jimliuxyz.maprunner

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.jimliuxyz.maprunner.handset.RecListActivity
import com.jimliuxyz.maprunner.treadmill.TreadmillActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun asTreadmill(view: View) {
        finish()
        startActivity(Intent(this, TreadmillActivity::class.java))
    }

    fun asHandset(view: View) {
        finish()
        startActivity(Intent(this, RecListActivity::class.java))
    }
}
