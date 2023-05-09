package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import com.example.test.databinding.ActivityStudentChangeTeacherBinding
import com.example.test.databinding.ActivityStudentLessonBinding
import com.example.test.databinding.ActivityStudentProfileBinding
import com.google.gson.Gson
import com.google.gson.JsonElement
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class StudentLesson : AppCompatActivity() {
    private lateinit var binding : ActivityStudentLessonBinding
    private lateinit var builder : AlertDialog.Builder

    private fun showAlert(Title: String, Message: String) {
        builder.setTitle(Title)
            .setMessage(Message)
            .setCancelable(true)
            .show()
    }

    private fun log(Message: String) {
        Log.d("LoginLog", Message)
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
                R.id.nav_homework -> startActivity(Intent(this@studentProfile, StudentHomework::class.java))
                R.id.nav_lesson -> startActivity(Intent(this@studentProfile, StudentLesson::class.java))
                R.id.nav_teacher -> startActivity(Intent(this@studentProfile, StudentChangeTeacher::class.java))
            }
        }

        binding.signInButton.setOnClickListener {

            val url = "http://localhost:8080/authApi/rollbackHomework/"

            val urlObj = URL(url)

            data class Body(val email: String, val password: String)

            val requestBody =
                Body(binding.emailField.text.toString(), binding.passwordField.text.toString())

            val jsonData = Gson().toJson(requestBody)

            val connection = urlObj.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")

            connection.doOutput = true
            connection.doInput = true

            val jsonDataConverted = jsonData.toByteArray(Charsets.UTF_8)

//                // Set the content length of the request body
//                connection.setRequestProperty("Content-Length", data.size.toString())

            val outputStream = DataOutputStream(connection.outputStream)
            outputStream.write(jsonDataConverted)
            outputStream.flush()
            outputStream.close()

            val responseCode = connection.responseCode

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }

            reader.close()
            connection.disconnect()

            if (response.isEmpty()) {
                log("NoData")
                return@setOnClickListener
            }

            if (responseCode == 500) {
                log("Response code 500")
            }

            val jsonResponse = response.toString()

            val gson = Gson()
            val jsonElement: JsonElement = gson.fromJson(jsonResponse, JsonElement::class.java)

            // Access the accessToken field if it exists
            if (jsonElement.isJsonObject) {
                val jsonObject = jsonElement.asJsonObject
                val success = jsonObject.get("success").asBoolean
                if (success == false) {
                    showAlert("Ошибка", "Неверная почта или пароль")
                }

                val data = jsonObject.getAsJsonObject("data")
                val accessToken = data.get("accessToken").asString
                val refreshToken = data.get("refreshToken").asString

                GlobalVars.accessToken = accessToken
                GlobalVars.refreshToken = refreshToken

                val jwt: Claims = Jwts.parserBuilder()
                    .build()
                    .parseClaimsJws(accessToken)
                    .body

                val role = jwt.get("role", Int::class.java)

                if (role == 0) {
                    startActivity(Intent(this, studentProfile::class.java))
                } else {
                    startActivity(Intent(this, TeacherProfile::class.java))
                }

            } else {
                println("Invalid JSON response.")
            }
        }
    }
}