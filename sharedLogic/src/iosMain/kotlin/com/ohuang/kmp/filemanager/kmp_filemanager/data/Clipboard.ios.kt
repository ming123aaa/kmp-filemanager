package com.ohuang.kmp.filemanager.kmp_filemanager.data

import platform.UIKit.UIPasteboard

actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
}