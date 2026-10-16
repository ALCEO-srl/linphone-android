/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.FileNotFoundException
import org.linphone.compatibility.Compatibility
import org.linphone.core.tools.Log

class ImageUtils {
    companion object {
        // dms: decode with BitmapFactory.Options.inSampleSize so that the bitmap isn't bigger than needed
        // (Play Console: "BitmapFactory without downsampling"). Default size: notification large icon.
        fun decodeSampledResource(context: Context, resId: Int, reqSize: Int = getNotificationLargeIconSize(context)): Bitmap? {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(context.resources, resId, options)
            options.inSampleSize = computeInSampleSize(options, reqSize)
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeResource(context.resources, resId, options)
        }

        fun decodeSampledFile(path: String, reqSize: Int): Bitmap? {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)
            options.inSampleSize = computeInSampleSize(options, reqSize)
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeFile(path, options)
        }

        private fun computeInSampleSize(options: BitmapFactory.Options, reqSize: Int): Int {
            var sampleSize = 1
            while (options.outWidth / (sampleSize * 2) >= reqSize && options.outHeight / (sampleSize * 2) >= reqSize) {
                sampleSize *= 2
            }
            return sampleSize
        }

        private fun getNotificationLargeIconSize(context: Context): Int {
            return context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        }

        fun getRoundBitmapFromUri(
            context: Context,
            fromPictureUri: Uri?
        ): Bitmap? {
            var bm: Bitmap? = null
            if (fromPictureUri != null) {
                bm = try {
                    // We make a copy to ensure Bitmap will be Software and not Hardware, required for shortcuts
                    Compatibility.getBitmapFromUri(context, fromPictureUri).copy(Bitmap.Config.ARGB_8888, true)
                } catch (fnfe: FileNotFoundException) {
                    return null
                } catch (e: Exception) {
                    Log.e("[Image Utils] Failed to get bitmap from URI [$fromPictureUri]: $e")
                    return null
                }
            }
            if (bm != null) {
                val roundBm = getRoundBitmap(bm)
                if (roundBm != null) {
                    bm.recycle()
                    return roundBm
                }
            }
            return bm
        }

        fun rotateImage(source: Bitmap, angle: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle)
            val rotatedBitmap = Bitmap.createBitmap(
                source, 0, 0, source.width, source.height, matrix, true
            )
            source.recycle()
            return rotatedBitmap
        }

        private fun getRoundBitmap(bitmap: Bitmap): Bitmap? {
            val output =
                Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val color = -0xbdbdbe
            val paint = Paint()
            val rect =
                Rect(0, 0, bitmap.width, bitmap.height)
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = color
            canvas.drawCircle(
                bitmap.width / 2.toFloat(),
                bitmap.height / 2.toFloat(),
                bitmap.width / 2.toFloat(),
                paint
            )
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }
    }
}
