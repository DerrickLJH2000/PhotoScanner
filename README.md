# Photo Scanner App

This app allows users to scan their photos and restore back its originality. It allows manual and automatic cropping of single photo, and automatic for multiple photos. 

# Getting Started

Upon running the application, make sure to enable all permissions needed for the application. Then, you can select an image from your Gallery or take a picture with the camera. 
Automatic Edge Detection help will crop the image for user convenience, however, users can still adjust the crop points themselves. A zoom feature is enabled for devices running API Level 28 and above.

## Features

* Load Image from Gallery
* Camera Tools e.g. (Flash, Grid lines)
* Automatic Edge Detection and Manual Cropping
* Automatic Edge Detection for Multiple Objects
* Google Filters API
* Zooming of image with Chris Banes' PhotoView dependency
```
implementation 'com.github.chrisbanes:PhotoView:2.3.0'
```
* Saving Image to External File Directory and preview in Index Page with RecyclerView

## Built With

OpenCV Library 3.4.3 :  
https://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.4.3/opencv-3.4.3-android-sdk.zip/download

[product-screenshot1]: static/9.jpg
[product-screenshot2]: static/7.jpg
[product-screenshot3]: static/5.jpg
[product-screenshot3]: static/4.jpg