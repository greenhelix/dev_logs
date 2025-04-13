import 'package:flutter/material.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';

List<Test> getTestData() {
  return [
    Test(1, 'Module A', 'Case 001', 'Pass', 'Checks login flow', 'Tool v1.0', 'FW v2.1'),
    Test(2, 'Module A', 'Case 002', 'Fail', 'Validates input fields', 'Tool v1.1', 'FW v2.1'),
    Test(3, 'Module B', 'Case 003', 'Pass', 'Tests logout functionality', 'Tool v1.2', 'FW v2.2'),
    Test(4, 'Module B', 'Case 004', 'Pass', 'Handles session timeout', 'Tool v1.0', 'FW v2.3'),
    Test(5, 'Module C', 'Case 005', 'Fail', 'Form validation tests', 'Tool v1.1', 'FW v2.1'),
    Test(6, 'Module C', 'Case 006', 'Pass', 'Database write check', 'Tool v1.2', 'FW v2.4'),
    Test(7, 'Module A', 'Case 007', 'Pass', 'API response validation', 'Tool v1.0', 'FW v2.2'),
    Test(8, 'Module B', 'Case 008', 'Fail', 'Timeout error handling', 'Tool v1.1', 'FW v2.1'),
    Test(9, 'Module C', 'Case 009', 'Pass', 'Authentication test', 'Tool v1.2', 'FW v2.4'),
    Test(10, 'Module A', 'Case 010', 'Fail', 'UI responsiveness', 'Tool v1.0', 'FW v2.2'),
    Test(11, 'Module A', 'Case 011', 'Pass', 'User role permissions', 'Tool v1.0', 'FW v2.1'),
    Test(12, 'Module B', 'Case 012', 'Pass', 'Report generation test', 'Tool v1.2', 'FW v2.3'),
    Test(13, 'Module C', 'Case 013', 'Fail', 'PDF export check', 'Tool v1.1', 'FW v2.4'),
    Test(14, 'Module A', 'Case 014', 'Pass', 'File upload test', 'Tool v1.2', 'FW v2.3'),
    Test(15, 'Module B', 'Case 015', 'Pass', 'Push notification test', 'Tool v1.0', 'FW v2.2'),
    Test(16, 'Module C', 'Case 016', 'Fail', 'Language localization', 'Tool v1.1', 'FW v2.1'),
    Test(17, 'Module A', 'Case 017', 'Pass', 'Dark mode check', 'Tool v1.2', 'FW v2.3'),
    Test(18, 'Module B', 'Case 018', 'Fail', 'Accessibility test', 'Tool v1.0', 'FW v2.1'),
    Test(19, 'Module C', 'Case 019', 'Pass', 'Data encryption test', 'Tool v1.1', 'FW v2.2'),
    Test(20, 'Module A', 'Case 020', 'Pass', 'Error boundary validation', 'Tool v1.2', 'FW v2.3'),
    Test(21, 'Module A', 'Case 021', 'Pass', 'Background sync', 'Tool v1.1', 'FW v2.4'),
    Test(22, 'Module B', 'Case 022', 'Fail', 'Geo-location accuracy', 'Tool v1.0', 'FW v2.2'),
    Test(23, 'Module C', 'Case 023', 'Pass', 'Cache invalidation', 'Tool v1.2', 'FW v2.4'),
    Test(24, 'Module A', 'Case 024', 'Fail', 'Session restore', 'Tool v1.0', 'FW v2.1'),
    Test(25, 'Module B', 'Case 025', 'Pass', 'Widget rendering check', 'Tool v1.1', 'FW v2.3'),
    Test(26, 'Module C', 'Case 026', 'Pass', 'Email template test', 'Tool v1.2', 'FW v2.2'),
    Test(27, 'Module A', 'Case 027', 'Fail', 'Push retry logic', 'Tool v1.0', 'FW v2.3'),
    Test(28, 'Module B', 'Case 028', 'Pass', 'User deletion test', 'Tool v1.1', 'FW v2.1'),
    Test(29, 'Module C', 'Case 029', 'Fail', 'Network fallback check', 'Tool v1.2', 'FW v2.4'),
    Test(30, 'Module A', 'Case 030', 'Pass', 'OTP flow test', 'Tool v1.0', 'FW v2.2'),
  ];
}

class Test {

  Test(
      this.id,
      this.module,
      this.cases,
      this.status,
      this.description,
      this.toolVersion,
      this.fwVersion
      );

  final int id;
  final String module;
  final String cases;
  final String status;
  final String description;
  final String toolVersion;
  final String fwVersion;
}