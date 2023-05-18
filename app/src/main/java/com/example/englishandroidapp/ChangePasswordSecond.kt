package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.auth0.jwt.JWT
import com.example.test.databinding.ActivityChangePasswordSecondBinding
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
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

class ChangePasswordSecond : AppCompatActivity() {
    data class Body(val newPassword: String)

    private lateinit var binding : ActivityChangePasswordSecondBinding
    private lateinit var builder : AlertDialog.Builder

    private fun showAlert(Title : String, Message : String) {
        builder.setTitle(Title)
            .setMessage(Message)
            .setCancelable(true)
            .show()
    }

    private fun log(Message : String) {
        Log.d("ChangePasswordSecondLog", Message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordSecondBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        setContentView(binding.root)

        binding.confirmButton.setOnClickListener {
            if (binding.codeField.text.toString().trim().isEmpty()) {
                showAlert("Ошибка", "Не введен код")
                return@setOnClickListener
            }

            if (binding.newPasswordField.text.toString().trim().isEmpty()) {
                showAlert("Ошибка", "Не введен новый пароль")
                return@setOnClickListener
            }

            if (binding.newPassword2Field.text.toString().trim().isEmpty()) {
                showAlert("Ошибка", "Не введено подтверждение пароля")
                return@setOnClickListener
            }

            if (!binding.newPasswordField.text.toString().equals(binding.newPassword2Field.text.toString())) {
                showAlert("Ошибка", "Пароли не совпадают")
                return@setOnClickListener
            }

            log("Start")
            val requestBodyClass = Body(binding.newPasswordField.text.toString())

            val jsonData = Gson().toJson(requestBodyClass)

            log(jsonData)

            val client = OkHttpClient()

            val requestBody = jsonData.toRequestBody()


            log("http://10.0.2.2:8080/authApi/restore-password/" + binding.codeField.text.toString())
            val request = Request.Builder()
                .url("http://10.0.2.2:8080/authApi/restore-password/" + binding.codeField.text.toString())
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()
            log(request.toString())
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
                                    .setMessage("Пароль успешно изменен")
                                    .setCancelable(true)
                                    .setPositiveButton("Ок") {
                                            dialogInterface, it ->
                                        finish()
                                        startActivity(Intent(this@ChangePasswordSecond, MainActivity::class.java))
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