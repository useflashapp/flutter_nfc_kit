package com.example.testnfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainCall : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

    }
    public fun initNFC(){
        val adapter = NfcAdapter.getDefaultAdapter(this)


        if (adapter == null) {
            Toast.makeText(this, "الجهاز لا يدعم NFC", Toast.LENGTH_LONG).show()
            return
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))

        val techs = arrayOf(arrayOf(IsoDep::class.java.name))

        adapter.enableForegroundDispatch(this, pendingIntent, filters, techs)
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val tag = intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            readCard(tag)
        }
    }

    fun readCard(tag: Tag) {
        val iso = IsoDep.get(tag)
        iso.connect()

        Log.d("NFC", "Connected to IsoDep")

        // SELECT PPSE
        val selectPpse = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00,
            0x0E,
            '2'.code.toByte(),'P'.code.toByte(),'A'.code.toByte(),'Y'.code.toByte(),
            '.'.code.toByte(),'S'.code.toByte(),'Y'.code.toByte(),'S'.code.toByte(),
            '.'.code.toByte(),'D'.code.toByte(),'D'.code.toByte(),'F'.code.toByte(),
            '0'.code.toByte(),'1'.code.toByte(),
            0x00
        )

        val ppseResponse = iso.transceive(selectPpse)
        Log.d("NFC", "PPSE Response = ${ppseResponse.toHex()}")
        val hex = ppseResponse.joinToString("") { "%02X".format(it) }
        EmvTlvParser.parseAndLog(hex)

        val aid = extractAid(ppseResponse)
        if (aid == null) {
            Log.e("NFC", "AID NOT FOUND")
            return
        }

        Log.d("NFC", "AID = ${aid.toHex()}")

        // SELECT AID
        val selectAid = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00,
            aid.size.toByte(), *aid, 0x00
        )

        val aidResponse = iso.transceive(selectAid)
        Log.d("NFC", "SELECT AID Response = ${aidResponse.toHex()}")

        // GPO Command
        val gpo = byteArrayOf(
            0x80.toByte(), 0xA8.toByte(), 0x00, 0x00,
            0x02, 0x83.toByte(), 0x00, 0x00
        )

        val gpoResponse = iso.transceive(gpo)
        Log.d("NFC", "GPO Response = ${gpoResponse.toHex()}")

        // استخراج SFI من GPO
        val sfi = extractSfi(gpoResponse)
        Log.d("NFC", "SFI = $sfi")

        if (sfi == -1) {
            Log.e("NFC", "Could not extract SFI")
            return
        }

        // READ RECORD
        val readRecord = byteArrayOf(
            0x00, 0xB2.toByte(), 0x01, (sfi shl 3 or 4).toByte(), 0x00
        )

        val recordResponse = iso.transceive(readRecord)
        Log.d("NFC", "Record Response = ${recordResponse.toHex()}")

        val pan = extractPan(recordResponse)
        val exp = extractExpiry(recordResponse)

        Log.d("NFC", "PAN = $pan")
        Log.d("NFC", "EXP = $exp")

        iso.close()
    }

    fun extractAid(resp: ByteArray): ByteArray? {
        val tag = 0x4F.toByte()
        for (i in resp.indices) {
            if (resp[i] == tag) {
                val len = resp[i + 1]
                return resp.copyOfRange(i + 2, i + 2 + len)
            }
        }
        return null
    }

    fun extractSfi(resp: ByteArray): Int {
        // يبحث عن AIP / AFL داخل GPO
        for (i in resp.indices) {
            if (resp[i] == 0x94.toByte()) {   // AFL
                return (resp[i + 2].toInt() and 0xF8) shr 3
            }
        }
        return -1
    }

    fun extractPan(record: ByteArray): String {
        val idx = record.indexOfSequence(byteArrayOf(0x5A.toByte()))
        if (idx == -1) return ""
        val len = record[idx + 1].toInt()
        return record.copyOfRange(idx + 2, idx + 2 + len).toHex().trimEnd('F')
    }

    fun extractExpiry(record: ByteArray): String {
        val idx = record.indexOfSequence(byteArrayOf(0x5F.toByte(), 0x24.toByte()))
        if (idx == -1) return ""
        val data = record.copyOfRange(idx + 2, idx + 5)
        return "20${data[0].toHex()}/${data[1].toHex()}"
    }

    fun ByteArray.indexOfSequence(seq: ByteArray): Int {
        outer@ for (i in indices) {
            if (i + seq.size > size) return -1
            for (j in seq.indices) {
                if (this[i + j] != seq[j]) continue@outer
            }
            return i
        }
        return -1
    }

    fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }
    fun Byte.toHex(): String = "%02X".format(this)

}