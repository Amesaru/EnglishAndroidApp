package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.example.test.databinding.ActivityMainBinding
import com.example.test.databinding.ActivityRegistrationBinding
import com.google.gson.Gson
import com.google.gson.JsonElement
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class Registration : AppCompatActivity() {

    private lateinit var binding : ActivityRegistrationBinding
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
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
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

            if (binding.passwordField.text.toString().trim().isEmpty()) {
                showAlert("Ошибка", "Не введен новый пароль")
                return@setOnClickListener
            }

            if (binding.passwordField2.text.toString().trim().isEmpty()) {
                showAlert("Ошибка", "Не введено подтверждение пароля")
                return@setOnClickListener
            }

            if (!binding.passwordField.text.toString().equals(binding.passwordField2.text.toString())) {
                showAlert("Ошибка", "Пароли не совпадают")
                return@setOnClickListener
            }

            val url = "http://localhost:8080/authApi/registration-request-mobile"

            val urlObj = URL(url)

            data class Body(val email: String, val password: String)

            val requestBody = Body(binding.emailField.text.toString(), binding.passwordField.text.toString())

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

        binding.registrationButton.setOnClickListener {
            startActivity(Intent(this, Registration::class.java))
        }

        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(this, ChangePassword::class.java))
        }
    }
}