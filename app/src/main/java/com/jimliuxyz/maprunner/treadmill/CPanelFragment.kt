package com.jimliuxyz.maprunner.treadmill

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.jimliuxyz.maprunner.R
import com.jimliuxyz.maprunner.view.SpeedView


class CPanelFragment: Fragment(), Contract.ICPanel {

    private lateinit var vBt:ImageButton
    private lateinit var vSpeedView:SpeedView
    private lateinit var tvMileage:TextView

    private lateinit var imLineS:ImageView
    private lateinit var imLineV:ImageView
    private lateinit var imLineH:ImageView

    private lateinit var btnSpdDn:ImageButton
    private lateinit var btnSpdUp:ImageButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        var root = inflater?.inflate(R.layout.treadmill_cpanel, container, false)!!

        vBt = root.findViewById<ImageButton>(R.id.btnBt)
        vSpeedView = root.findViewById<SpeedView>(R.id.speedView)
        tvMileage = root.findViewById<TextView>(R.id.tvMileage)

        imLineS = root.findViewById<ImageView>(R.id.ivLineS)
        imLineV = root.findViewById<ImageView>(R.id.ivLineV)
        imLineH = root.findViewById<ImageView>(R.id.ivLineH)

        btnSpdDn = root.findViewById<ImageButton>(R.id.btnSpdDn)
        btnSpdUp = root.findViewById<ImageButton>(R.id.btnSpdUp)
        return root
    }

    private var last_state = Contract.BluetoothState.Off
    private var last_speed = 0.0
    private var last_mileage = 0.0
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("last_state", last_state.toInt())
        outState.putDouble("last_speed", last_speed)
        outState.putDouble("last_mileage", last_mileage)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null){
            last_state = Contract.BluetoothState.fromInt(savedInstanceState.getInt("last_state"))
            last_speed = savedInstanceState.getDouble("last_speed")
            last_mileage = savedInstanceState.getDouble("last_mileage")

            showBluetoothState(last_state)
            showSpeed(last_speed)
            showMileage(last_mileage)
        }
    }

    override fun showBluetoothState(state: Contract.BluetoothState) {

        last_state = state
        activity?.runOnUiThread() {
            when (state){
                Contract.BluetoothState.Waiting->{
                    vBt.setImageResource(R.drawable.cp_bt_blue)
                }
                Contract.BluetoothState.Linked->{
                    vBt.setImageResource(R.drawable.cp_bt_on)
                }
                else -> {
                    vBt.setImageResource(R.drawable.cp_bt)
                }
            }
            when (state){
                Contract.BluetoothState.Linked->{
                    vSpeedView.setImageResource(R.drawable.cp_spdpan_on)
                    imLineS.setImageResource(R.drawable.cp_lines_on)
                    imLineV.setImageResource(R.drawable.cp_linev_on)
                    imLineH.setImageResource(R.drawable.cp_lineh_on)
                }
                else -> {
                    vSpeedView.setImageResource(R.drawable.cp_spdpan)
                    imLineS.setImageResource(R.drawable.cp_lines)
                    imLineV.setImageResource(R.drawable.cp_linev)
                    imLineH.setImageResource(R.drawable.cp_lineh)
                }
            }
        }
    }

    override fun showSpeed(speed: Double) {

        last_speed = speed
        activity?.runOnUiThread(){
            vSpeedView.setValue(speed.toFloat())
        }
    }

    override fun animateSpeedBtn(up:Boolean) {
        var set = AnimatorSet()

        var btn = if(up) btnSpdUp else btnSpdDn

        set.play(ObjectAnimator.ofFloat(btn, View.SCALE_X, 1f, 1.1f, 1f))
                .with(ObjectAnimator.ofFloat(btn, View.SCALE_Y, 1f, 1.1f, 1f))

        set.interpolator = DecelerateInterpolator()
        set.duration = 150
        set.start()

//        vibrate()
    }

    fun vibrate() {
        val myVibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            myVibrator?.vibrate(VibrationEffect.createOneShot(150, 10))
        }
        else
            myVibrator.vibrate(150)
    }

    override fun showMileage(mileage: Double) {
        last_mileage = mileage
        activity?.runOnUiThread(){
            var str = String.format("%.2fkm", mileage)
            tvMileage.setText(str)
        }
    }
}