package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.auth0.jwt.JWT
import com.example.test.databinding.ActivityConfirmRegistrationBinding
import com.example.test.databinding.ActivityMainBinding
import com.example.test.databinding.ActivityRegistrationBinding
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class confirmRegistration : AppCompatActivity() {

    private lateinit var binding : ActivityConfirmRegistrationBinding
    private lateinit var builder : AlertDialog.Builder

    private fun showAlert(Title : String, Message : String) {
        builder.setTitle(Title)
            .setMessage(Message)
            .setCancelable(true)
            .show()
    }

    private fun log(Message : String) {
        Log.d("ConfirmRegistration", Message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmRegistrationBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        setContentView(binding.root)

        binding.confirmButton.setOnClickListener {
            log("Start")

            val client = OkHttpClient()

            log("http://10.0.2.2:8080/authApi/confirm/" + binding.codeField.text.toString())
            val request = Request.Builder()
                .url("http://10.0.2.2:8080/authApi/confirm/" + binding.codeField.text.toString())
                .get()
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    log("GetResponse")
                    val responseCode = response.code
                    if (responseCode == 500) {
                        log("Response code 500")
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
                                    .setMessage("Аккаунт успешно создан")
                                    .setCancelable(true)
                                    .setPositiveButton("Ок") {
                                            dialogInterface, it ->
                                        finish()
                                        startActivity(Intent(this@confirmRegistration, MainActivity::class.java))
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
    }
}