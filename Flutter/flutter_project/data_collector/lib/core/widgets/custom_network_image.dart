import 'package:flutter/material.dart';

// A robust widget for displaying network images.
// Handles loading states, empty URLs, and rendering errors for unsupported formats.
class CustomNetworkImage extends StatelessWidget {
  final String imageUrl;
  final double? width;
  final double? height;
  final BoxFit fit;

  const CustomNetworkImage({
    Key? key,
    required this.imageUrl,
    this.width,
    this.height,
    this.fit = BoxFit.cover,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final cleanUrl = imageUrl.trim();

    // Show placeholder if the URL is empty
    if (cleanUrl.isEmpty) {
      return _buildPlaceholder(Icons.image_not_supported, 'No Image');
    }

    return Image.network(
      cleanUrl,
      width: width,
      height: height,
      fit: fit,
      // Show a CircularProgressIndicator while the image is downloading
      loadingBuilder: (BuildContext context, Widget child,
          ImageChunkEvent? loadingProgress) {
        if (loadingProgress == null) return child;
        return SizedBox(
          width: width,
          height: height,
          child: Center(
            child: CircularProgressIndicator(
              value: loadingProgress.expectedTotalBytes != null
                  ? loadingProgress.cumulativeBytesLoaded /
                      (loadingProgress.expectedTotalBytes ?? 1)
                  : null,
            ),
          ),
        );
      },
      // Catch and display a broken image icon if the image fails to load (e.g. CORS, webp/heic format issues)
      errorBuilder:
          (BuildContext context, Object error, StackTrace? stackTrace) {
        return _buildPlaceholder(Icons.broken_image, 'Failed Load');
      },
    );
  }

  // Helper method to build consistent error/empty placeholders
  Widget _buildPlaceholder(IconData icon, String text) {
    return Container(
      width: width,
      height: height,
      color: Colors.grey[200],
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: Colors.grey[500], size: 32),
          const SizedBox(height: 8),
          Text(
            text,
            style: TextStyle(color: Colors.grey[500], fontSize: 12),
          ),
        ],
      ),
    );
  }
}
