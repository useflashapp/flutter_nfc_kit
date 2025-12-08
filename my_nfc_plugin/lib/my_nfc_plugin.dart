import 'dart:async';
import 'package:flutter/services.dart';

class MyNfcPlugin {



  static const MethodChannel _channel = MethodChannel('my_nfc_plugin');


  MyNfcPlugin._internal();

  // Singleton instance
  static final MyNfcPlugin _instance = MyNfcPlugin._internal();

  // Initialize NFC
  static Future<String> initNfc() async {
    try {
      final result = await _channel.invokeMethod<String>('initNfc');
      return result ?? "NFC initialization failed";
    } on PlatformException catch (e) {
      return "Error: ${e.message}";
    }
  }

  // Read card via NFC and return CardData
  static Future<CardData> readCard() async {
    try {
      final Map? result = await _channel.invokeMethod('readCard');
      if (result == null) return CardData();
      return CardData.fromMap(Map<String, dynamic>.from(result));
    } on PlatformException catch (e) {
      print("Error reading card: ${e.message}");
      return CardData();
    }
  }

  // Optional: parse raw EMV hex data

}

class CardData {
  final String? pan;
  final String? expiry;
  final String? aid;
  final String? cardholderName;
  final String? track2;

  CardData({
    this.pan,
    this.expiry,
    this.aid,
    this.cardholderName,
    this.track2,
  });

  factory CardData.fromMap(Map<String, dynamic> map) {
    return CardData(
      pan: map['pan'] as String?,
      expiry: map['expiry'] as String?,
      aid: map['aid'] as String?,
      cardholderName: map['cardholderName'] as String?,
      track2: map['track2'] as String?,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'pan': pan,
      'expiry': expiry,
      'aid': aid,
      'cardholderName': cardholderName,
      'track2': track2,
    };
  }

  @override
  String toString() {
    return 'CardData(pan: $pan, expiry: $expiry, aid: $aid, cardholderName: $cardholderName, track2: $track2)';
  }
}