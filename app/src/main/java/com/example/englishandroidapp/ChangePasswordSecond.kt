package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.test.databinding.ActivityChangePasswordSecondBinding
import com.google.gson.Gson
import com.google.gson.JsonElement
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ChangePasswordSecond : AppCompatActivity() {

    private lateinit var binding : ActivityChangePasswordSecondBinding
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
        binding = ActivityChangePasswordSecondBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        setContentView(binding.root)

        binding.changePasswordButton.setOnClickListener {
            if (binding.keyField.text.toString().trim().isEmpty()) {
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

            val url = "http://localhost:8080/authApi/restore-password/"

            val urlObj = URL(url)

            data class Body(val email: String)

            val requestBody = Body(binding.newPasswordField.text.toString())

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
                    showAlert("Ошибка", "Ошибка во время изменения пароля")
                }

                builder.setTitle("Успех")
                    .setMessage("Пароль успешно изменен")
                    .setCancelable(true)
                    .setPositiveButton("Ок") {
                            dialogInterface, it ->
                        finish()
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    .show()
            } else {
                println("Invalid JSON response.")
            }
        }
    }
}