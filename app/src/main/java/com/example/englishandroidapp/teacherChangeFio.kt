package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.test.databinding.ActivityTeacherChangeFioBinding
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class teacherChangeFio : AppCompatActivity() {
    data class Body(val newFio: String)

    private lateinit var binding : ActivityTeacherChangeFioBinding
    private lateinit var builder : AlertDialog.Builder

    private fun showAlert(Title : String, Message : String) {
        builder.setTitle(Title)
            .setMessage(Message)
            .setCancelable(true)
            .show()
    }

    private fun log(Message : String) {
        Log.d("TeacherChangeFioLog", Message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherChangeFioBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        setContentView(binding.root)

        binding.confirmButton.setOnClickListener {
            if (binding.FIOField.text.toString().trim().isEmpty()) {
                showAlert("Ошибка", "Не ввели ФИО")
                return@setOnClickListener
            }

            log("Start")
            val requestBodyClass = Body(binding.FIOField.text.toString())

            val jsonData = Gson().toJson(requestBodyClass)

            val client = OkHttpClient()

            val requestBody = jsonData.toRequestBody()

            val request = Request.Builder()
                .url("http://10.0.2.2:8080/teacherProfileApi/setFio")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GlobalVars.accessToken)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    log("GetResponse")
                    val responseCode = response.code
                    if (responseCode != 200) {
                        log("Response code $responseCode")
                        showAlert("Ошибка", "Попробуйте снова")
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
                            return
                        } else {
                            runOnUiThread {
                                builder.setTitle("Успех")
                                    .setMessage("ФИО успешно изменено")
                                    .setCancelable(true)
                                    .setPositiveButton("Ок") {
                                            dialogInterface, it ->
                                        finish()
                                        startActivity(Intent(this@teacherChangeFio, TeacherProfile::class.java))
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
            startActivity(Intent(this, TeacherProfile::class.java))
        }
    }
}