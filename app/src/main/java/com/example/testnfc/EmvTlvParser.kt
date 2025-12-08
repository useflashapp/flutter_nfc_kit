package com.example.testnfc

import android.util.Log

object EmvTlvParser {

    fun parseAndLog(dataHex: String) {
        val bytes = hexToBytes(dataHex)
        val tlvs = parseTlv(bytes)

        Log.d("EMV", "=======================")
        Log.d("EMV", " EMV TLV FULL PARSER ")
        Log.d("EMV", "=======================")

        for (tlv in tlvs) {
            val valueHex = bytesToHex(tlv.value)
            val desc = tagDescription(tlv.tag)

            Log.d("EMV", "TAG ${tlv.tag} | $desc")
            Log.d("EMV", "VALUE: $valueHex")

            decodeSpecialTags(tlv.tag, tlv.value)
            Log.d("EMV", "--------------------------------")
        }
    }

    // ---------- Decode Special Tags (PAN, Track2, Expiry...) -----------
    private fun decodeSpecialTags(tag: String, value: ByteArray) {
        when (tag.uppercase()) {

            "5A" -> {
                val pan = bytesToHex(value).trimEnd('F')
                Log.d("EMV", "PAN = $pan")
            }

            "5F24" -> {
                val y = "%02X".format(value[0])
                val m = "%02X".format(value[1])
                Log.d("EMV", "Expiry = 20$y/$m")
            }

            "57" -> {
                val t2 = bytesToHex(value)
                Log.d("EMV", "Track2 = $t2")

                val parts = t2.split("D")
                if (parts.size == 2) {
                    val pan = parts[0]
                    val expiry = parts[1].substring(0, 4)
                    val service = parts[1].substring(4, 7)
                    Log.d("EMV", "PAN = $pan")
                    Log.d("EMV", "Expiry = 20${expiry.substring(0,2)}/${expiry.substring(2,4)}")
                    Log.d("EMV", "Service Code = $service")
                }
            }

            "5F28" -> Log.d("EMV", "Country Code = ${bytesToHex(value)}")
            "5F2A" -> Log.d("EMV", "Currency Code = ${bytesToHex(value)}")
            "9F02" -> Log.d("EMV", "Amount Authorized = ${bytesToHex(value)}")
            "9F03" -> Log.d("EMV", "Amount Other = ${bytesToHex(value)}")
            "9A"   -> Log.d("EMV", "Transaction Date = ${bytesToHex(value)}")
            "9F21" -> Log.d("EMV", "Transaction Time = ${bytesToHex(value)}")
            "9F10" -> Log.d("EMV", "Issuer App Data = ${bytesToHex(value)}")
            "9F36" -> Log.d("EMV", "ATC = ${bytesToHex(value)}")
            "82"   -> Log.d("EMV", "AIP = ${bytesToHex(value)}")
            "94"   -> Log.d("EMV", "AFL = ${bytesToHex(value)}")

            "9F34" -> {
                Log.d("EMV", "CVM Results = ${bytesToHex(value)}")
            }
        }
    }

    // ----------- TLV Structure -------------
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

            val length = data[i].toInt() and 0xFF
            i++

            val value = data.copyOfRange(i, i + length)
            i += length

            list.add(TLV(tag, length, value))
        }

        return list
    }

    // ---------- Helpers ----------------
    private fun tagDescription(tag: String): String =
        when (tag.uppercase()) {
            "5A" -> "Application PAN"
            "57" -> "Track 2 Equivalent"
            "5F24" -> "Expiry Date"
            "5F28" -> "Issuer Country Code"
            "5F2A" -> "Transaction Currency"
            "9F02" -> "Amount Authorized"
            "9F03" -> "Amount Other"
            "9A" -> "Transaction Date"
            "9F10" -> "Issuer Application Data"
            "9F21" -> "Transaction Time"
            "82" -> "Application Interchange Profile (AIP)"
            "94" -> "Application File Locator (AFL)"
            "9F36" -> "ATC"
            "9F34" -> "CVM Results"
            else -> "Unknown / EMV Tag"
        }

    private fun hexToBytes(hex: String): ByteArray =
        hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02X".format(it) }
}