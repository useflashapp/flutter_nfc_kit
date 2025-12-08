import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'my_nfc_plugin_platform_interface.dart';

/// An implementation of [MyNfcPluginPlatform] that uses method channels.
class MethodChannelMyNfcPlugin extends MyNfcPluginPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('my_nfc_plugin');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
