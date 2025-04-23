import 'package:flutter/material.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';

void main() {
  runApp(MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const SfDataGridDemo()));
}

class SfDataGridDemo extends StatefulWidget {
  const SfDataGridDemo({Key? key}) : super(key: key);

  @override
  _SfDataGridDemoState createState() => _SfDataGridDemoState();
}



class _SfDataGridDemoState extends State<SfDataGridDemo> {
  List<Employee> _employees = <Employee>[];
  late EmployeeDataSource _employeeDataSource;

  @override
  void initState() {
    super.initState();
    _employees = getEmployeeData();
    _employeeDataSource = EmployeeDataSource(_employees);
  }

  String dropdownValue = status.first;

  @override
  Widget build(BuildContext context) {

    return Scaffold(
      appBar: AppBar(
        title: const Text('Flutter SfDataGrid'),
      ),
      body: SfDataGrid(source: _employeeDataSource, columns: [
        GridColumn(
          columnName: 'id',
          label: Container(
            padding: const EdgeInsets.all(8.0),
            alignment: Alignment.center,
            child: const Text('ID'),
          ),
        ),
        GridColumn(
          columnName: 'name',
          label: Container(
            padding: const EdgeInsets.all(8.0),
            alignment: Alignment.center,
            child: const Text('Name'),
          ),
        ),
        GridColumn(
          columnName: 'designation',
          label: Container(
            padding: const EdgeInsets.all(8.0),
            alignment: Alignment.center,
            child: const Text('Designation'),
          ),
        ),
        GridColumn(
          columnName: 'button',
          label: Container(
            padding: const EdgeInsets.all(8.0),
            alignment: Alignment.center,
            child: const Text('Details '),
          ),
        ),
        GridColumn(
          columnName: 'status',
          label: Container(
            padding: const EdgeInsets.all(8.0),
            alignment: Alignment.center,
            child: const Text('Status ')
          ),
        ),
      ]),
    );
  }

  List<Employee> getEmployeeData() {
    return [
      Employee(10001, 'James', 'Project Lead ', 'pass'),
      Employee(10002, 'Kathryn', 'Manager', 'pass'),
      Employee(10003, 'Lara', 'Developer', 'pass'),
      Employee(10004, 'Michael', 'Designer', 'pass'),
      Employee(10005, 'Martin', 'Developer', 'pass'),
      Employee(10006, 'Newberry', 'Developer', 'pass'),
      Employee(10007, 'Balnc', 'Developer', 'pass'),
      Employee(10008, 'Perry', 'Developer', 'pass'),
      Employee(10009, 'Gable', 'Developer', 'pass'),
      Employee(10010, 'Grimes', 'Developer', 'pass'),
    ];
  }
}

const List<String> status = <String>['fail', 'pass', 'incomplete', 'wavier'];

class EmployeeDataSource extends DataGridSource {
  EmployeeDataSource(List<Employee> employees) {
    buildDataGridRow(employees);
  }

  void buildDataGridRow(List<Employee> employeeData) {
    dataGridRow = employeeData.map<DataGridRow>((employee) {
      return DataGridRow(cells: [
        DataGridCell<int>(columnName: 'id', value: employee.id),
        DataGridCell<String>(columnName: 'name', value: employee.name),
        DataGridCell<String>(columnName: 'designation', value: employee.designation),
        const DataGridCell<Widget>(columnName: 'button', value: null),
        DataGridCell<String>(columnName: 'status', value: employee.status),
      ]);
    }).toList();
  }

  List<DataGridRow> dataGridRow = <DataGridRow>[];

  @override
  List<DataGridRow> get rows => dataGridRow.isEmpty ? [] : dataGridRow;

  @override
  DataGridRowAdapter? buildRow(DataGridRow row) {
    return DataGridRowAdapter(
        cells: row.getCells().map<Widget>((dataGridCell) {
          return Container(
              alignment: Alignment.center,
              child:
                dataGridCell.columnName == 'button' ? LayoutBuilder(builder: (BuildContext context, BoxConstraints constraints) { return ElevatedButton(onPressed: () {
                        showDialog(
                          context: context,
                          builder: (context) => AlertDialog(
                              content: SizedBox( height: 100,
                                child: Column(
                                        mainAxisAlignment:
                                        MainAxisAlignment.spaceBetween,
                                        children: [
                                          Text(
                                              'Employee ID: ${row.getCells()[0].value.toString()}'),
                                          Text(
                                              'Employee Name: ${row.getCells()[1].value.toString()}'),
                                          Text(
                                              'Employee Designation: ${row.getCells()[2].value.toString()}'),
                                        ],)
                            )
                          )
                        );
                      },
                      child: const Text('Details')
                  );
                  })
                  : Text(dataGridCell.value.toString())
          );
        }).toList());
  }
}

class Employee {
  Employee(this.id, this.name, this.designation, this.status);
  final int id;
  final String name;
  final String designation;
  final String status;
}