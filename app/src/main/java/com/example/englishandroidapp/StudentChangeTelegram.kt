package com.example.test

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.test.databinding.ActivityStudentChangeTelegramBinding
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import okhttp3.*

class StudentChangeTelegram : AppCompatActivity() {

    private lateinit var binding: ActivityStudentChangeTelegramBinding
    private lateinit var builder: AlertDialog.Builder

    private fun showAlert(Title: String, Message: String) {
        builder.setTitle(Title)
            .setMessage(Message)
            .setCancelable(true)
            .show()
    }

    private fun log(Message: String) {
        Log.d("StudentChangeTelegramLog", Message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentChangeTelegramBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        setContentView(binding.root)
        log(GlobalVars.TelegramDeepLink)

        binding.telegramButton.setOnClickListener {
            val deepLinkUri = Uri.parse(GlobalVars.TelegramDeepLink)
            val intent = Intent(Intent.ACTION_VIEW, deepLinkUri)
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                log("ActivityNotFound")
                e.printStackTrace()
            }
        }

        binding.confirmButton.setOnClickListener {
            log("Start")

            val client = OkHttpClient()

            val request = Request.Builder()
                .url("http://10.0.2.2:8080/profileApi/confirmTelegram")
                .post(RequestBody.create(null, ""))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GlobalVars.accessToken)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    log("GetResponse")
                    val responseCode = response.code
                    if (responseCode != 200) {
                        log("Response code $responseCode")
                        runOnUiThread {
                            showAlert("Ошибка", "Попробуйте снова")
                        }
                        return
                    }
                    val responseBodyString = response.body?.string()
                    if (responseBodyString != null) {
                        log(responseBodyString)
                        val jsonObject = Gson().fromJson(responseBodyString, JsonObject::class.java)
                        val success = jsonObject.get("success").asBoolean
                        if (success == false) {
                            log("no success")
                            runOnUiThread {
                                showAlert("Ошибка", jsonObject.get("reason").asString)
                            }
                        } else {
                            runOnUiThread {
                                builder.setTitle("Успех")
                                    .setMessage("Телеграм успешно подтвержден")
                                    .setCancelable(true)
                                    .setPositiveButton("Ок") { dialogInterface, it ->
                                        finish()
                                        startActivity(
                                            Intent(
                                                this@StudentChangeTelegram,
                                                studentProfile::class.java
                                            )
                                        )
                                    }
                                    .show()
                            }
                        }

                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    // Handle failure/error
                    when (e) {
                        is IOException -> {
                            log("IO")
                            e.printStackTrace()
                        }
                        is RuntimeException -> {
                            log("RunTime")
                            e.printStackTrace()
                        }
                        else -> {
                            log("Other")
                            e.printStackTrace()
                        }
                    }
                }
            })
        }

        binding.backToProfile.setOnClickListener {
            startActivity(Intent(this, studentProfile::class.java))
        }
    }
}