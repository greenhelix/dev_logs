import 'package:flutter/material.dart';

class AnimatedCrossFadeScreen extends StatefulWidget {
  @override
  _AnimatedCrossFadeScreenState createState() =>
      _AnimatedCrossFadeScreenState();
}

class _AnimatedCrossFadeScreenState extends State<AnimatedCrossFadeScreen> {
  bool selected = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('AnimatedCrossFade')),
      body: GestureDetector(
          onTap: () {
            setState(() {
              selected = !selected;
            });
          },
          child: Center(
            child: AnimatedCrossFade(
              duration: const Duration(milliseconds: 500),
              firstChild: const FlutterLogo(
                  style: FlutterLogoStyle.horizontal, size: 100.0),
              secondChild:
              const FlutterLogo(style: FlutterLogoStyle.stacked, size: 100.0),
              crossFadeState:
              selected ? CrossFadeState.showFirst : CrossFadeState.showSecond,
            ),
          )),
    );
  }
}