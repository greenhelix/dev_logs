import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

// [Common Widget] Responsive List Tile
// Mobile: Swipe to Edit/Delete
// Web/Desktop: Trailing Buttons for Edit/Delete
class ResponsiveListTile extends StatelessWidget {
  final Widget child; // The content (usually a ListTile)
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final bool enableEdit;

  const ResponsiveListTile({
    Key? key,
    required this.child,
    required this.onEdit,
    required this.onDelete,
    this.enableEdit = true,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // Check screen width to determine layout (Mobile < 600px < Tablet/Desktop)
    final isWideScreen = MediaQuery.of(context).size.width > 600;

    if (isWideScreen) {
      // [Web/Desktop Mode] Show buttons on the right
      return Row(
        children: [
          Expanded(child: child),
          if (enableEdit)
            IconButton(
              icon: const Icon(Icons.edit, color: Colors.blue),
              onPressed: onEdit,
              tooltip: '수정',
            ),
          IconButton(
            icon: const Icon(Icons.delete, color: Colors.red),
            onPressed: onDelete,
            tooltip: '삭제',
          ),
          const SizedBox(width: 8),
        ],
      );
    } else {
      // [Mobile Mode] Use Slidable for swipe actions
      return Slidable(
        // Key is required for proper animation
        key: UniqueKey(),
        startActionPane: enableEdit
            ? ActionPane(
                motion: const ScrollMotion(),
                children: [
                  SlidableAction(
                    onPressed: (_) => onEdit(),
                    backgroundColor: Colors.blue,
                    foregroundColor: Colors.white,
                    icon: Icons.edit,
                    label: '수정',
                  ),
                ],
              )
            : null,
        endActionPane: ActionPane(
          motion: const ScrollMotion(),
          children: [
            SlidableAction(
              onPressed: (_) => onDelete(),
              backgroundColor: Colors.red,
              foregroundColor: Colors.white,
              icon: Icons.delete,
              label: '삭제',
            ),
          ],
        ),
        child: child,
      );
    }
  }
}
