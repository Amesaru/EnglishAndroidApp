package com.example.test

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.test.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.auth0.jwt.JWT
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import com.google.gson.JsonObject

class MainActivity : AppCompatActivity() {

    data class Body(val email: String, val password: String)

    private lateinit var binding : ActivityMainBinding
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
        Log.d("LoginLog", Message)
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this@MainActivity)
        setContentView(binding.root)

        binding.emailField.doAfterTextChanged {
            if (!isEmailValid(binding.emailField.text.toString())) {
                binding.emailField.error = "Неккоректная почта"
            }
            if (binding.emailField.text.toString().trim().isEmpty()) {
                binding.emailField.error = null
            }
        }

//        binding.testButton.setOnClickListener {
//
//        }

        binding.signInButton.setOnClickListener {
            if (binding.emailField.text.toString().trim().isEmpty()) {
                showAlert("Ошибка", "Не введена почта")
                return@setOnClickListener
            }

            if (binding.passwordField.text.toString().trim().isEmpty()) {
                showAlert("Ошибка", "Не введен пароль")
                return@setOnClickListener
            }

            if (!isEmailValid(binding.emailField.text.toString())) {
                showAlert("Ошибка", "Некорректная почта")
                return@setOnClickListener
            }
            log("Start")
            val requestBodyClass = Body(binding.emailField.text.toString(), binding.passwordField.text.toString())

            val jsonData = Gson().toJson(requestBodyClass)

            val client = OkHttpClient()

            val requestBody = jsonData.toRequestBody()

            val request = Request.Builder()
                .url("http://10.0.2.2:8080/authApi/sign-in-request")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    log("GetResponse")
                    val responseCode = response.code
                    if (responseCode != 200) {
                        log("Response code$responseCode")
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
                            return
                        } else {
                            val data = jsonObject.getAsJsonObject("data")
                            val accessToken = data.get("accessToken").asString
                            val refreshToken = data.get("refreshToken").asString
                            GlobalVars.accessToken = accessToken
                            GlobalVars.refreshToken = refreshToken
                            log(GlobalVars.accessToken)
                            val decodedJWT = JWT.decode(accessToken)
                            val role = decodedJWT.getClaim("role").asInt()
                            if (role == 0) {
                                startActivity(Intent(this@MainActivity, studentProfile::class.java))
                            } else {
                                startActivity(Intent(this@MainActivity, TeacherProfile::class.java))
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

        binding.registrationButton.setOnClickListener {
            startActivity(Intent(this, Registration::class.java))
        }

        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(this, ChangePassword::class.java))
        }
    }
}