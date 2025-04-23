import 'package:flutter/material.dart';
import 'package:flutter_ui_study/tables/test/getData.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';

void main() {
  runApp(MaterialApp(
      title: 'DataTable Demo',
      theme: ThemeData(
        primarySwatch: Colors.green,
      ),
      home: const DataGridCustom()));
}

class DataGridCustom extends StatefulWidget {
   const  DataGridCustom({Key? key}) : super(key: key);

   @override
  _DataGridCustomState createState() => _DataGridCustomState();
}

class _DataGridCustomState extends State<DataGridCustom> {

  List<Test> _tests = <Test>[];
  late TestDataSource _testDataSource;

  @override
  void initState() {
    super.initState();
    _tests = getTestData();
    _testDataSource = TestDataSource(_tests, context);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Flutter DataGrid Custom 1'),),
      body: SfDataGrid(source: _testDataSource, columns: [
        _buildGridColumn('ID', 'id'),
        _buildGridColumn('Module', 'module'),
        _buildGridColumn('Test', 'cases'),
        _buildGridColumn('Status', 'status'),
        _buildGridColumn('Description', 'description'),
        _buildGridColumn('ToolVersion', 'toolVersion'),
        _buildGridColumn('FwVersion', 'fwVersion'),
      ]),
    );
  }
}

GridColumn _buildGridColumn(String columnName, String labelText) {
  return GridColumn(
    columnName: columnName,
    label: Container(
      padding: const EdgeInsets.all(8.0),
      alignment: Alignment.center,
      child: Text(labelText),
    ),
  );
}

class TestDataSource extends DataGridSource {

  List<DataGridRow> dataGridRow = <DataGridRow>[];
  final BuildContext context;

  TestDataSource(List<Test> a, this.context) { buildDataGridRow(a);}

  @override
  List<DataGridRow> get rows => dataGridRow;

  @override
  DataGridRowAdapter? buildRow(DataGridRow row) {
    return DataGridRowAdapter(
        cells: row.getCells().map<Widget>((dataGridCell) {
          final columnName = dataGridCell.columnName;
          final cellValue = dataGridCell.value;

          if(columnName == 'status') {
            return Container(
              alignment: Alignment.center,
              child: DropdownButton<String>(
                value: cellValue.toString(),
                items: ['Pass', 'Fail', 'Pending'].map((status) =>
                    DropdownMenuItem(
                        value: status,
                        child: Text(status),
                    )).toList(),
                onChanged: (newValue) {
                  print('Changed status: $newValue');
                  // status = newValue;
                },
              ),);
          }
          else if(columnName == 'description') {
            return Container( alignment: Alignment.center,
                child: ElevatedButton(
                onPressed: () {
              showDialog(
                context: context,
                builder: (context) => AlertDialog(
                  content: SizedBox(
                    height: 100,
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text('ID: ${row.getCells()[0].value}'),
                        Text('test: ${row.getCells()[1].value}'),
                        Text('description: ${row.getCells()[2].value}'),
                      ],
                    ),
                  ),
                ),
              );
            },
            child: const Text('Details'),)
            );
          }
          else {
            return Container(alignment: Alignment.center, child: Text(cellValue.toString()));
          }

        }).toList(),
      );
  }

  void buildDataGridRow(List<Test> data) {
    dataGridRow = data.map<DataGridRow>((test) {
      return DataGridRow(cells: [
        DataGridCell(columnName: 'id',        value: test.id),
        DataGridCell(columnName: 'module',    value: test.module),
        DataGridCell(columnName: 'cases',      value: test.cases),
        DataGridCell(columnName: 'status',    value: test.status),
        DataGridCell(columnName: 'description',   value: test.description),
        DataGridCell(columnName: 'toolVersion',  value: test.toolVersion),
        DataGridCell(columnName: 'fwVersion',    value: test.fwVersion)
      ]);
    }).toList();
  }

}