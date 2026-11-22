package ru.sotber.sapr_wolf.preprocessor

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.ProjectFile
import ru.sotber.sapr_wolf.model.SavedCalculation

data class DecodedProject(
    val formatVersion: Int,
    val normalizationResult: NormalizationResult,
    val calculation: SavedCalculation? = null,
    val calculationInvalidationReason: CalculationInvalidationReason? = null,
) {
    val construction: Construction get() = normalizationResult.construction
    val wasNormalized: Boolean get() = normalizationResult.wasModified
}

object ProjectFileCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    fun encode(
        construction: Construction,
        calculation: SavedCalculation? = null,
    ): String {
        val projectFile = ProjectFile(
            formatVersion = ProjectFile.CURRENT_FORMAT_VERSION,
            construction = construction,
            calculation = calculation,
            hash = calculation?.let { ProjectHash.computeHash(construction, it) },
        )
        return json.encodeToString(ProjectFile.serializer(), projectFile)
    }

    fun decodeProject(jsonString: String): DecodedProject {
        val rootObject = json.parseToJsonElement(jsonString).jsonObject
        val projectFile = if ("construction" in rootObject) {
            json.decodeFromJsonElement(ProjectFile.serializer(), rootObject)
        } else {
            val legacyConstruction = json.decodeFromJsonElement(Construction.serializer(), rootObject)
            ProjectFile(
                formatVersion = ProjectFile.LEGACY_FORMAT_VERSION,
                construction = legacyConstruction,
                calculation = null,
                hash = null,
            )
        }

        val normalizationResult = ConstructionNormalizer.normalize(projectFile.construction)
        val rawCalculation = projectFile.calculation
            ?: return DecodedProject(
                formatVersion = projectFile.formatVersion,
                normalizationResult = normalizationResult,
            )

        return when (
            val verification = CalculationVerifier.verify(
                construction = normalizationResult.construction,
                calculation = rawCalculation,
                hash = projectFile.hash,
                wasNormalized = normalizationResult.wasModified,
            )
        ) {
            CalculationVerificationResult.Valid -> DecodedProject(
                formatVersion = projectFile.formatVersion,
                normalizationResult = normalizationResult,
                calculation = rawCalculation,
                calculationInvalidationReason = null,
            )

            is CalculationVerificationResult.Invalid -> DecodedProject(
                formatVersion = projectFile.formatVersion,
                normalizationResult = normalizationResult,
                calculation = null,
                calculationInvalidationReason = verification.reason,
            )
        }
    }

    fun decode(jsonString: String): Construction =
        decodeProject(jsonString).construction

    fun decodeAndNormalize(jsonString: String): NormalizationResult =
        decodeProject(jsonString).normalizationResult
}
