package com.ivannuri.tns

import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var player: MediaPlayer? = null
    private var drive = 0.3f
    private var mix = 0.7f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Crea sesión de audio segura
        try {
            player = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_RINGTONE_URI)
            player?.isLooping = true
            player?.setVolume(0f, 0f) // silencioso
            player?.start()
            val session = player!!.audioSessionId

            eq = Equalizer(0, session).apply { enabled = true }
            bass = BassBoost(0, session).apply { enabled = true }
            Toast.makeText(this, "Motor audio OK - sesión $session", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Sin efectos: ${e.message}", Toast.LENGTH_LONG).show()
        }

        val driveBar = findViewById<SeekBar>(R.id.drive)
        val mixBar = findViewById<SeekBar>(R.id.mix)
        val masterBar = findViewById<SeekBar>(R.id.master)

        driveBar.setOnSeekBarChangeListener(simple {
            drive = it / 100f
            findViewById<TextView>(R.id.driveVal).text = "$it%"
            applySafe()
        })
        mixBar.setOnSeekBarChangeListener(simple {
            mix = it / 100f
            findViewById<TextView>(R.id.mixVal).text = "$it%"
        })
        masterBar.setOnSeekBarChangeListener(simple {
            val db = (it/100f*40f)-20f
            findViewById<TextView>(R.id.masterVal).text = String.format("%.1f dB", db)
        })

        findViewById<Button>(R.id.btnCalibrate).setOnClickListener {
            Toast.makeText(this, AiCalibrator().calibrate(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun applySafe() {
        try {
            eq?.let { e ->
                for (i in 0 until e.numberOfBands) {
                    val level = (drive * 1000 - 500).toInt().coerceIn(-1500,1500)
                    e.setBandLevel(i.toShort(), level.toShort())
                }
            }
            bass?.setStrength((drive * 1000).toInt().toShort())
        } catch (_: Exception) {}
    }

    private fun simple(a:(Int)->Unit) = object: SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p:Int, f:Boolean)=a(p)
        override fun onStartTrackingTouch(s:SeekBar?){}
        override fun onStopTrackingTouch(s:SeekBar?){}
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.stop(); player?.release()
        eq?.release(); bass?.release()
    }
}
