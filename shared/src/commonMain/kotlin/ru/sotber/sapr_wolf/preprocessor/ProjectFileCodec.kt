package ru.sotber.sapr_wolf.preprocessor

import kotlinx.serialization.json.Json
import ru.sotber.sapr_wolf.model.Construction

object ProjectFileCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    fun encode(construction: Construction): String =
        json.encodeToString(Construction.serializer(), construction)

    fun decode(jsonString: String): Construction =
        json.decodeFromString(Construction.serializer(), jsonString)

    fun decodeAndNormalize(jsonString: String): NormalizationResult {
        val rawConstruction = decode(jsonString)
        return ConstructionNormalizer.normalize(rawConstruction)
    }
}
