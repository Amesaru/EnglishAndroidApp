package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.example.test.databinding.ActivityChangePasswordBinding
import com.example.test.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ChangePassword : AppCompatActivity() {

    data class Body(val email: String)

    private lateinit var binding : ActivityChangePasswordBinding
    private lateinit var builder : AlertDialog.Builder

    private fun isEmailValid(eMail: String?): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(eMail).matches()
    }

    private fun showAlert(Title : String, Message : String) {
        builder.setTitle(Title)
            .setMessage(Message)
            .setCancelable(true)
            .show()
    }

    private fun log(Message : String) {
        Log.d("ChangePasswordLog", Message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        setContentView(binding.root)

        binding.emailField.doAfterTextChanged {
            if (!isEmailValid(binding.emailField.text.toString())) {
                binding.emailField.error = "Неккоректная почта"
            }
            if (binding.emailField.text.toString().trim().isEmpty()) {
                binding.emailField.error = null
            }
        }

        binding.confirmButton.setOnClickListener {
            if (binding.emailField.text.toString().trim().isEmpty()) {
                showAlert("Ошибка", "Не введена почта")
                return@setOnClickListener
            }

            if (!isEmailValid(binding.emailField.text.toString())) {
                showAlert("Ошибка", "Некорректная почта")
                return@setOnClickListener
            }

            log("Start")

            val requestBodyClass = Body(binding.emailField.text.toString())

            val jsonData = Gson().toJson(requestBodyClass)

            val client = OkHttpClient()

            val requestBody = jsonData.toRequestBody()

            val request = Request.Builder()
                .url("http://10.0.2.2:8080/authApi/startChangePassword-mobile")
                .post(requestBody)
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
                            startActivity(Intent(this@ChangePassword, ChangePasswordSecond::class.java))
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

        binding.backToLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

    }
}