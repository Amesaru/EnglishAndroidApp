package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.example.test.databinding.ActivityChangeFioBinding
import com.example.test.databinding.ActivityChangePasswordBinding
import com.google.gson.Gson
import com.google.gson.JsonElement
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ChangeFio : AppCompatActivity() {
    private lateinit var binding : ActivityChangeFioBinding
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
        binding = ActivityChangeFioBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        setContentView(binding.root)

        binding.confirmButton.setOnClickListener {
            val url = "http://localhost:8080/authApi/setFio"

            val urlObj = URL(url)

            data class Body(val FIO: String)

            val requestBody = Body(binding.FIOField.text.toString())

            val jsonData = Gson().toJson(requestBody)

            val connection = urlObj.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer" + GlobalVars.accessToken)

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
                Log.d("ChangeFIOLog", "NoData)
                return@setOnClickListener
            }

            if (responseCode == 500) {
                Log.d("ChangeFIOLog", "Response code 500")
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

                startActivity(Intent(this, studentProfile::class.java))
            } else {
                println("Invalid JSON response.")
            }
        }
    }
}