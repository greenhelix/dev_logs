import 'package:flutter/material.dart';

/// Flutter code sample for [DataTable].

void main() => runApp(const DataTableExampleApp());

class DataTableExampleApp extends StatelessWidget {
  const DataTableExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('DataTable Sample')),
        body: const DataTableExample(),
      ),
    );
  }
}

class DataTableExample extends StatefulWidget {
  const DataTableExample({super.key});

  @override
  State<DataTableExample> createState() => _DataTableExampleState();
}

class _DataTableExampleState extends State<DataTableExample> {
  static const int numItems = 20; // number of rows
  List<bool> selected = List<bool>.generate(numItems, (int index) => false);

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: DataTable(
        columns: const <DataColumn>[
          //column add , column width is ratio width using intrinsic column width by flex ratio.
          DataColumn(label: Text('Number'), columnWidth: IntrinsicColumnWidth(flex: 0.5)),
          DataColumn(label: Text('test'), columnWidth: IntrinsicColumnWidth(flex: 0.5))
        ],
        rows: List<DataRow>.generate(
          numItems,
              (int index) => DataRow(
            color: WidgetStateProperty.resolveWith<Color?>((Set<WidgetState> states) {
              // All rows will have the same selected color.
              if (states.contains(WidgetState.selected)) {
                return Theme.of(context).colorScheme.primary.withOpacity(0.08);
              }
              // Even rows will have a grey color.
              if (index.isEven) {
                return Colors.grey.withOpacity(0.3);
              }
              return null; // Use default value for other states and odd rows.
            }),
            cells: <DataCell>[DataCell(Text('Row $index')), DataCell(Text('test $index'))],  // column items add
            selected: selected[index],

            
            onSelectChanged: (bool? value) {
              setState(() {
                selected[index] = value!;
              });
            },
          ),
        ),
      ),
    );
  }
}