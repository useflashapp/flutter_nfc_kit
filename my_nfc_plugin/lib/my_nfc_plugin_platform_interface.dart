import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'my_nfc_plugin_method_channel.dart';

abstract class MyNfcPluginPlatform extends PlatformInterface {
  /// Constructs a MyNfcPluginPlatform.
  MyNfcPluginPlatform() : super(token: _token);

  static final Object _token = Object();

  static MyNfcPluginPlatform _instance = MethodChannelMyNfcPlugin();

  /// The default instance of [MyNfcPluginPlatform] to use.
  ///
  /// Defaults to [MethodChannelMyNfcPlugin].
  static MyNfcPluginPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [MyNfcPluginPlatform] when
  /// they register themselves.
  static set instance(MyNfcPluginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
