import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:libre_notes/main.dart';

void main() {
  testWidgets('Libre Notes shell renders key workspace areas', (WidgetTester tester) async {
    tester.view.physicalSize = const Size(1800, 1200);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(const LibreNotesApp());
    await tester.pumpAndSettle();

    expect(find.text('Libre Notes'), findsWidgets);
    expect(find.text('Project Roadmap'), findsWidgets);
    expect(find.text('Attachments'), findsOneWidget);
    expect(find.text('Autosaved'), findsOneWidget);
  });
}
