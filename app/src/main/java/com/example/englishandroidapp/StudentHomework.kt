package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import com.example.test.databinding.ActivityStudentHomeworkBinding
import com.example.test.databinding.ActivityStudentProfileBinding
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

class StudentHomework : AppCompatActivity() {
    private lateinit var binding : ActivityStudentHomeworkBinding
    private lateinit var builder : AlertDialog.Builder

    data class Body(val topic: String, val homework: String)

    private fun showAlert(Title: String, Message: String) {
        builder.setTitle(Title)
            .setMessage(Message)
            .setCancelable(true)
            .show()
    }

    private fun log(Message: String) {
        Log.d("StudentHomeworkLog", Message)
    }

    private fun updateTokens() {
        log("UpdateTokens")
        val requestBodyClass = studentProfile.refreshBody(GlobalVars.refreshToken)

        val jsonData = Gson().toJson(requestBodyClass)

        val client = OkHttpClient()

        val requestBody = jsonData.toRequestBody()

        val request = Request.Builder()
            .url("http://10.0.2.2:8080/authApi/refresh")
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
                        val data = jsonObject.getAsJsonObject("data")
                        val accessToken = data.get("accessToken").asString
                        val refreshToken = data.get("refreshToken").asString
                        GlobalVars.accessToken = accessToken
                        GlobalVars.refreshToken = refreshToken
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentHomeworkBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        setContentView(binding.root)

        binding.openSidebar.setOnClickListener {
            binding.drawer.openDrawer(GravityCompat.START)
        }

        binding.sidebar.setNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.nav_profile -> {
                    updateTokens()
                    startActivity(Intent(this, studentProfile::class.java))
                }
                R.id.nav_lesson -> {
                    updateTokens()
                    startActivity(Intent(this, StudentLesson::class.java))
                }
                R.id.nav_homework -> {
                    updateTokens()
                    startActivity(Intent(this, StudentHomework::class.java))
                }
                R.id.nav_teacher -> {
                    updateTokens()
                    startActivity(Intent(this, StudentChangeTeacher::class.java))
                }
            }
            true
        }

        binding.getHomeworkButton.setOnClickListener {
            log("Start")

            if (binding.themeField.text.toString().trim().isEmpty()) {
                showAlert("Ошибка", "Не введен топик")
                return@setOnClickListener
            }

            val client = OkHttpClient()
            log("http://10.0.2.2:8080/homeworkStudentApi/rollbackHomework/"+binding.themeField.text.toString())
            val request = Request.Builder()
                .url("http://10.0.2.2:8080/homeworkStudentApi/rollbackHomework/"+binding.themeField.text.toString())
                .get()
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
                            val data = jsonObject.get("data").asString
                            runOnUiThread {
                                binding.homeworkField.visibility = View.VISIBLE
                                binding.answerField.visibility = View.VISIBLE
                                binding.homeworkField.setText(data)
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

        binding.confirmButton.setOnClickListener {
            log("Start")

            val requestBodyClass = Body(
                binding.themeField.text.toString(),
                binding.answerField.text.toString()
            )

            val jsonData = Gson().toJson(requestBodyClass)

            val client = OkHttpClient()

            val requestBody = jsonData.toRequestBody()

            log("http://10.0.2.2:8080/homeworkStudentApi/submitHomework")
            val request = Request.Builder()
                .url("http://10.0.2.2:8080/homeworkStudentApi/submitHomework")
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
                        runOnUiThread {
                            showAlert("Ошибка", "Попробуйте снова")
                        }
                    } else {
                        runOnUiThread {
                            showAlert("Успех", "Домашнее задание успешно отправлено на проверку")
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