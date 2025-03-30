import 'package:flutter/material.dart';

class AnimatedThemeScreen extends StatefulWidget {
  @override
  _AnimatedPositionedDirectionalScreenState createState() =>
      _AnimatedPositionedDirectionalScreenState();
}

class _AnimatedPositionedDirectionalScreenState
    extends State<AnimatedThemeScreen> with SingleTickerProviderStateMixin {
  bool selected = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('AnimatedTheme')),
      body: GestureDetector(
        onTap: () {
          setState(() {
            selected = !selected;
          });
        },
        child: AnimatedTheme(
          data: selected ? ThemeData.light() : ThemeData.dark(),
          duration: const Duration(milliseconds: 500),
          child: Center(
            child: Card(
              child: const Padding(
                padding: EdgeInsets.all(16),
                child: Text(
                  'theme',
                  style: TextStyle(fontSize: 24),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}