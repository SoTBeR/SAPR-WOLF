package ru.sotber.sapr_wolf.preprocessor

import ru.sotber.sapr_wolf.model.CalculationHash
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.SavedCalculation

object ProjectHash {
    private val HMAC_KEY: ByteArray = "SAPR_WOLF::SPRW::HASH::V2::2026".encodeToByteArray()

    fun computeHash(
        construction: Construction,
        calculation: SavedCalculation,
    ): CalculationHash {
        val canonicalBytes = buildCanonicalPayload(construction, calculation).encodeToByteArray()
        val digestHex = hmacSha256(HMAC_KEY, canonicalBytes).toHexString()
        return CalculationHash(
            algorithm = CalculationHash.ALGORITHM_HMAC_SHA256,
            digest = digestHex,
        )
    }

    fun verifyHash(
        construction: Construction,
        calculation: SavedCalculation,
        hash: CalculationHash?,
    ): Boolean {
        if (hash == null) return false
        if (!hash.algorithm.equals(CalculationHash.ALGORITHM_HMAC_SHA256, ignoreCase = true)) {
            return false
        }
        val expected = computeHash(construction, calculation).digest
        return constantTimeEquals(expected, hash.digest.trim().lowercase())
    }

    internal fun buildCanonicalPayload(
        construction: Construction,
        calculation: SavedCalculation,
    ): String = buildString {
        append("solver=").append(calculation.solverVersion).append('\n')
        append("rods=").append(construction.rods.size).append('\n')
        for (rod in construction.rods) {
            append(canonicalDouble(rod.length)).append(',')
            append(canonicalDouble(rod.crossSectionArea)).append(',')
            append(canonicalDouble(rod.elasticModulus)).append(',')
            append(canonicalDouble(rod.allowableStress)).append(',')
            append(canonicalDouble(rod.distributedLoad)).append('\n')
        }
        append("forces=")
        construction.nodeForces.forEachIndexed { idx, value ->
            if (idx > 0) append(',')
            append(canonicalDouble(value))
        }
        append('\n')
        append("fixed=")
        construction.fixedNodes.sorted().forEachIndexed { idx, node ->
            if (idx > 0) append(',')
            append(node)
        }
        append('\n')
        append("displacements=")
        calculation.displacements.forEachIndexed { idx, value ->
            if (idx > 0) append(',')
            append(canonicalDouble(value))
        }
        append('\n')
        append("reactions=")
        calculation.reactions.entries.sortedBy { it.key }.forEachIndexed { idx, (node, value) ->
            if (idx > 0) append(',')
            append(node).append(':').append(canonicalDouble(value))
        }
        append('\n')
        append("residual=").append(canonicalDouble(calculation.equilibriumResidual)).append('\n')
        append("strengths=").append(calculation.rodStrengths.size).append('\n')
        for (strength in calculation.rodStrengths) {
            append(strength.rodIndex).append(',')
            append(canonicalDouble(strength.maxStress)).append(',')
            append(canonicalDouble(strength.allowableStress)).append(',')
            append(canonicalDouble(strength.safetyFactor)).append(',')
            append(if (strength.isSatisfied) '1' else '0').append('\n')
        }
    }

    private fun canonicalDouble(value: Double): String {
        val normalized = if (value == 0.0) 0.0 else value
        return normalized.toBits().toULong().toString(16).padStart(16, '0')
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }

    internal fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
        val blockSize = 64
        val normalizedKey = if (key.size > blockSize) sha256(key) else key
        val paddedKey = ByteArray(blockSize)
        normalizedKey.copyInto(paddedKey)

        val oKeyPad = ByteArray(blockSize) { i -> (paddedKey[i].toInt() xor 0x5c).toByte() }
        val iKeyPad = ByteArray(blockSize) { i -> (paddedKey[i].toInt() xor 0x36).toByte() }

        val innerInput = ByteArray(blockSize + message.size)
        iKeyPad.copyInto(innerInput, destinationOffset = 0)
        message.copyInto(innerInput, destinationOffset = blockSize)
        val innerHash = sha256(innerInput)

        val outerInput = ByteArray(blockSize + innerHash.size)
        oKeyPad.copyInto(outerInput, destinationOffset = 0)
        innerHash.copyInto(outerInput, destinationOffset = blockSize)
        return sha256(outerInput)
    }

    internal fun sha256(input: ByteArray): ByteArray {
        val bitLength = input.size.toLong() * 8L
        val paddedSize = ((input.size + 9 + 63) / 64) * 64
        val padded = ByteArray(paddedSize)
        input.copyInto(padded)
        padded[input.size] = 0x80.toByte()

        for (i in 0 until 8) {
            padded[paddedSize - 1 - i] = ((bitLength ushr (i * 8)) and 0xffL).toByte()
        }

        var h0 = 0x6a09e667
        var h1 = 0xbb67ae85.toInt()
        var h2 = 0x3c6ef372
        var h3 = 0xa54ff53a.toInt()
        var h4 = 0x510e527f
        var h5 = 0x9b05688c.toInt()
        var h6 = 0x1f83d9ab
        var h7 = 0x5be0cd19

        val w = IntArray(64)

        var offset = 0
        while (offset < paddedSize) {
            for (i in 0 until 16) {
                val base = offset + i * 4
                w[i] = ((padded[base].toInt() and 0xff) shl 24) or
                    ((padded[base + 1].toInt() and 0xff) shl 16) or
                    ((padded[base + 2].toInt() and 0xff) shl 8) or
                    (padded[base + 3].toInt() and 0xff)
            }
            for (i in 16 until 64) {
                val s0 = w[i - 15].rotateRight(7) xor w[i - 15].rotateRight(18) xor (w[i - 15] ushr 3)
                val s1 = w[i - 2].rotateRight(17) xor w[i - 2].rotateRight(19) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4
            var f = h5
            var g = h6
            var h = h7

            for (i in 0 until 64) {
                val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + s1 + ch + SHA256_K[i] + w[i]
                val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj

                h = g
                g = f
                f = e
                e = d + temp1
                d = c
                c = b
                b = a
                a = temp1 + temp2
            }

            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
            h5 += f
            h6 += g
            h7 += h

            offset += 64
        }

        val out = ByteArray(32)
        val hashes = intArrayOf(h0, h1, h2, h3, h4, h5, h6, h7)
        for (i in hashes.indices) {
            val v = hashes[i]
            out[i * 4] = (v ushr 24).toByte()
            out[i * 4 + 1] = (v ushr 16).toByte()
            out[i * 4 + 2] = (v ushr 8).toByte()
            out[i * 4 + 3] = v.toByte()
        }
        return out
    }

    private fun ByteArray.toHexString(): String {
        val hexChars = "0123456789abcdef"
        val result = CharArray(size * 2)
        for (i in indices) {
            val byte = this[i].toInt() and 0xff
            result[i * 2] = hexChars[byte ushr 4]
            result[i * 2 + 1] = hexChars[byte and 0x0f]
        }
        return result.concatToString()
    }

    private val SHA256_K = intArrayOf(
        0x428a2f98, 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
        0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
        0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
        0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
        0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
        0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
        0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(),
        0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt(),
    )
}
