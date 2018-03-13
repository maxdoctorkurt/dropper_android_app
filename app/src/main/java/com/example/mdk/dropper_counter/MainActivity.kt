package com.example.mdk.dropper_counter

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Patterns
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private var savedStationIP: String = ""
    private var savedDripsPerSecond: Int = 0
    private val IP_ADDRESS = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
            + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
            + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
            + "|[1-9][0-9]|[0-9]))")

    private val client = OkHttpClient();

    private fun connectToAP() {

        setProgress(true);

        client.newCall(Request.Builder()

                .addHeader("Connection", "close")
                .url("http://192.168.4.1:80/")
                .post(RequestBody.create(MediaType
                        .parse("application/json"), "{ssid: \"${ssid.text}\", pass: \"${pass.text}\"}"))
                .build())
                .enqueue(object : Callback {
                    override fun onFailure(call: Call?, e: IOException?) {
                        e?.printStackTrace();

                        runOnUiThread {
                            setProgress(false);
                            showMessage("Не удалось совершить запрос. Проверьте подключение к точке доступа DripCounter (пароль - 00000000)")
                        }

                    }

                    override fun onResponse(call: Call?, response: Response?) {

                        if (response != null) {
                            if (!response.isSuccessful) {
                                throw IOException("Unexpected code $response")
                            }

                            val reader = JSONObject(response.body()?.string())
                            val speed = reader.getString("speed")
                            val stationIP = reader.getString("station_ip")
                            savedDripsPerSecond = speed.toInt()
                            savedStationIP = stationIP

                            runOnUiThread {

                                if (!isValidIp(savedStationIP))
                                    showMessage("Не удалось подключиться к требуемой точке - проверьте ввод и попробуйте еще раз")
                                else {
                                    etSensorIP.setText(savedStationIP.toString())
                                    etSensorValue.setText(savedDripsPerSecond.toString())
                                    showMessage("Начата настройка подключения... Повторите запрос черен несколько секунд для получения актуального IP адреса датчика")
                                }

                                setProgress(false);
                            }
                        }
                    }
                })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connect.setOnClickListener({

            if (ssid.text.isEmpty()) {
                showMessage("Пустое имя точки")
                ssid.requestFocus()
                return@setOnClickListener
            } else if (pass.text.length < 8) {
                showMessage("Пароль должен иметь длину не менее 8 символов")
                pass.requestFocus()
                return@setOnClickListener
            }
            connectToAP()
        })
    }

    fun setProgress(progress: Boolean) {
        progress_bar.visibility = if (progress) View.VISIBLE else View.INVISIBLE
    }


    fun isValidIp(ip: String?): Boolean {
        return IP_ADDRESS.matcher(ip).matches();
    }

    fun showMessage(msg: String) {
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
    }

}
