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
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ChangePassword : AppCompatActivity() {
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

            val url = "http://localhost:8080/authApi/sign-in-request"

            val urlObj = URL(url)

            data class Body(val email: String)

            val requestBody = Body(binding.emailField.text.toString())

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

            if (responseCode == 500) {
                Log.d("LoginLog", "Response code 500")
            }

            val jsonResponse = response.toString()

            val gson = Gson()
            val jsonElement: JsonElement = gson.fromJson(jsonResponse, JsonElement::class.java)

            // Access the accessToken field if it exists
            if (jsonElement.isJsonObject) {
                val jsonObject = jsonElement.asJsonObject
                val success = jsonObject.get("success").asBoolean
                if (success == false) {
                    showAlert("Ошибка", "неподходящая почта")
                }

                startActivity(Intent(this, ChangePasswordSecond::class.java))
            } else {
                println("Invalid JSON response.")
            }
        }
    }
}