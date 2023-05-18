package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import com.example.test.databinding.ActivityStudentChangeTeacherBinding
import com.example.test.databinding.ActivityStudentLessonBinding
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

class StudentLesson : AppCompatActivity() {
    private lateinit var binding : ActivityStudentLessonBinding
    private lateinit var builder : AlertDialog.Builder

    data class dayBody(val day: String)
    data class reserveBody(val timeSlotId: String, val date: String)

    private var cur_id: String = ""
    private var cur_date: String = ""

    private var time_arr = ArrayList<String>()
    private var id_arr = ArrayList<String>()

    private fun showAlert(Title: String, Message: String) {
        builder.setTitle(Title)
            .setMessage(Message)
            .setCancelable(true)
            .show()
    }

    private fun log(Message: String) {
        Log.d("StudentLessonLog", Message)
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
        binding = ActivityStudentLessonBinding.inflate(layoutInflater)
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

        binding.chooseTimeButton.setOnClickListener {
            time_arr.clear()
            id_arr.clear()
            if (binding.dateField.text.toString().trim().isEmpty()) {
                showAlert("Ошибка", "Нет даты")
                return@setOnClickListener
            }
            log("Start")

            cur_date = binding.dateField.text.toString()

            val requestBodyClass = dayBody(
                binding.dateField.text.toString()
            )

            val jsonData = Gson().toJson(requestBodyClass)

            val client = OkHttpClient()

            val requestBody = jsonData.toRequestBody()

            val request = Request.Builder()
                .url("http://10.0.2.2:8080/teacherScheduleApi/freeSlotsByDay")
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
                            val data = jsonObject.getAsJsonArray("data")
                            for (time in data) {
                                val timeObject = time.asJsonObject
                                time_arr.add(timeObject.get("startTime").asString +"-"+timeObject.get("endTime").asString)
                                id_arr.add(timeObject.get("id").asString)
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
            Thread.sleep(1000)
            binding.spinner.visibility = View.VISIBLE
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, time_arr)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinner.adapter = adapter
            binding.spinner.performClick()
            binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedItem = parent?.getItemAtPosition(position).toString()
                    val selectedIndex = position
                    cur_id = id_arr[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Handle case when nothing is selected
                }
            }
        }

        binding.confirmButton.setOnClickListener {
            log("Start")

            val requestBodyClass = reserveBody(cur_id, cur_date)

            val jsonData = Gson().toJson(requestBodyClass)

            val client = OkHttpClient()

            val requestBody = jsonData.toRequestBody()

            val request = Request.Builder()
                .url("http://10.0.2.2:8080/teacherScheduleApi/startClassConfirmation")
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
                            showAlert("Успех", "Занятие успешно забронировано")
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