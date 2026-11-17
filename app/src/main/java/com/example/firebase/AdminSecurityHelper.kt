package com.example.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min
import kotlin.math.pow

object AdminSecurityHelper {

    private val secureRandom = SecureRandom()
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$password:zelu_admin_vault_v1".toByteArray(Charsets.UTF_8)
        val hashed = digest.digest(input)
        return hashed.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        if (password.isBlank() || expectedHash.isBlank()) return false
        val actual = hashPassword(password, salt)
        return constantTimeEquals(actual, expectedHash)
    }

    fun generateResetToken(): String {
        val bytes = ByteArray(18)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest("zelu_reset:$token".toByteArray(Charsets.UTF_8))
        return hashed.joinToString("") { "%02x".format(it) }
    }

    fun verifyResetToken(token: String, expectedHash: String, expiresAtMillis: Long): Boolean {
        if (token.isBlank() || expectedHash.isBlank()) return false
        if (System.currentTimeMillis() > expiresAtMillis) return false
        return constantTimeEquals(hashToken(token.trim()), expectedHash)
    }

    fun generateBase32Secret(length: Int = 16): String {
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(BASE32_ALPHABET[secureRandom.nextInt(BASE32_ALPHABET.length)])
        }
        return sb.toString()
    }

    fun buildOtpAuthUri(email: String, secret: String): String {
        val safeEmail = Uri.encode(email.ifBlank { "admin@zelumath.ai" })
        return "otpauth://totp/ZELU%20Math%20AI:$safeEmail?secret=$secret&issuer=ZELU%20Math%20AI&digits=6&period=30"
    }

    fun verifyTotpCode(secret: String, userCode: String, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val cleaned = userCode.trim().filter { it.isDigit() }
        if (cleaned.length != 6 || secret.isBlank()) return false
        val currentStep = currentTimeMillis / 30_000L
        for (offset in -1L..1L) {
            val expected = generateTotpCode(secret, currentStep + offset)
            if (constantTimeEquals(cleaned, expected)) return true
        }
        return false
    }

    fun generateTotpCode(base32Secret: String, timeStep: Long): String {
        val keyBytes = decodeBase32(base32Secret)
        if (keyBytes.isEmpty()) return "000000"
        val data = ByteBuffer.allocate(8).putLong(timeStep).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % 10.0.pow(6).toInt()
        return otp.toString().padStart(6, '0')
    }

    private fun decodeBase32(input: String): ByteArray {
        val normalized = input.uppercase().filter { it in BASE32_ALPHABET }
        if (normalized.isEmpty()) return ByteArray(0)
        val out = ByteArrayOutputStream()
        var buffer = 0
        var bitsLeft = 0
        for (ch in normalized) {
            val value = BASE32_ALPHABET.indexOf(ch)
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out.write((buffer shr (bitsLeft - 8)) and 0xFF)
                bitsLeft -= 8
            }
        }
        return out.toByteArray()
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }

    suspend fun compressImageUriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int,
        jpegQuality: Int = 78
    ): String? = withContext(Dispatchers.IO) {
        try {
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext null

            val scale = min(
                1.0f,
                maxDimension.toFloat() / maxOf(rawBitmap.width, rawBitmap.height).coerceAtLeast(1)
            )
            val targetW = (rawBitmap.width * scale).toInt().coerceAtLeast(1)
            val targetH = (rawBitmap.height * scale).toInt().coerceAtLeast(1)
            val resized = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
            } else {
                rawBitmap
            }

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(40, 92), baos)
            val bytes = baos.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        if (base64.isBlank()) return null
        return try {
            val clean = base64.substringAfter("base64,", base64)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun parseHexColorOrNull(hex: String): Color? {
        val clean = hex.trim().removePrefix("#")
        if (clean.length !in listOf(6, 8)) return null
        return try {
            val value = clean.toLong(16)
            if (clean.length == 6) {
                Color((0xFF000000L or value).toInt())
            } else {
                Color(value.toInt())
            }
        } catch (_: Exception) {
            null
        }
    }
}
