package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import com.example.test.databinding.ActivityChangeTelegramBinding
import com.example.test.databinding.ActivityStudentChangeTeacherBinding
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

class StudentChangeTeacher : AppCompatActivity() {

    private lateinit var binding : ActivityStudentChangeTeacherBinding
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
        binding = ActivityStudentChangeTeacherBinding.inflate(layoutInflater)
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

        binding.getTeachersButton.setOnClickListener {

            val url = "http://localhost:8080/authApi/teachersAll"

            val urlObj = URL(url)

            val connection = urlObj.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer" + GlobalVars.accessToken)

            connection.doOutput = true
            connection.doInput = true

//                // Set the content length of the request body
//                connection.setRequestProperty("Content-Length", data.size.toString())

            val outputStream = DataOutputStream(connection.outputStream)
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
                var name_arr: MutableList<String> = ArrayList()
                var id_arr: MutableList<String>  = ArrayList()
                val jsonObject = jsonElement.asJsonObject
                val success = jsonObject.get("success").asBoolean
                if (success == false) {
                    showAlert("Ошибка", "Неверная почта или пароль")
                }

                val data = jsonObject.getAsJsonObject("data")

                for teacher in data {
                    if (teacher['fio'] != "") {
                        name_arr.add(teacher['fio'])
                        id_arr.add(teacher['id'])
                    }
                 }

            } else {
                println("Invalid JSON response.")
            }
        }
    }
}