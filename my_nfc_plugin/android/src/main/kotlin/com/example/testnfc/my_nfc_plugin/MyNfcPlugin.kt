package com.flash.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.util.Log
import android.widget.Toast
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

class MyNfcPlugin: FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "my_nfc_plugin")
        channel.setMethodCallHandler(this)


    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() { activity = null }
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) { activity = binding.activity }
    override fun onDetachedFromActivity() { activity = null }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when(call.method) {
            "initNfc" -> {
                initNFC()
                result.success("NFC initialized")
            }
            else -> result.notImplemented()
        }
    }

    fun initNFC() {

        val adapter = NfcAdapter.getDefaultAdapter(activity)

        if(adapter == null) {
            Toast.makeText(activity, "الجهاز لا يدعم NFC", Toast.LENGTH_LONG).show()
            return
        }
        val pendingIntent = PendingIntent.getActivity(
            activity, 0,
            Intent(activity, activity!!.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        val techs = arrayOf(arrayOf(IsoDep::class.java.name))
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techs)
    }

    // ================= EMV TLV Parser =================
    object EmvTlvParser {

        data class CardData(
            var pan: String? = null,
            var expiry: String? = null,
            var aid: String? = null,
            var cardholderName: String? = null,
            var track2: String? = null
        )

        fun parseAndLog(dataHex: String): CardData {
            val bytes = hexToBytes(dataHex)
            val tlvs = parseTlv(bytes)
            val card = CardData()

            Log.d("EMV", "=======================")
            Log.d("EMV", " EMV TLV FULL PARSER ")
            Log.d("EMV", "=======================")

            for (tlv in tlvs) {
                val valueHex = bytesToHex(tlv.value)
                val desc = tagDescription(tlv.tag)

                Log.d("EMV", "TAG ${tlv.tag} | $desc")
                Log.d("EMV", "VALUE: $valueHex")

                when (tlv.tag.uppercase()) {
                    "5A" -> card.pan = valueHex
                    "5F24" -> card.expiry = valueHex // YYMMDD
                    "4F" -> card.aid = valueHex
                    "5F20" -> card.cardholderName = tlv.value.toString(Charsets.UTF_8).trim()
                    "57" -> {
                        val track = valueHex
                        card.track2 = track
                        val trackStr = track.replace("D", "=")
                        val parts = trackStr.split("=")
                        if (parts.size >= 2) {
                            card.pan = parts[0]
                            card.expiry = parts[1].substring(0, 4) // YYMM
                        }
                    }
                }

                decodeSpecialTags(tlv.tag, tlv.value)
                Log.d("EMV", "--------------------------------")
            }

            Log.d("EMV", "Extracted PAN: ${card.pan}")
            Log.d("EMV", "Expiry: ${card.expiry}")
            Log.d("EMV", "AID: ${card.aid}")
            Log.d("EMV", "Cardholder Name: ${card.cardholderName}")
            Log.d("EMV", "Track2: ${card.track2}")

            return card
        }

        private fun decodeSpecialTags(tag: String, value: ByteArray) {}

        data class TLV(val tag: String, val length: Int, val value: ByteArray)

        private fun parseTlv(data: ByteArray): List<TLV> {
            val list = mutableListOf<TLV>()
            var i = 0

            while (i < data.size) {
                var tag = "%02X".format(data[i])
                i++
                if ((tag.toInt(16) and 0x1F) == 0x1F) {
                    tag += "%02X".format(data[i])
                    i++
                }

                var length = data[i].toInt() and 0xFF
                i++
                if (length > 0x80) {
                    val count = length - 0x80
                    length = 0
                    repeat(count) {
                        length = (length shl 8) + (data[i].toInt() and 0xFF)
                        i++
                    }
                }

                val value = data.copyOfRange(i, i + length)
                i += length

                list.add(TLV(tag, length, value))
            }

            return list
        }

        private fun tagDescription(tag: String): String {
            return when(tag.uppercase()) {
                "5A" -> "Primary Account Number (PAN)"
                "5F24" -> "Expiry Date"
                "4F" -> "Application Identifier (AID)"
                "5F20" -> "Cardholder Name"
                "57" -> "Track 2 Equivalent"
                else -> "Unknown Tag"
            }
        }

        private fun hexToBytes(hex: String): ByteArray =
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        private fun bytesToHex(bytes: ByteArray): String =
            bytes.joinToString("") { "%02X".format(it) }
    }
}