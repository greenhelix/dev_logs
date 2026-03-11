class RedmineCurrentUser {
  const RedmineCurrentUser({
    required this.id,
    required this.login,
    required this.firstname,
    required this.lastname,
  });

  final int id;
  final String login;
  final String firstname;
  final String lastname;

  String get displayName {
    final fullName = [firstname, lastname]
        .where((item) => item.trim().isNotEmpty)
        .join(' ')
        .trim();
    if (fullName.isNotEmpty) {
      return fullName;
    }
    return login.isEmpty ? 'unknown' : login;
  }

  factory RedmineCurrentUser.fromMap(Map<String, dynamic> map) {
    return RedmineCurrentUser(
      id: (map['id'] as num?)?.toInt() ?? 0,
      login: map['login'] as String? ?? '',
      firstname: map['firstname'] as String? ?? '',
      lastname: map['lastname'] as String? ?? '',
    );
  }
}
