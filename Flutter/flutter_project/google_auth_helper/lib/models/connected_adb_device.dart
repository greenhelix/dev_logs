class ConnectedAdbDevice {
  const ConnectedAdbDevice({
    required this.serial,
    required this.state,
    this.modelName = '',
    this.product = '',
    this.deviceName = '',
  });

  final String serial;
  final String state;
  final String modelName;
  final String product;
  final String deviceName;

  bool get isReady => state == 'device';

  String get summaryLabel {
    final parts = <String>[
      if (modelName.isNotEmpty) modelName,
      if (product.isNotEmpty) product,
      if (deviceName.isNotEmpty) deviceName,
    ];
    return parts.isEmpty ? serial : '$serial (${parts.join(' / ')})';
  }
}
