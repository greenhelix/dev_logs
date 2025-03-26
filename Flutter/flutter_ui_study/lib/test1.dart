import 'package:flutter/material.dart';

class AnimatedAlignScreen extends StatefulWidget
{
  @override
  _AnimatedAlignScreenState createState() => _AnimatedAlignScreenState();
}

class _AnimatedAlignScreenState extends State<AnimatedAlignScreen>
{
  var alignment = Alignment.bottomLeft;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: Text("AnimatedAlign"),),
        body: Container(
          padding: EdgeInsets.all(30.0),
          child: Column(
            children: <Widget>[
              Expanded(
                child: AnimatedAlign(
                  alignment: alignment,
                  duration: Duration(milliseconds: 100),
                  child: FlutterLogo(size: 150)
                )
              ),
              TextButton(
                child: Text("alignment change"),
                onPressed:() {
                  setState(() {
                    alignment = alignment == Alignment.bottomLeft
                        ? Alignment.topRight : Alignment.bottomLeft;
                  });
                },
              )
            ],
          ),
        ),
      )
    );
  }
}