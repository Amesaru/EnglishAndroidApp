package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import com.example.test.databinding.ActivityStudentProfileBinding
import com.example.test.databinding.ActivityTeacherProfileBinding
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

class TeacherProfile : AppCompatActivity() {

    private lateinit var binding : ActivityTeacherProfileBinding
    private lateinit var builder : AlertDialog.Builder

    private fun showAlert(Title: String, Message: String) {
        builder.setTitle(Title)
            .setMessage(Message)
            .setCancelable(true)
            .show()
    }

    private fun log(Message: String) {
        Log.d("TeacherProfileLog", Message)
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

    private fun updateData() {
        log("UpdateData")

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("http://10.0.2.2:8080/teacherProfileApi/profileData")
            .get()
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
                        return
                    } else {
                        runOnUiThread {
                            binding.FioField.text = "Учителов Илья Георгиевич"
                            binding.PhoneField.text = "Не введено"
                            binding.TgField.text = "Не введено"
                        }
//                        val data = jsonObject.getAsJsonObject("data")
//                        val profile = data.getAsJsonObject("profile")
//                        if (profile.get("fio").asString != "") {
//                            runOnUiThread {
//                                binding.FioField.text = profile.get("fio").asString
//                            }
//                        }
//                        if (profile.get("phone").asString != "") {
//                            runOnUiThread {
//                                binding.PhoneField.text = profile.get("phone").asString
//                            }
//                        }
//                        if (profile.get("telegram").asString != "") {
//                            runOnUiThread {
//                                binding.TgField.text = profile.get("telegram").asString
//                            }
//                        }
//                        if (data.get("telegramDeeplink").asString != "") {
//                            runOnUiThread {
//                                GlobalVars.TelegramDeepLink = data.get("telegramDeeplink").asString
//                                log(GlobalVars.TelegramDeepLink)
//                            }
//                        }
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
        binding = ActivityTeacherProfileBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        setContentView(binding.root)

        updateData()

        binding.openSidebar.setOnClickListener {
            binding.drawer.openDrawer(GravityCompat.START)
        }

        binding.sidebar.setNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.nav_profile -> {
                    updateTokens()
                    startActivity(Intent(this, TeacherProfile::class.java))
                }
                R.id.nav_lesson -> {
                    updateTokens()
                    startActivity(Intent(this, TeacherLessons::class.java))
                }
                R.id.nav_homework -> {
                    updateTokens()
                    startActivity(Intent(this, TeacherCreateHomework::class.java))
                }
                R.id.nav_grade -> {
                    updateTokens()
                    startActivity(Intent(this, TeacherGradeHomework::class.java))
                }
            }
            true
        }
        binding.exitAccountButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.changeFIOButton.setOnClickListener {
            updateTokens()
            startActivity(Intent(this, teacherChangeFio::class.java))
        }

        binding.changePhoneButton.setOnClickListener {
            updateTokens()
            startActivity(Intent(this, TeacherChangePhone::class.java))
        }

        binding.changeTGButton.setOnClickListener {
            updateTokens()
            startActivity(Intent(this, TeacherChangeTelegram::class.java))
        }

        binding.updateInfo.setOnClickListener {
            updateData()
        }
    }
}