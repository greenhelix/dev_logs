enum ConsoleHealthStatus { idle, checking, ok, failed, needsAttention }

class ConsoleHealth {
  const ConsoleHealth({
    required this.status,
    required this.message,
    this.matchedPrompt,
  });

  final ConsoleHealthStatus status;
  final String message;
  final String? matchedPrompt;

  static const idle = ConsoleHealth(
    status: ConsoleHealthStatus.idle,
    message: '콘솔이 시작되지 않았습니다.',
  );
}
