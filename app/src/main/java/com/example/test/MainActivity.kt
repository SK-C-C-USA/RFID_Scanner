package com.example.test
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textView)

        // Check if NFC is available on the device
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            textView?.text = "NFC is not available on this device"
        } else {
            // Enable NFC foreground dispatch to handle RFID tag scans
            enableNfcForegroundDispatch()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun enableNfcForegroundDispatch() {
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val intentFilters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )
        val techLists = arrayOf(arrayOf<String>())
        try {
            nfcAdapter?.enableForegroundDispatch(
                this, pendingIntent, intentFilters, techLists
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleNfcIntent(intent: Intent) {
        // Handle the RFID tag data here
        val tagId = extractTagId(intent)
        createRecord(tagId)

        // Add the following code to navigate back to the main screen after 5 seconds
        Thread {
            Thread.sleep(5000) // Sleep for 5 seconds

            runOnUiThread {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }.start()
    }


    private fun extractTagId(intent: Intent): String {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

        // Check if the tag is not null
        tag ?: return "Tag not found"

        // Get the ID bytes from the tag
        val idBytes = tag.id

        // Convert the ID bytes to a hexadecimal string
        val tagId = bytesToHexString(idBytes)

        return tagId
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        val stringBuilder = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val hex = String.format("%02X", byte)
            stringBuilder.append(hex)
        }
        return stringBuilder.toString()
    }



    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    private fun readUrlFromFile(): String? {
        try {
            val inputStream = assets.open("url.txt")
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val url = bufferedReader.readLine()
            bufferedReader.close()
            return url
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
    private fun createRecord(userCode: String) {
        Thread {
            try {
                val url_text = readUrlFromFile()
                if (url_text != null) {
                    // Step 1: Get user information based on the provided code
                    val user = getUserInfo(url_text, userCode)

                    runOnUiThread {
                        if (user != null) {
                            // User found, display the name
                            val userName = user.getString("name")
                            textView?.text = "User Name: $userName"
                        } else {
                            // User not found, display a message
                            textView?.text = "관리자 (dev@skccus.com)에게 등록을 요청하세요."
                        }
                    }

                    if (user != null) {
                        // Step 2: Send the tagging record to the server
                        sendTaggingRecord(url_text, user.getString("id"))
                    }
                } else {
                    // Handle the case where reading the URL failed
                    Log.e("RFID_Scanner", "Failed to read URL from file")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun getUserInfo(baseUrl: String, userCode: String): JSONObject? {
        try {
            val url = URL("$baseUrl/users/")
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")

                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { br ->
                    val response = StringBuilder()
                    var responseLine: String?
                    while (br.readLine().also { responseLine = it } != null) {
                        response.append(responseLine!!.trim())
                    }

                    val userList = JSONArray(response.toString())

                    // Find the user with the specified code
                    for (i in 0 until userList.length()) {
                        val user = userList.getJSONObject(i)
                        if (user.getString("code") == userCode) {
                            return user
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun sendTaggingRecord(baseUrl: String, userId: String) {
        try {
            val url = URL("$baseUrl/records/")
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; utf-8")
                doOutput = true

                // Create a JSON object for the tagging record
                val jsonInputString = "{\"user\": $userId}"
                outputStream.use { os ->
                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { br ->
                    val response = StringBuilder()
                    var responseLine: String?
                    while (br.readLine().also { responseLine = it } != null) {
                        response.append(responseLine!!.trim())
                    }

                    // Log the server response
                    Log.i("RFID_Scanner", response.toString())
                }

                // Handle UI updates if needed
                runOnUiThread {
                    // You can update the UI here if needed
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}