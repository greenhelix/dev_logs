import 'package:flutter/material.dart';

class AnimatedContainerScreen extends StatefulWidget {
  @override
  _AnimatedContainerScreenState createState() => _AnimatedContainerScreenState();
}

class _AnimatedContainerScreenState extends State<AnimatedContainerScreen> {
  bool selected = false;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: Text('AnimatedContainer')),
        body: GestureDetector(
          onTap: () {
            setState(() {
              selected = !selected;
            });
          },
          child: Center(
            child: AnimatedContainer(
              width: selected ? 300.0 : 100.0,
              height: selected ? 100.0 : 300.0,
              alignment:
              selected ? Alignment.center : AlignmentDirectional.topCenter,
              duration: Duration(milliseconds: 500),
              decoration: BoxDecoration(
                border: selected
                    ? Border.all(color: Colors.black, width: 3)
                    : Border.all(color: Colors.red, width: 3),
                gradient: new LinearGradient(
                  begin: FractionalOffset.topCenter,
                  end: FractionalOffset.bottomCenter,
                  colors: selected
                      ? [Colors.lightGreen, Colors.redAccent]
                      : [Colors.orange, Colors.deepOrangeAccent],
                  stops: [0.0, 1.0],
                ),
                color: selected ? Colors.red : Colors.blue,
              ),
              curve: Curves.fastOutSlowIn,
              child: FlutterLogo(size: 75),
            ),
          ),
        ),
      ),
    );
  }
}