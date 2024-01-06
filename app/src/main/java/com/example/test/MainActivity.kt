package com.example.test
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag

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
        textView?.text = "RFID Tag ID: $tagId"
    }

//    private fun extractTagId(intent: Intent): String {
//        // Replace this with the actual logic to extract the RFID tag ID
//        // from the intent based on the RFID library you are using
//        return "1234567890"
//    }

    //
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
}