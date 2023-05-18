package com.example.test

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import com.auth0.jwt.JWT
import com.example.test.databinding.ActivityStudentChangeTeacherBinding
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException


//class CustomArrayAdapter(context: Context, items: ArrayList<String>) :
//    ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//        val view = super.getDropDownView(position, convertView, parent)
//        val textView = view.findViewById<TextView>(android.R.id.text1)
//        textView.visibility = View.VISIBLE
//        textView.setTextColor(Color.BLACK) // Adjust text color if needed
//        return view
//    }
//}

class StudentChangeTeacher : AppCompatActivity() {

    private lateinit var binding : ActivityStudentChangeTeacherBinding
    private lateinit var builder : AlertDialog.Builder
    private var cur_teacher_id: String = ""

    private var name_arr = ArrayList<String>()
    private var id_arr = ArrayList<String>()


    private fun showAlert(Title: String, Message: String) {
        builder.setTitle(Title)
            .setMessage(Message)
            .setCancelable(true)
            .show()
    }

    private fun log(Message: String) {
        Log.d("StudentChangeTeacherLog", Message)
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

    private fun getTeachers() {
        name_arr.clear()
        id_arr.clear()
        log("Start")
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("http://10.0.2.2:8080/profileApi/teachersAll")
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
                        val data = jsonObject.getAsJsonArray("data")
                        for (teacher in data) {
                            val teacherObject = teacher.asJsonObject
                            name_arr.add(teacherObject.get("fio").asString)
                            id_arr.add(teacherObject.get("id").asString)
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

        binding.getTeachersButton.setOnClickListener {
            binding.singleTeacher.visibility = View.INVISIBLE
            binding.textview17.visibility = View.INVISIBLE
            binding.spinner.visibility = View.INVISIBLE
            getTeachers()
            Thread.sleep(1000)
            binding.spinner.visibility = View.VISIBLE
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, name_arr)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinner.adapter = adapter
            binding.spinner.performClick()
            binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedItem = parent?.getItemAtPosition(position).toString()
                    val selectedIndex = position
                    cur_teacher_id = id_arr[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Handle case when nothing is selected
                }
            }
//            if (name_arr.size == 1) {
//                binding.singleTeacher.text = name_arr[0]
//                binding.singleTeacher.visibility = View.VISIBLE
//                binding.textview17.visibility = View.VISIBLE
//                cur_teacher_id = id_arr[0]
//            } else {
//                binding.spinner.visibility = View.VISIBLE
//                val adapter = CustomArrayAdapter(this@StudentChangeTeacher, name_arr)
//                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//                binding.spinner.adapter = adapter
//                binding.spinner.performClick()
//                binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                        val selectedItem = parent?.getItemAtPosition(position).toString()
//                        val selectedIndex = position
//                        cur_teacher_id = id_arr[position]
//                    }
//                    override fun onNothingSelected(parent: AdapterView<*>?) {
//                        // Handle case when nothing is selected
//                    }
//                }
//            }
        }

        binding.confirmButton.setOnClickListener {
            if (cur_teacher_id == "") {
                showAlert("Ошибка", "Выберите учителя")
                return@setOnClickListener
            }

            log("Start")
            val decodedJWT = JWT.decode(GlobalVars.accessToken)
            val id = decodedJWT.getClaim("jti").asString()

            val client = OkHttpClient()

            log("http://10.0.2.2:8080/managerApi/attachStudentToTeacher/$id/$cur_teacher_id")
            val request = Request.Builder()
                .url("http://10.0.2.2:8080/managerApi/attachStudentToTeacher/$id/$cur_teacher_id")
                .get()
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
                        } else {
                            runOnUiThread {
                                showAlert("Успех", "Учитель изменен")
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
    }
}