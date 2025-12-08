import 'package:flutter_test/flutter_test.dart';
import 'package:my_nfc_plugin/my_nfc_plugin.dart';
import 'package:my_nfc_plugin/my_nfc_plugin_platform_interface.dart';
import 'package:my_nfc_plugin/my_nfc_plugin_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockMyNfcPluginPlatform
    with MockPlatformInterfaceMixin
    implements MyNfcPluginPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final MyNfcPluginPlatform initialPlatform = MyNfcPluginPlatform.instance;

  test('$MethodChannelMyNfcPlugin is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelMyNfcPlugin>());
  });


}
